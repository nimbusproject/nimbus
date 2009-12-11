import zope.interface
import workspacecontrol.api

class INetworkSecurity(workspacecontrol.api.IWCModule):
    """NetworkSecurity is a wcmodule that sets up (or tears down) anything that
    is needed for the VM to be booted and obtain a secure networking setup.
    
    The typical mechanism for this so far is to configure ebtables with the rules
    necessary for anti-spoofing enforcement of all packets.  
    
    This includes knowing where target DHCP requests should go, so there is
    currently some coordination necessary between NetworkBootstrap and
    NetworkSecurity implementations (this has resulted in practice that the
    NetworkBootstrap implementation will fill in the "dhcpvifname" attribute
    on the NIC instance.
    """
    
    def __init__(params, common):
         """
         params -- instance of Parameters
         
         common -- instance of Common
         """
  
    def setup(nic_set):
        """Do any necessary work to set up the network security mechanisms,
        this is always called after a network lease is secured and after the
        network bootstrap mechanisms are set up but before a VM is launched.
        
        nic_set -- instance of NICSet
        """
  
    def teardown(nic_set):
        """Do any necessary work to tear down the network security mechanisms,
        this is always called after a VM is shutdown for good but before an IP
        lease is returned.
        
        nic_set -- instance of NICSet
        """
  