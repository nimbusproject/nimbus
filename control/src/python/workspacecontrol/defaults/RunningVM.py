import zope.interface
import workspacecontrol.api.objects

class DefaultRunningVM:
    
    zope.interface.implements(workspacecontrol.api.objects.IRunningVM)
    
    def __init__(self):
        
        # names
        self.wchandle = None
        self.vmm_id = None
        self.vmm_uuid = None
        
        # types
        self.xmldesc = None
        self.ostype = None
        
        # memory
        self.curmem = 0
        self.maxmem = 0
        
        # vcpus
        self.numvcpus = 0
        
        # cputime used (nanoseconds)
        self.cputime = 0
        
        # state
        self.running = False
        self.blocked = False
        self.paused = False
        self.shutting_down = False
        self.shutoff = False
        self.crashed = False
        