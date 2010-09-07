import zope.interface
import workspacecontrol.api.objects

class DefaultLocalFile:
    
    zope.interface.implements(workspacecontrol.api.objects.ILocalFile)
    
    def __init__(self):
        self.path = None
        self.mountpoint = None
        self.rootdisk = False
        self.read_write = False
        self.editable = False
    