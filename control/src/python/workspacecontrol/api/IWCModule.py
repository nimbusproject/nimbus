import zope.interface

class IWCModule(zope.interface.Interface):
    """Ancestor interface of all callable workspace-control modules.
    
    Every class/interface in the workspacecontrol.api package descends from
    IWCModule, IWCObject, or IWCError.
    """
    
    def validate():
        """Called before any actions are taken.
        """
