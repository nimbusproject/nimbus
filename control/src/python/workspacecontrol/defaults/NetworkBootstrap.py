from commands import getstatusoutput
import os
import string
import sys
import zope.interface

import IPy

import workspacecontrol.api.modules
from workspacecontrol.api.exceptions import *
import workspacecontrol.main.wc_args as wc_args

class DefaultNetworkBootstrap:
    """NetworkBootstrap is a wcmodule that sets up (or tears down) anything that
    is needed for the VM to be booted and obtain the proper networking setup.
    
    The de facto standard mechanism for this is DHCP and currently the typical
    Nimbus mechanism for getting the right DHCP information to a VM is to run a
    local DHCP daemon that is populated with the IP information to give a
    particular MAC address.  This may be centralized in the future which could
    possibly render the typical deployment implementation of this interface to
    be a no-op.
    """
    
    zope.interface.implements(workspacecontrol.api.modules.INetworkBootstrap)
    
    def __init__(self, params, common):
         """
         params -- instance of Parameters
         
         common -- instance of Common
         """
         self.p = params
         self.c = common
         self.dhcpconfig = None
         self.dhcpd_path = None
         self.dhcpd_conf_path = None
         self.sudo_path = None
         
         # dict of bridge name --> DHCPd vif
         self.vifdict = {}
         
    def validate(self):
        
        self.dhcpconfig = self.p.get_conf_or_none("mount", "mounttool")
        if not self.dhcpconfig:
            self.c.log.warn("no dhcpconfig configuration (networks.conf), DHCP configuration disabled")
            return
        
        bridges = self.p.all_confs_in_section("dhcp-bridges")
        
        for tup in bridges:
            self.c.log.debug("DHCPd vif for bridge %s: %s" % (tup[0], tup[1]))
            self.vifdict[tup[0]] = tup[1]
            
        if len(self.vifdict) == 0:
            # same as disabling it
            self.c.log.warn("there are no DHCPd interfaces set up (networks.conf), DHCP configuration disabled")
            self.dhcpconfig = None
            return
            
        self._validate_exes()
    
    def _validate_exes(self):
        if not self.dhcpconfig:
            raise ProgrammingError("if there is no dhcpconfig setting, this module is disabled entirely")
            
        if not os.path.isabs(self.dhcpconfig):
            self.dhcpconfig = self.c.resolve_libexec_dir(self.dhcpconfig)
        
        if not os.access(self.dhcpconfig, os.F_OK):
            raise InvalidConfig("dhcp config tool does not exist: '%s'" % self.dhcpconfig)
            
        if not os.access(self.dhcpconfig, os.X_OK):
            raise InvalidConfig("dhcp config tool is not executable: '%s'" % self.dhcpconfig)

        self.c.log.info("DHCP configuration tool configured: %s" % self.dhcpconfig)
        
        self.sudo_path = self.p.get_conf_or_none("sudo", "sudo")
        if not self.sudo_path:
            raise InvalidConfig("mount tool is configured but there is no sudo configuration")
            
        if not os.path.isabs(self.sudo_path):
            raise InvalidConfig("path to sudo must be absolute")
            
        if not os.access(self.sudo_path, os.F_OK):
            raise InvalidConfig("sudo is configured with an absolute path, but it does not seem to exist: '%s'" % self.sudo_path)
            
        if not os.access(self.sudo_path, os.X_OK):
            raise InvalidConfig("sudo is configured with an absolute path, but it does not seem executable: '%s'" % self.sudo_path)

        self.c.log.info("sudo configured: %s" % self.sudo_path)
    
    def setup(self, nic_set, dryrun=False):
        """Do any necessary work to set up the network bootstrapping process,
        this is always called after a network lease is secured but before a VM
        is launched.
        
        nic_set -- instance of NICSet
        """
        
        # disabled
        if not self.dhcpconfig:
            return
            
        for nic in nic_set.niclist():
            
            # 1. choose the DHCP vif if not present
            if not nic.dhcpvifname:
                if not nic.bridge:
                    raise InvalidConfig("Cannot choose a DHCP vif without a bridge assignment for this NIC: %s" % nic.ip)
                if self.vifdict.has_key(nic.bridge):
                    nic.dhcpvifname = vifdict[nic.bridge]
                else:
                    raise InvalidConfig("Cannot choose a DHCP vif for NIC with IP '%s' -- there is no DHCP vif configuration for its bridge '%s' (see networks.conf)" % (nic.ip, nic.bridge))
            
            
            # 2. account for any optional arguments
            if nic.broadcast:
                brd = nic.broadcast
            else:
                brd = "none"

            if nic.netmask:
                netmask = nic.netmask
            else:
                netmask = "none"

            if nic.gateway:
                gtwy = nic.gateway
            else:
                gtwy = "none"

            if nic.dns:
                dns = nic.dns
            else:
                dns = "none"

            dns2 = "none"

            if nic.hostname:
                hostname = nic.hostname
            else:
                hostname = self.opts.name # ... todo: ?


            # 3. make the adjustment
            
            cmd = "%s %s add %s %s %s %s %s %s %s %s %s %s" % (self.sudo_path, self.dhcpconfig, nic.vifname, nic.ip, nic.dhcpvifname, nic.mac, brd, netmask, gtwy, nic.hostname, dns, dns2)

            self.c.log.debug("command = '%s'" % cmd)
            if dryrun:
                self.c.log.debug("(dryrun, didn't run that)")
                continue

            ret,output = getstatusoutput(cmd)
            if ret:
                errmsg = "problem running command: '%s' ::: return code" % cmd
                errmsg += ": %d ::: output:\n%s" % (ret, output)
                self.c.log.error(errmsg)
                raise UnexpectedError(errmsg)
            else:
                self.c.log.debug("altered DHCP rules successfully: %s" % cmd)

    
    def teardown(self, nic_set, dryrun=False):
        """Do any necessary work to tear down the network bootstrapping process,
        this is always called after a VM is shutdown for good but before a 
        network lease is returned.
        
        nic_set -- instance of NICSet
        """
        
        # disabled
        if not self.dhcpconfig:
            return
        
        for nic in nic_set.niclist():
            cmd = "%s %s rem %s %s " % (self.sudo_path, self.dhcpconfig, nic.vifname, nic.ip)
            
            self.c.log.debug("command = '%s'" % cmd)
            if dryrun:
                self.c.log.debug("(dryrun, didn't run that)")
                continue
                
            try:
                ret,output = getstatusoutput(cmd)
            except:
                self.c.log.exception("problem removing DHCP rules")

            if ret:
                errmsg = "problem running command: '%s' ::: return code" % cmd
                errmsg += ": %d ::: output:\n%s" % (ret, output)
                self.c.log.error(errmsg)
            else:
                self.c.log.debug("removed DHCP rules successfully: %s" % cmd)
                