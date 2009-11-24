import zope.interface
import workspacecontrol.api

class NetworkBootstrap(workspacecontrol.api.WCModule):
   """NetworkBootstrap is an wcmodule that sets up (or tears down) anything that
   is needed for the VM to be booted and obtain the proper networking setup.
   
   The de facto standard mechanism for this is DHCP and currently the typical
   Nimbus mechanism for getting the right DHCP information to a VM is to run a
   local DHCP daemon that is populated with the IP information to give a
   particular MAC address.  This may be centralized in the future which could
   possibly render the typical deployment implementation of this interface to
   be a no-op.
   """

   def setup(nic_set, p, c):
       """Do any necessary work to set up the network bootstrapping process,
       this is always called after a network lease is secured but before a VM
       is launched.
       
       nic_set -- instance of NICSet
       
       p -- instance of Parameters
       
       c -- instance of Common
       """

   def teardown(nic_set, p, c):
       """Do any necessary work to tear down the network bootstrapping process,
       this is always called after a VM is shutdown for good but before a 
       network lease is returned.
       
       nic_set -- instance of NICSet
       
       p -- instance of Parameters
       
       c -- instance of Common
       """
       