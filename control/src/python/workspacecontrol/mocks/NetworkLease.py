import zope.interface

import workspacecontrol.api.modules
from workspacecontrol.api.exceptions import *

class NetworkLease:
    """Implementation of procurement that obtains a fake nic set.  Keeps
    track of leases, to simulate failures etc.
    """
    
    zope.interface.implements(workspacecontrol.api.modules.INetworkLease)
   
    def __init__(self, params, common):
        self.p = params
        self.c = common
        
        # list of tuples: (leased, nic) where leased is T/F
        self.allnics = self._new_allnics()
    
    def validate(self):
        pass
    
    def obtain(self, vm_name, nic_name, network_name):
        """Decide on a network lease.
        
        vm_name -- the unique VM deployment name
        
        nic_name -- an internal name unique across the VM's NICs
        
        network_name -- desired network's logical name
        
        Return a new instance of NIC or None
        """
        
        # NOTE: nic_name is ignored by this mock impl
        
        self.c.log.debug("asked for nic lease from network '%s'" % network_name)
        
        retnic = None
        for i, nictup in enumerate(self.allnics):
            if nictup[0]:
                continue
            nic = nictup[1]
            if nic.network == network_name:
                self.allnics[i] = (True, nic)
                retnic = nic
                self.c.log.info("leased nic '%s'" % nic.name)
                break
                
        return retnic

    def release(self, nic_set):
        """Release network leases.
        
        nic_set -- the instance of NICSet to release
        """
        
        self.c.log.debug("asked to retire nic")
        
        for nic_to_retire in nic_set.niclist():
            self._release_nic(nic_to_retire)
        
    def _release_nic(self, nic_to_retire):
        for i, nictup in enumerate(self.allnics):
            if not nictup[0]:
                continue
            nic = nictup[1]
            if nic.network == nic_to_retire.network:
                if nic.ip == nic_to_retire.ip:
                    self.allnics[i] = (False, nic)
                    self.c.log.info("retired nic '%s'" % nic.name)
                    break
        
    def _new_allnics(self):
        
        allnics = []
        nic_cls = self.c.get_class_by_keyword("NIC")
        
        for i in range(10,35):
            nic = self._populate_onenic(i, nic_cls, "public", "virbr1")
            allnics.append((False,nic))
        for i in range(55,80):
            nic = self._populate_onenic(i, nic_cls, "private", "virbr3")
            allnics.append((False,nic))
            
        return allnics
        
    def _populate_onenic(self, i, nic_cls, network_name, bridge_name):
        if i < 10 or i > 99:
            raise ProgrammingError("i must be 2 digits, for MAC")
            
        nic = nic_cls()
        nic.network = network_name
        
        # not lease's responsibility to populate but in here for mock's sake
        nic.bridge = bridge_name
        
        nic.mac = "AA:23:4d:21:6c:%d" % i
        
        nic.nic_type = "BRIDGED"
        
        nic.vifname = "vif%d.0" % i
        
        if network_name == "public":
            nic.name = "pubnic-%d" % i
            nic.dhcpvifname = None # not lease's responsibility
            nic.ip = "10.0.0.%d" % i
            nic.gateway = "10.0.0.1"
            nic.broadcast = "255.0.0.0"
            nic.netmask = "10.255.255.255"
            nic.dns = "10.0.0.2"
            nic.hostname = "publichost-%d" % i
        else:
            nic.name = "privnic-%d" % i
            nic.dhcpvifname = None # not lease's responsibility
            nic.ip = "192.168.0.%d" % i
            nic.gateway = "192.168.0.1"
            nic.broadcast = "192.168.0.255"
            nic.netmask = "255.255.255.0"
            nic.dns = "192.168.0.1"
            nic.hostname = "privatehost-%d" % i
        
        return nic
        