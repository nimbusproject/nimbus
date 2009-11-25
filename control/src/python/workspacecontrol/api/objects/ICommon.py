import zope.interface
import workspacecontrol.api

class ICommon(workspacecontrol.api.IWCObject):
    """Common is a systemwide mechanism for common functionality such as
    logging.  Modules are not required to use it.
    """
    
    def __init__(params):
        """
        params -- instance of Parameters
        """
  
    log = zope.interface.Attribute(
    """log may never be None
    """)
    
    trace = zope.interface.Attribute(
    """trace is True will trigger a torrent of debug statements vs. the regular
    debug level
    """)
    
    dryrun = zope.interface.Attribute(
    """dryrun is True will trigger a test runthrough of the command; may not be
    supported, you can make your module a no-op or print something instead
    """)