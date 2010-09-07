import zope.interface
import workspacecontrol.api.objects

class DefaultKernel:
    
    zope.interface.implements(workspacecontrol.api.objects.IKernel)
    
    def __init__(self):
        self.onboard_kernel = False
        self.kernel_path = None
        self.initrd_path = None
        self.kernel_args = None
        