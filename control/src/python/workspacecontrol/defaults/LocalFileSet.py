import zope.interface
import workspacecontrol.api.objects

class DefaultLocalFileSet:
    
    zope.interface.implements(workspacecontrol.api.objects.ILocalFileSet)
    
    def __init__(self, lfs_list):
        self.lfs_list = lfs_list
    
    def flist(self):
        return self.lfs_list
    
