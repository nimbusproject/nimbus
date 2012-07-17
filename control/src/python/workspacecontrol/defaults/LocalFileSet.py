import zope.interface
import workspacecontrol.api.objects

class DefaultLocalFileSet:

    zope.interface.implements(workspacecontrol.api.objects.ILocalFileSet)

    def __init__(self, lfs_list, instance_dir=None):
        self.lfs_list = lfs_list
        self.local_instance_dir = instance_dir

    def flist(self):
        return self.lfs_list

    def instance_dir(self):
        return self.local_instance_dir
