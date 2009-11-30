import zope.interface
import workspacecontrol.api

class IRunningVM(workspacecontrol.api.IWCObject):
    """RunningVM is what the Platform module's info command returns, given
    a handle (that must be supplied externally).
    """
    
    wchandle = zope.interface.Attribute(
    """wchandle is the management named, used to look this up in the first place
    """)
    
    vmm_id = zope.interface.Attribute(
    """vmmid is the hypervisor-internal ID number
    """)
    
    vmm_uuid = zope.interface.Attribute(
    """vmmuuid is typically the hypervisor-internal UUID, may be missing
    """)
    
    xmldesc = zope.interface.Attribute(
    """xmldesc is a platform's XML representation of the domain, may be missing
    """)
    
    ostype = zope.interface.Attribute(
    """ostype is typically the VM's ABI as seen from the 'outside', may be
    missing
    """)
    
    curmem = zope.interface.Attribute(
    """curmem is the amount of memory (MB) currently assigned to the VM
    """)
    
    maxmem = zope.interface.Attribute(
    """maxmem is the amount of memory (MB) available to the VM.  i.e., this will
    be higher than the curmem attribute if ballooning is possible.  If there
    is no ballooning possible, curmem will always equal maxmem.
    """)
    
    numvcpus = zope.interface.Attribute(
    """numvcpus is the current amount of vcpus assigned to the VM.  Nimbus
    assumes this does not change out of band during runtime.
    """)
    
    cputime = zope.interface.Attribute(
    """cputime is the amount of time (in nanoseconds) the VM has been scheduled
    on a CPU
    """)
    
    running = zope.interface.Attribute(
    """VM state: running, True/False
    """)
    
    blocked = zope.interface.Attribute(
    """VM state: blocked, True/False  (waiting on I/O)
    """)
    
    paused = zope.interface.Attribute(
    """VM state: paused, True/False
    """)
    
    shutting_down = zope.interface.Attribute(
    """VM state: in process of shutting down, True/False
    """)
    
    shutoff = zope.interface.Attribute(
    """VM state: shutoff, True/False
    """)
    
    crashed = zope.interface.Attribute(
    """VM state: crashed, True/False
    """)
    
    