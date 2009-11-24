import zope.interface
import workspacecontrol.api

class Platform(workspacecontrol.api.WCModule):
    """Platform is the direct VMM control interface"""

    xen3 = zope.interface.Attribute("""xen3 is True if this is a Xen3 host""")
    kvm = zope.interface.Attribute("""kvm is True if this is a KVM host""")

    def create(local_file_set, nic_set, parameters):
        """create launches a VM"""

    def destroy(running_vm, parameters):
        """destroy shuts a VM down instantly"""
       
    def shutdown(running_vm, parameters):
        """shutdown shuts a VM down gracefully"""
       
    def reboot(running_vm, parameters):
        """reboot reboots a running VM in place"""
       
    def pause(running_vm, parameters):
        """pause pauses a running VM in place"""
       
    def unpause(running_vm, parameters):
        """unpause unpauses a paused VM"""
       
    def info(handle, parameters):
        """info polls the current status of the VM
        
        Return instance of RunningVM or None if the handle was not found.
        """
