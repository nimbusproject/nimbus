import zope.interface
import workspacecontrol.api

class NIC(workspacecontrol.api.WCObject):
    """NIC is one NIC that a VM needs to be setup with   The information is consulted by many wcmodules.
    """
    
    name = zope.interface.Attribute(
    """name
    """)
    
    network = zope.interface.Attribute(
    """network (formerly "association")
    """)
    
    bridge = zope.interface.Attribute(
    """bridge
    """)
    
    mac = zope.interface.Attribute(
    """mac
    """)
    
    nic_type = zope.interface.Attribute(
    """nic_type is carried over here in order to provide a place for both future
    and experimental/non-standard implementations to go about things differently
    (but in the initial release, there is nothing that uses this)
    """)
    
    vifname = zope.interface.Attribute(
    """vifname
    """)
    
    dhcpvifname = zope.interface.Attribute(
    """dhcpvifname
    """)
    
    ip = zope.interface.Attribute(
    """ip
    """)
    
    gateway = zope.interface.Attribute(
    """gateway
    """)
    
    broadcast = zope.interface.Attribute(
    """broadcast
    """)
    
    netmask = zope.interface.Attribute(
    """netmask
    """)
    
    dns = zope.interface.Attribute(
    """dns is a DNS instance (or None)
    """)
    
    hostname = zope.interface.Attribute(
    """hostname
    """)

