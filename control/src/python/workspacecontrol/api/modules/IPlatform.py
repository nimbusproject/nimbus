import zope.interface
import workspacecontrol.api

class IPlatform(workspacecontrol.api.IWCModule):
    """Platform is the direct VMM control interface"""

    xen3 = zope.interface.Attribute("""xen3 is True if this is a Xen3 host""")
    kvm0 = zope.interface.Attribute("""kvm is True if this is a KVM host""")
    
    def __init__(params, common):
        """
        params -- instance of Parameters
        
        common -- instance of Common
        """

    def create(local_file_set, nic_set, kernel):
        """create launches a VM"""

    def destroy(running_vm):
        """destroy shuts a VM down instantly"""
       
    def shutdown(running_vm):
        """shutdown shuts a VM down gracefully"""
       
    def reboot(running_vm):
        """reboot reboots a running VM in place"""
       
    def pause(running_vm):
        """pause pauses a running VM in place"""
       
    def unpause(running_vm):
        """unpause unpauses a paused VM"""
       
    def info(handle):
        """info polls the current status of the VM
        
        Return instance of RunningVM or None if the handle was not found.
        """

    def print_create_spec(local_file_set, nic_set, kernel):
        """If possible, print to stdout something that the platform adapter
        produces for the underlying mechanism's creation call(s).
        
        This is used for testing and debugging.  This is not a requirement to
        implement an IPlatform adapter, it could do nothing.
        """
