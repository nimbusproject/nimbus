import zope.interface
import workspacecontrol.api

class IRunningVM(workspacecontrol.api.IWCObject):
    """RunningVM is what the Platform module's info command returns, given
    a handle (that must be supplied externally).
    """
    
    wchandle = zope.interface.Attribute(
    """wchandle ...
    """)
    
    state = zope.interface.Attribute(
    """state ...
    """)
    
    # TODO: many other fields are coming
    