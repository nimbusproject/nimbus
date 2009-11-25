import zope.interface

class IWCObject(zope.interface.Interface):
    """Ancestor interface of all workspacecontrol.api objects that are not
    callable modules, for example "Kernel" or "LocalFileSet"
    
    Every class/interface in the workspacecontrol.api package descends from
    IWCModule, IWCObject, or IWCError.
    """
