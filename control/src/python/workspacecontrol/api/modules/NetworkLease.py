import zope.interface
import workspacecontrol.api

class NetworkLease(workspacecontrol.api.WCModule):
   """NetworkLease is an wcmodule that secures a networking lease in its own
   implementation specific way.  It could do this via parameters to the program
   or it could for example call out to a site's centralized lease manager.
   """

   def obtain(parameters):
       """Decide on the network leases.
       
       parameters -- instance of Parameters
       
       Return a new instance of NICSet
       """

   def release(nic_set, parameters):
       """Release the network leases.
       
       nic_set -- the instance of NICSet to release
       
       parameters -- instance of Parameters
       """
       