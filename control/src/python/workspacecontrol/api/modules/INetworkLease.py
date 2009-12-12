import zope.interface
import workspacecontrol.api

class INetworkLease(workspacecontrol.api.IWCModule):
    """NetworkLease is a wcmodule that secures a networking lease in its own
    implementation specific way.  It could do this via parameters to the program
    or it could for example call out to a site's centralized lease manager.
    """
    
    def __init__(params, common):
        """
        params -- instance of Parameters
        
        common -- instance of Common
        """
    
    def obtain(vm_name, nic_name, network_name):
        """Decide on a network lease.
        
        vm_name -- the unique VM deployment name
        
        nic_name -- an internal name unique across the VM's NICs
        
        network_name -- desired network's logical name
        
        Return a new instance of NIC or None
        """
    
    def release(vm_name, nic_set):
        """Release network leases.
        
        vm_name -- the unique VM deployment name
        
        nic_set -- instance of NICSet
        """
