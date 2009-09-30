#Copyright 1999-2006 University of Chicago
#
#Licensed under the Apache License, Version 2.0 (the "License"); you may not
#use this file except in compliance with the License. You may obtain a copy
#of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
#Unless required by applicable law or agreed to in writing, software
#distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#License for the specific language governing permissions and limitations
#under the License.

"""Instead of creating the class on the fly (which is possible),
   this makes for easier set/notset checks
   
   class Enumeration is from:
       http://aspn.activestate.com/ASPN/Cookbook/Python/Recipe/67107
       
   This is licensed under the general Python license (like examples in 
   the Python documentation).
"""
   
import types, string, exceptions, random
from workspace.util.IPy import IP

class EnumException(exceptions.Exception):
    pass
    
class Enumeration:
    def __init__(self, name, enumList):
        self.__doc__ = name
        lookup = { }
        reverseLookup = { }
        i = 0
        uniqueNames = [ ]
        uniqueValues = [ ]
        for x in enumList:
            if type(x) == types.TupleType:
                x, i = x
            if type(x) != types.StringType:
                raise EnumException, "enum name is not a string: " + x
            if type(i) != types.IntType:
                raise EnumException, "enum value is not an integer: " + i
            if x in uniqueNames:
                raise EnumException, "enum name is not unique: " + x
            if i in uniqueValues:
                raise EnumException, "enum value is not unique for " + x
            uniqueNames.append(x)
            uniqueValues.append(i)
            lookup[x] = i
            reverseLookup[i] = x
            i = i + 1
        self.lookup = lookup
        self.reverseLookup = reverseLookup
    def __getattr__(self, attr):
        if not self.lookup.has_key(attr):
            raise AttributeError
        return self.lookup[attr]
    def whatis(self, value):
        return self.reverseLookup[value]

NicType = Enumeration("NicType", 
    ["BRIDGED", 
     "NAT", 
     "VNET"
     ])

ConfigurationMode = Enumeration("ConfigurationMode",
    ["DHCP",
     "VPN",
     "STATIC",
     "INDEPENDENT"
     ])
     
     
def assignNicType(astring):
    if not NicType.lookup.has_key(astring):
        return None
        
    return NicType.__getattr__(astring)
    
def assignConfigurationMode(astring):
    if not ConfigurationMode.lookup.has_key(astring):
        return None
        
    return ConfigurationMode.__getattr__(astring)
    
    
def subnetGuess(ip):
    x = IP(ip).strBin()
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

def broadcastGuess(ip):
    x = IP(ip).strBin()
    y = IP(ip).strHex()
    if x[0] == '0':
        y = y[:4] + "FFFFFF"
        return ('A', IP(y).strNormal())
    elif x[1] == '0':
        y = y[:6] + "FFFF"
        return ('B', IP(y).strNormal())
    elif x[2] == '0':
        y = y[:8] + "FF"
        return ('C', IP(y).strNormal())
    elif x[3] == '0':
        # multicasting
        return ('D', None)
    else:
        # reserved
        return ('E', None)
    
     
# ----------------------------------------- #
# nic class                                 #
# ----------------------------------------- #

class xen_nic:
    
    def __init__(self):
        self.name = None
        self.association = None
        self.bridge = None
        self.mac = None
        self.nic_type = None
        self.configuration_mode = None
        self.vifname = None
        self.dhcpvifname = None
        
        self.ip = None
        self.gateway = None
        self.broadcast = None
        self.netmask = None
        self.dns = None
        self.hostname = None
        self.certname = None
        self.keyname = None
        self.certpath = None
        self.keypath = None
        
        
# ----------------------------------------- #
# MAC track                                 #
# ----------------------------------------- #

class mac_track:
    
    def __init__(self):
        self.macdict = { }
        
    def addMAC(self, association, mac):
        if not self.macdict.has_key(association):
            raise Exception, "association '%s' not registered" % association
            
        # not checking if prefix matches, prefix is only for auto-gen
        mac = string.upper(mac)
        if mac in self.macdict[association][1]:
            return False
        self.macdict[association][1].append(mac)
        return True
        
    def newMAC(self, association):
        if not self.macdict.has_key(association):
            raise Exception, "association '%s' not registered" % association
            
        prefix = self.macdict[association][0]
        if not prefix:
            prefix = ""
        macs = self.macdict[association][1]
        if not macs:
            self.macdict[association][1] = [ ]
            macs = self.macdict[association][1]
        
        if prefix != "":
            # only supporting PAIRs of hex digits in the MAC prefix
            prefixparts = prefix.split(":")
            for part in prefixparts:
                if len(part) != 2:
                    raise Exception, "MAC prefix invalid, '%s' not a hex digit pair" % part
                for i in part:
                    if i not in string.hexdigits:
                        raise Exception, "MAC prefix invalid, '%s' not a hex digit" % i
            
            # how many to conjure? 
            numprefixpairs = len(prefixparts)
            if numprefixpairs > 5:
                raise Exception, "MAC prefix invalid, there are over 5 hex digit pairs (%d of them)" % numprefixpairs
            numnewpairs = 6 - numprefixpairs
            
        newmac = prefix
        choices = "0123456789ABCDEF"
        found = False
        counter = 0
        while found == False:
            if counter > 500:
                raise Exception, "couldn't find new MAC after %d tries" % counter
                
            if prefix == "":
                numnewpairs = 5
                choiceidx = random.randrange(len(choices))
                newmac += choices[choiceidx]
                choiceidx = random.randrange(len(choices))
                newmac += choices[choiceidx]
                
            for i in range(numnewpairs):
                newmac += ":"
                choiceidx = random.randrange(len(choices))
                newmac += choices[choiceidx]
                choiceidx = random.randrange(len(choices))
                newmac += choices[choiceidx]
            if newmac in macs:
                newmac = prefix
                counter += 1
            else:
                self.macdict[association][1].append(newmac)
                found = True
        return newmac
        
    def retireMAC(self, association, mac):
        if not self.macdict.has_key(association):
            raise Exception, "association '%s' not registered" % association
            
        if not self.macdict[association][1]:
            return False
            
        mac = string.upper(mac)
            
        try:
            self.macdict[association][1].remove(mac)
        except ValueError:
            return False
            
        # there shouldn't even be duplicates
        try:
            while True:
                self.macdict[association][1].remove(mac)
        except ValueError:
            return True
            
    def newAssociation(self, association, prefix):
        if self.macdict.has_key(association):
            return False
        if prefix:
            numprefixpairs = len(prefix.split(":"))
            if numprefixpairs > 5:
                raise Exception, "MAC prefix invalid, there are over 5 hex digit pairs (%d of them)" % numprefixpairs
        self.macdict[association] = [prefix, None]
        return True
        
    def retireAssocation(self, association):
        if self.macdict.has_key(association):
            self.macdict.pop(association)
        return True
        
# ----------------------------------------- #
# IP track                                  #
# ----------------------------------------- #

class ip_track:
    
    def __init__(self):
        self.ipdict = { }

    def addIP(self, association, ip):
        if not self.ipdict.has_key(association):
            raise Exception, "association '%s' not registered" % association
            
        # not checking if prefix matches, prefix is only for auto-gen
        if ip in self.ipdict[association]:
            return False
        self.ipdict[association].append(ip)
        return True
        
    def retireIP(self, association, ip):
        if not self.ipdict.has_key(association):
            raise Exception, "association '%s' not registered" % association
            
        if not self.ipdict[association]:
            return False
            
        try:
            self.ipdict[association].remove(ip)
        except ValueError:
            return False
            
        # there shouldn't even be duplicates
        try:
            while True:
                self.ipdict[association].remove(ip)
        except ValueError:
            return True
            
    def newAssociation(self, association):
        if self.ipdict.has_key(association):
            return False
        self.ipdict[association] = [ ]
        return True
        
    def retireAssocation(self, association):
        if self.ipdict.has_key(association):
            self.ipdict.pop(association)
        return True
