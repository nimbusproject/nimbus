from commands import getstatusoutput
import os
import sys
import zope.interface

import workspacecontrol.api.modules
from workspacecontrol.api.exceptions import *
import workspacecontrol.main.wc_args as wc_args

class DefaultNetworkSecurity:
    """NetworkSecurity is a wcmodule that sets up (or tears down) anything that
    is needed for the VM to be booted and obtain a secure networking setup.
    
    The typical mechanism for this so far is to configure ebtables with the
    rules necessary for anti-spoofing enforcement of all packets.  
    
    This includes knowing where target DHCP requests should go, so there is
    currently some coordination necessary between NetworkBootstrap and
    NetworkSecurity implementations (this has resulted in practice that the
    NetworkBootstrap implementation will fill in the "dhcpvifname" attribute
    on the NIC instance).
    """
    
    zope.interface.implements(workspacecontrol.api.modules.INetworkSecurity)
    
    def __init__(self, params, common):
         self.p = params
         self.c = common
         self.ebtablesconfig = None
         self.sudo_path = None
         
    def validate(self):
        
        localdhcp = self.p.get_conf_or_none("dhcp", "localdhcp")
        self.localdhcp = bool(localdhcp and localdhcp.lower() != 'false')
        
        self.ebtablesconfig = self.p.get_conf_or_none("netsecurity", "ebtablesconfig")

        if not self.ebtablesconfig:
            self.c.log.warn("no ebtablesconfig configuration (networks.conf), network security configuration disabled")
            return
        
        self._validate_exes()
    
    def _validate_exes(self):
        if not self.ebtablesconfig:
            raise ProgrammingError("if there is no ebtablesconfig setting, this module is disabled entirely")
            
        if not os.path.isabs(self.ebtablesconfig):
            self.ebtablesconfig = self.c.resolve_libexec_dir(self.ebtablesconfig)
        
        if not os.access(self.ebtablesconfig, os.F_OK):
            raise InvalidConfig("ebtables config tool does not exist: '%s'" % self.ebtablesconfig)
            
        if not os.access(self.ebtablesconfig, os.X_OK):
            raise InvalidConfig("ebtales config tool is not executable: '%s'" % self.ebtablesconfig)

        self.c.log.debug("ebtables configuration tool configured: %s" % self.ebtablesconfig)
        
        self.sudo_path = self.p.get_conf_or_none("sudo", "sudo")
        if not self.sudo_path:
            raise InvalidConfig("ebtables tool is configured but there is no sudo configuration")
            
        if not os.path.isabs(self.sudo_path):
            raise InvalidConfig("path to sudo must be absolute")
            
        if not os.access(self.sudo_path, os.F_OK):
            raise InvalidConfig("sudo is configured with an absolute path, but it does not seem to exist: '%s'" % self.sudo_path)
            
        if not os.access(self.sudo_path, os.X_OK):
            raise InvalidConfig("sudo is configured with an absolute path, but it does not seem executable: '%s'" % self.sudo_path)

        self.c.log.debug("sudo configured for network security: %s %s" % (self.sudo_path, self.ebtablesconfig))
  
    def setup(self, nic_set):
        """Do any necessary work to set up the network security mechanisms,
        this is always called after a network lease is secured and after the
        network bootstrap mechanisms are set up but before a VM is launched.
        
        nic_set -- instance of NICSet
        """
        
        # disabled
        if not self.ebtablesconfig:
            return
            
        for nic in nic_set.niclist():
            
            if not nic.vifname:
                raise InvalidInput("NIC object has no vifname")
            if self.localdhcp and not nic.dhcpvifname:
                raise InvalidInput("NIC object has no dhcpvifname but localdhcp is enabled")
            if not nic.mac:
                raise InvalidInput("NIC object has no mac")
            if not nic.ip:
                raise InvalidInput("NIC object has no ip")
                
            cmd = "%s %s add %s %s %s" % (self.sudo_path, self.ebtablesconfig, nic.vifname, nic.mac, nic.ip)
            if self.localdhcp:
                cmd += " " + nic.dhcpvifname

            self.c.log.debug("command = '%s'" % cmd)
            if self.c.dryrun:
                self.c.log.debug("(dryrun, didn't run that)")
                continue

            ret,output = getstatusoutput(cmd)
            if ret:
                errmsg = "problem running command: '%s' ::: return code" % cmd
                errmsg += ": %d ::: output:\n%s" % (ret, output)
                self.c.log.error(errmsg)
                raise UnexpectedError(errmsg)
            else:
                self.c.log.debug("added ebtables rules successfully: %s" % cmd)
  
    def teardown(self, nic_set):
        """Do any necessary work to tear down the network security mechanisms,
        this is always called after a VM is shutdown for good but before an IP
        lease is returned.
        
        nic_set -- instance of NICSet
        """
        
        # disabled
        if not self.ebtablesconfig:
            return
            
        for nic in nic_set.niclist():
            
            if not nic.vifname:
                raise InvalidInput("NIC object has no vifname")
            
            cmd = "%s %s rem %s" % (self.sudo_path, self.ebtablesconfig, nic.vifname)

            self.c.log.debug("command = '%s'" % cmd)
            if self.c.dryrun:
                self.c.log.debug("(dryrun, didn't run that)")
                continue

            ret,output = getstatusoutput(cmd)
            if ret:
                errmsg = "problem running command: '%s' ::: return code" % cmd
                errmsg += ": %d ::: output:\n%s" % (ret, output)
                self.c.log.error(errmsg)
                raise UnexpectedError(errmsg)
            else:
                self.c.log.debug("removed ebtables rules successfully: %s" % cmd)
