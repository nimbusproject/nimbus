import zope.interface
import workspacecontrol.api

class NetworkSecurity(workspacecontrol.api.WCModule):
   """NetworkSecurity is an wcmodule that sets up (or tears down) anything that
   is needed for the VM to be booted and obtain a secure networking setup.
   
   The typical mechanism for this so far is to configure ebtables with the rules
   necessary for anti-spoofing enforcement of all packets.  
   
   This includes knowing where target DHCP requests should go, so there is
   currently some coordination necessary between NetworkBootstrap and
   NetworkSecurity implementations (this has resulted in practice that the
   NetworkBootstrap implementation will fill in the "dhcpvifname" attribute
   on the NIC instance.
   """

   def setup(nic_set, p, c):
       """Do any necessary work to set up the network security mechanisms,
       this is always called after a network lease is secured and after the
       network bootstrap mechanisms are set up but before a VM is launched.
       
       nic_set -- instance of NICSet
       
       p -- instance of Parameters
       
       c -- instance of Common
       """

   def teardown(nic_set, p, c):
       """Do any necessary work to tear down the network security mechanisms,
       this is always called after a VM is shutdown for good but before an IP
       lease is returned.
       
       nic_set -- instance of NICSet
       
       p -- instance of Parameters
       
       c -- instance of Common
       """
       