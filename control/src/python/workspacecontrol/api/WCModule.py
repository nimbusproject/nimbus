import zope.interface

class WCModule(zope.interface.Interface):
    """Ancestor interface of all callable workspace-control modules.
    
    Every class/interface in the workspacecontrol.api package descends from
    WCModule, WCObject, or WCError.
    """
    