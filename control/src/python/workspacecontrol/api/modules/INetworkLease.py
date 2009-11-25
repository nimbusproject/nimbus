import zope.interface
import workspacecontrol.api

class INetworkLease(workspacecontrol.api.IWCModule):
   """NetworkLease is an wcmodule that secures a networking lease in its own
   implementation specific way.  It could do this via parameters to the program
   or it could for example call out to a site's centralized lease manager.
   """
   
   def __init__(params, common):
        """
        params -- instance of Parameters
        
        common -- instance of Common
        """

   def obtain(network_name):
       """Decide on the network leases.
       
       network_name -- desired network's logical name
       
       Return a new instance of NICSet
       """

   def release(nic_set):
       """Release the network leases.
       
       nic_set -- the instance of NICSet to release
       """
       