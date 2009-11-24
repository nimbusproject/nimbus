import workspacecontrol.api

class LocalNetworkSetup(workspacecontrol.api.WCModule):
   """LocalNetworkSetup translates IP address needs to any local information
   needed about the network bridges/topology.  This may be expanded as time
   goes on.
   """
   
   def ip_to_bridge(ipaddress, p, c):
       """Given an IP address required for a particular NIC, what is the local
       system bridge that it needs to be put on?
       
       If there are multiple bridges that support the same IP address ranges
       (perhaps there is an elaborate VPN setup) then the deployer will need to
       configure the other method "network_name_to_bridge" to be used
       exclusively (how that would happen has not been figured out yet).
       
       ipaddress -- string with valid IP address
       
       p -- instance of Parameters
       
       c -- instance of Common
       
       Return bridge name
       """
   
   def network_name_to_bridge(network_name, p, c):
       """Given a network name required for a particular NIC, what is the local
       system bridge that it needs to be put on?
       
       This method is frowned upon because it requires the deployer to duplicate
       information that is set up in the central service (the network's logical
       name).
       
       network_name -- desired network's logical name.
       
       p -- instance of Parameters
       
       c -- instance of Common
       
       Return bridge name
       """
