import IPy
import string
import sys
import zope.interface

import workspacecontrol.api.modules
from workspacecontrol.api.exceptions import *
import workspacecontrol.main.wc_args as wc_args

class DefaultNetworkLease:
    """NetworkLease is a wcmodule that secures a networking lease in its own
    implementation specific way.  It could do this via parameters to the program
    or it could for example call out to a site's centralized lease manager.
    """
    
    zope.interface.implements(workspacecontrol.api.modules.INetworkLease)
    
    def __init__(self, params, common):
        self.p = params
        self.c = common
        self.nics = None
        self.networkarg = None
        
    def validate(self):
        arg = self.p.get_arg_or_none(wc_args.NETWORKING)
        if arg:
            self.nics = self._parse_arg(arg)
            self.networkarg = arg
    
    def obtain(self, vm_name, nic_name, network_name):
        """Decide on a network lease.
        
        vm_name -- the unique VM deployment name
        
        nic_name -- an internal name unique across the VM's NICs
        
        network_name -- desired network's logical name
        
        Return a new instance of NIC or None
        """
        
        # If the "lease" is obtained via commandline arguments, the assumption
        # is that a central entity is managing them, there is no persistence
        # etc. here.
        
        arg = self.p.get_arg_or_none(wc_args.NETWORKING)
        if not arg:
            return None
            
        if self.networkarg != arg:
            self.c.log.debug("networking parameter changed between validate and obtain\n - old: %s\n - new: %s\n" % (self.networkarg, arg))
            
            # means self.nics 'cache' is invalid, reparse
            self.nics = self._parse_arg(arg)
            self.networkarg = arg
            
        # Other implementations of this module might use vm_name, nic_name, and
        # network_name in other ways.  Maybe only for recording purposes, maybe
        # for indexing leases, or maybe passing on to central lease manager in
        # requests.  For this implementation, all that is needed is nic_name.
        
        for nic in self.nics:
            if nic.name == nic_name:
                return nic
        return None
    
    def release(self, vm_name, nic_set):
        """Release network leases.
        
        vm_name -- the unique VM deployment name
        
        nic_set -- the instance of NICSet to release
        """
        
        # If the "lease" is obtained via commandline arguments, the assumption
        # is that a central entity is managing them, there is no persistence
        # etc. here.
        pass
    
    
    # --------------------------------------------------------------------------
    # PARSING/VALIDATION IMPL
    # --------------------------------------------------------------------------
    
    def _parse_arg(self, arg):
        """Return a list of NICs >0 or raise InvalidInput"""
        
        argname = wc_args.NETWORKING.long_syntax

        # given input string might be quoted to escape semicolons
        # for certain delivery methods (e.g., sh over ssh) and
        # some methods may not strip quotes (e.g., localhost, direct
        # exe invocation)
        # so strip extra quotes if present
        if arg[0] == "'":
            arg = arg[1:]

        # (there is a pathological case where input was only a single quote...)
        if arg and arg[-1] == "'":
            arg = arg[:-1]
            
        arg = arg.strip()
        if not arg:
            raise InvalidInput("'%s' argument was empty" % argname)

        nicstrings = arg.split(';;')
        
        i = 0
        nics = []
        for nicstr in nicstrings:
            i += 1
            logstr = "nic #%d" % i
            nic = self._parse_one_nic(nicstr, logstr)
            self.c.log.info("%s is valid, MAC=%s IP=%s" % (logstr, nic.mac, nic.ip))
            nics.append(nic)

        self.c.log.debug("found %d valid NICs" % len(nics))
        
        # should not be possible:
        if len(nics) == 0:
            raise InvalidInput("could not find any valid NICs")

        seen_names = []
        for nic in nics:
            if nic.name in seen_names:
                raise InvalidInput("NIC names in a VM must be unique, duplicate: '%s'" % nic.name)
            seen_names.append(nic.name)

        return nics
        
    def _parse_one_nic(self, nicstring, logstring):
        """Return one NIC or raise InvalidInput"""
        
        if self.c.trace:
            self.c.log.debug("%s: ________________" % logstring)
            self.c.log.debug("%s: processing input: %s" % (logstring, nicstring))

        # sample:
        # publicnic;public;A2:AA:BB:2C:36:9A;Bridged;Static;128.135.125.22;128.135.125.1;128.135.125.255;255.255.255.0;128.135.247.50;tp-x002.ci.uchicago.edu
        
        # could also have ";null;null;null;null" at the end (something from
        # the olden days) which would make 15 vs. 11 fields

        opts = nicstring.split(';')

        if len(opts) < 11:
            raise InvalidInput("%s: NIC does not have the 11 minimum fields, "
                       "full input: %s" % (logstring, nicstring))
        
        nic_cls = self.c.get_class_by_keyword("NIC")
        nic = nic_cls()
        
        nic.name = opts[0]
        nic.network = opts[1]
        nic.mac = opts[2]
        nic.nic_type = opts[3]
        nic.ip = opts[5]
        nic.gateway = opts[6]
        nic.broadcast = opts[7]
        nic.netmask = opts[8]
        nic.dns = opts[9]
        nic.hostname = opts[10]
        
        # ------ name ------
        if self.c.trace:
            self.c.log.debug("%s: name = %s" % (logstring, nic.name))
        
        # ------ network ------
        if self.c.trace:
            self.c.log.debug("%s: network = %s" % (logstring, nic.network))
            
        # ------ MAC ------
        if self.c.trace:
            self.c.log.debug("%s: mac = %s" % (logstring, nic.mac))
        if not self._checkMAC(nic.mac):
            raise InvalidInput("%s: MAC is invalid: %s" % (logstring, nic.mac))
        
        # ------ nic_type ------
        # See INIC interface notes, currently nothing uses this anymore
        # and "BRIDGED" is the assumption.  That could change.
        if self.c.trace:
            self.c.log.debug("%s: nic_type (deprecated) = %s" % (logstring, nic.nic_type))
        
        # ------ configuration_mode ------
        # Currently nothing uses this anymore.
        configuration_mode = string.upper(opts[4])
        if self.c.trace:
            self.c.log.debug("%s: configuration_mode (deprecated) = %s" % (logstring, configuration_mode))
            
        # ------ ip ------
        if self.c.trace:
            self.c.log.debug("%s: checking IP %s" % (logstring, nic.ip))
        if not self._checkIP(nic.ip):
            raise InvalidInput("%s: IP setting '%s' is an invalid IP" % (logstring, nic.ip))
        
        # ------ gateway ------
        if self.c.trace:
            self.c.log.debug("%s: checking gateway %s" % (logstring, nic.gateway))
        if nic.gateway.strip().lower() == "null":
            if self.c.trace:
                self.c.log.debug("%s: gateway not set, perhaps being added to an isolated subnet" % logstring)
            nic.gateway = None
        elif not self._checkIP(nic.gateway):
            raise InvalidInput("%s: gateway setting %s is an invalid IP" % (logstring, nic.gateway))
        
        # ------ broadcast ------
        # if missing ('null'), try to give appropriate default
        if self.c.trace:
            self.c.log.debug("%s: checking broadcast %s" % (logstring, nic.broadcast))
        if nic.broadcast.strip().lower() == "null":
            # derive the broadcast from given IP 
            self.c.log.debug("%s: guessing broadcast from IP %s" % (logstring, nic.ip))
            (clz, guess) = self._broadcast_guess(nic.ip)
            if guess != None:
                self.c.log.debug("%s: no broadcast given, IP %s is a class %s address, assigning %s" % (logstring, nic.ip, clz, guess))
                nic.broadcast = guess
            else:
                raise InvalidInput("%s: fatal, given IP %s was not class A, B, or C?" % (logstring, nic.ip))

        elif not self._checkIP(nic.broadcast):
            raise InvalidInput("%s: broadcast setting '%s' is an invalid IP" % (logstring, nic.broadcast))
        
        # ------ subnet mask ------
        # if missing ('null'), try to give appropriate default
        if self.c.trace:
            self.c.log.debug("%s: checking subnet mask %s" % (logstring, nic.netmask))
        if nic.netmask.strip().lower() == "null":
            self.c.log.debug("%s: guessing subnet mask from IP %s" % (logstring, nic.ip))
            (clz, guess) = self._subnet_guess(nic.ip)
            if guess != None:
                self.c.log.debug("%s: no subnet mask given, IP %s is a class %s address, assigning %s" % (logstring, nic.ip, clz, guess))
                nic.netmask = guess
            else:
                raise InvalidInput("%s: fatal, given IP %s was not class A, B, or C?" % (logstring, nic.ip))
        
        elif not self._checkIP(nic.netmask):
            raise InvalidInput("%s: subnet mask setting '%s' is invalid" % (logstring, nic.netmask))
        
        # ------ dns ------
        if self.c.trace:
            self.c.log.debug("%s: checking dns %s" % (logstring, nic.dns))
        if nic.dns.strip().lower() == "null":
            self.c.log.debug("%s: dns not set" % logstring)
            nic.dns = None
        elif not self._checkIP(nic.dns):
            raise InvalidInput("%s: fatal, dns setting %s is an invalid IP" % (logstring, nic.dns))
        
        # ------ hostname ------
        if self.c.trace:
            self.c.log.debug("%s: checking hostname %s" % (logstring, nic.hostname))
        if nic.hostname.strip().lower() == "null":
            self.c.log.warn("%s: hostname not set" % logstring)
            nic.hostname = None

        return nic
        
        
    # ------------------------
    # Support for _parse_arg()
    # ------------------------
        
    def _checkMAC(self, mac):
        try:
            x = mac.split(":")
            if len(x) != 6:
                return False
            for i in x:
                if len(i) != 2:
                    return False
                for j in i:
                    if j not in string.hexdigits:
                        return False
            return True
        except:
            return False
            
    def _checkIP(self, ip):
        try:
            [a,b,c,d] = map(string.atoi, string.splitfields(ip, '.'))
            x = range(0,256)
            for i in (a,b,c,d):
                if i not in x:
                    return False
            return True
        except:
            return False
            
    def _subnet_guess(self, ip):
        x = IPy.IP(ip).strBin()
        if x[0] == '0':
            return ('A', '255.0.0.0')
        elif x[1] == '0':
            return ('B', '255.255.0.0')
        elif x[2] == '0':
            return ('C', '255.255.255.0')
        elif x[3] == '0':
            # multicasting
            return ('D', None)
        else:
            # reserved
            return ('E', None)
    
    def _broadcast_guess(self, ip):
        x = IPy.IP(ip).strBin()
        y = IPy.IP(ip).strHex()
        if x[0] == '0':
            y = y[:4] + "FFFFFF"
            return ('A', IPy.IP(y).strNormal())
        elif x[1] == '0':
            y = y[:6] + "FFFF"
            return ('B', IPy.IP(y).strNormal())
        elif x[2] == '0':
            y = y[:8] + "FF"
            return ('C', IPy.IP(y).strNormal())
        elif x[3] == '0':
            # multicasting
            return ('D', None)
        else:
            # reserved
            return ('E', None)
        