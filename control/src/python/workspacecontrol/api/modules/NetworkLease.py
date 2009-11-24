import zope.interface
import workspacecontrol.api

class NetworkLease(workspacecontrol.api.WCModule):
   """NetworkLease is an wcmodule that secures a networking lease in its own
   implementation specific way.  It could do this via parameters to the program
   or it could for example call out to a site's centralized lease manager.
   """

   def obtain(network_name, p, c):
       """Decide on the network leases.
       
       network_name -- desired network's logical name
       
       p -- instance of Parameters
       
       c -- instance of Common
       
       Return a new instance of NICSet
       """

   def release(nic_set, p, c):
       """Release the network leases.
       
       nic_set -- the instance of NICSet to release
       
       p -- instance of Parameters
       
       c -- instance of Common
       """
       