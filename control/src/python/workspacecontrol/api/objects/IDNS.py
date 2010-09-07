import zope.interface
import workspacecontrol.api

class IDNS(workspacecontrol.api.IWCObject):
    """DNS is an encapsulation of everything necessary to populate a VM's
    settings.  If a multi-NIC VM gets assigned different DNS entries for
    different NICs, the behavior is undefined.
    """
    
    nameservers = zope.interface.Attribute(
    """nameservers is a list of DNS server addresses to use, the typical syntax
    of which is a "nameserver xyz.com" line in the resolv.conf file inside the
    VM.  This list contains one string for each entry,
    e.g. ["192.168.0.1", "4.2.2.1", "4.2.2.2"]
    
    Can be empty or None.
    """)
    
    searches = zope.interface.Attribute(
    """searches is a list of domains to search with first, the typical syntax
    of which is a "search xyz.com" line in the resolv.conf file inside the VM.
    This list contains one string for each entry, 
    e.g. ["xyz.com", "abc.def.com"] 
    
    Can be empty or None.
    """)
    
    
    