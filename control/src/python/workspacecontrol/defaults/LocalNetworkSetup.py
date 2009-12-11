from commands import getstatusoutput
import os
import string
import sys
import zope.interface

import IPy

import workspacecontrol.api.modules
from workspacecontrol.api.exceptions import *
import workspacecontrol.main.wc_args as wc_args

class DefaultLocalNetworkSetup:
    """LocalNetworkSetup translates IP address needs to any local information
    needed about the network bridges/topology.  This may be expanded as time
    goes on.
    """
    
    zope.interface.implements(workspacecontrol.api.modules.ILocalNetworkSetup)
    
    def __init__(self, params, common):
         self.p = params
         self.c = common
         self.defaultbridge = None
         
         # dict of IP instance keys --> bridge name value
         self.bridgedict = {}
         
    def validate(self):
        self.defaultbridge = self.p.get_conf_or_none("defaultbridge", "default")
        if self.defaultbridge:
            self.c.log.debug("default bridge is configured: %s" % self.defaultbridge)
        else:
            self.c.log.debug("no default bridge is configured")
            
        self._populate_bridgedict()
        
        if not self.defaultbridge and len(self.bridgedict) == 0:
            raise InvalidConfig("There is no default bridge and there are no IP range --> bridge mappings.  Any VM request will fail.")
            
        self._pretty_print_configs()
    
    # --------------------------------------------------------------------------
    # validate() IMPLs
    # --------------------------------------------------------------------------
    
    def _populate_bridgedict(self):
        
        bridges = self.p.all_confs_in_section("bridges")
        for tup in bridges:
            # each bridge can list multiple values
            values = tup[1].split(",")
            for value in values:
                try:
                    new_iprange = IPy.IP(value)
                except:
                    errstr = "IP range '%s' for bridge '%s' is invalid" % (value, tup[0])
                    self.c.log.exception(errstr)
                    exception_type = sys.exc_type
                    try:
                        exceptname = exception_type.__name__ 
                    except AttributeError:
                        exceptname = exception_type
                    errstr += ": %s: %s" % (str(exceptname), str(sys.exc_value))
                    raise InvalidConfig(errstr)
            
                for iprange in self.bridgedict.keys():
                    if new_iprange == iprange:
                        # Note that this does not cover an *overlap* just the
                        # same ranges. So mainly a "typo defense"
                        # TODO: analyzing overlaps would be better.
                        raise InvalidConfig("Found duplicate IP range in bridge setups: %s" % new_iprange)
        
                self.c.log.debug("IP range for bridge %s: %s" % (tup[0], new_iprange))
                self.bridgedict[new_iprange] = tup[0]
            
    def _pretty_print_configs(self):
        
        pstr = "IP address/bridge configuration:\n - There is "
        if not self.defaultbridge:
            pstr += "not "
        pstr += "a default bridge configuration"
        if self.defaultbridge:
            pstr += ": %s" % self.defaultbridge
        else:
            pstr += "."
            
        # make a temporary dict that maps a bridge to a list of ranges
        rangedict = {}
        for iprange in self.bridgedict.keys():
            bridge = self.bridgedict[iprange]
            if rangedict.has_key(bridge):
                rangedict[bridge].extend([iprange])
            else:
                rangedict[bridge] = [iprange]
                
        if len(rangedict) == 0:
            pstr += "\n - There are no specific IP-range --> bridge mappings."
        else:
            for bridge in rangedict.keys():
                pstr += "\n - '%s' bridge: " % bridge
                for x,iprange in enumerate(rangedict[bridge]):
                    if x > 0:
                        pstr += ", "
                    pstr += "%s" % iprange
                
        self.c.log.info(pstr + "\n")
    
    # --------------------------------------------------------------------------
    # ip_to_bridge() from LocalNetworkSetup interface
    # --------------------------------------------------------------------------
    
    def ip_to_bridge(self, ipaddress):
        """Given an IP address required for a particular NIC, what is the local
        system bridge that it needs to be put on?
        
        If there are multiple bridges that support the same IP address ranges
        (perhaps there is an elaborate VPN setup) then the deployer will need
        to configure the other method "network_name_to_bridge" to be used
        exclusively (how that would happen has not been figured out yet).
        
        ipaddress -- string with valid IP address
        
        Return bridge name
        """
        
        if not self._checkIP(ipaddress):
            raise InvalidInput("'%s' is not a valid IPv4 address" % ipaddress)
        
        for iprange in self.bridgedict.keys():
            if self._ip_in_range(ipaddress, iprange):
                return self.bridgedict[iprange]
        
        if not self.defaultbridge:
            raise IncompatibleEnvironment("Cannot map the IP address '%s' to a bridge to use and there is no default bridge (see networks.conf)" % ipaddress)
            
        return self.defaultbridge
        
    # --------------------------------------------------------------------------
    # network_name_to_bridge() from LocalNetworkSetup interface
    # --------------------------------------------------------------------------
    
    def network_name_to_bridge(self, network_name):
        """Given a network name required for a particular NIC, what is the local
        system bridge that it needs to be put on?
        
        This method is frowned upon because it requires the deployer to
        duplicate information that is set up in the central service (the
        network's logical name).
        
        network_name -- desired network's logical name.
        
        Return bridge name
        """
        
        raise ProgrammingError("not implemented")
    
    # --------------------------------------------------------------------------
    # support methods
    # --------------------------------------------------------------------------
    
    def _ip_in_range(self, ip, iprange):
        if ip in iprange:
            if self.c.trace:
                self.c.log.debug("IP %s is in IP range %s" % (ip, iprange))
            return True
        return False
        
    def _checkIP(self, ipstring):
        try:
            [a,b,c,d] = map(string.atoi, string.splitfields(ipstring, '.'))
            x = range(0,256)
            for i in (a,b,c,d):
                if i not in x:
                    return False
            return True
        except:
            return False
