import zope.interface
import workspacecontrol.api.objects

class DefaultDNS:
    
    zope.interface.implements(workspacecontrol.api.objects.IDNS)
    
    def __init__(self):
        self.nameservers = []
        self.searches = []
    