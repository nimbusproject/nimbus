import zope.interface
import workspacecontrol.api.objects

class DefaultNIC:
    
    zope.interface.implements(workspacecontrol.api.objects.INIC)
    
    def __init__(self):
        self.name = None
        self.network = None
        self.bridge = None
        self.mac = None
        self.nic_type = None
        
        self.vifname = None
        self.dhcpvifname = None
        
        self.ip = None
        self.gateway = None
        self.broadcast = None
        self.netmask = None
        self.dns = None
        self.hostname = None
