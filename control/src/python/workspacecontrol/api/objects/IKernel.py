import zope.interface
import workspacecontrol.api

class IKernel(workspacecontrol.api.IWCObject):
    """Kernel is a representation of the kernel file(s) to use for a deployment
    """
    
    onboard_kernel = zope.interface.Attribute(
    """onboard_kernel, if set to True, signals the Platform module to boot the
    VM through other means (for example, pygrub or other method for VMs that
    carry their own kernels.  If True, kernel_path and initrd_path are ignored.
    """)
    
    kernel_path = zope.interface.Attribute(
    """If present, kernel_path is an absolute path to the kernel to use with
    the deployment.  If onboard_kernel is True, this is ignored.
    """)
    
    initrd_path = zope.interface.Attribute(
    """If present, initrd_path is an absolute path to the initrd to use with
    the deployment.  If onboard_kernel is True, this is ignored.
    """)
    
    kernel_args = zope.interface.Attribute(
    """If present, the arguments are passed through to the kernel 'commandline'.
    If onboard_kernel is True, this is ignored.
    """)
    