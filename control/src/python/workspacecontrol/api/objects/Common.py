import zope.interface
import workspacecontrol.api

class Common(workspacecontrol.api.WCObject):
    """Common is a systemwide mechanism for common functionality such as
    logging.  Modules are not required to use it.
    """
  
    log = zope.interface.Attribute(
    """log is never None
    """)
    