import zope.interface
import workspacecontrol.api

class ICommon(workspacecontrol.api.IWCObject):
    """Common is a systemwide mechanism for common functionality (such as
    logging) and common derived information (as opposed to raw parameter
    values).  Modules are not required to use it.
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
    
    def __init__(params):
        """
        params -- instance of Parameters
        """
        
    def resolve_var_dir(name):
        """Return absolute path to the needed file/dir in the var directory
        name -- relative path to file/directory
        """
        
    def resolve_libexec_dir(name):
        """Return absolute path to the needed file/dir in the libexec directory
        name -- relative path to file/directory
        """
        
    def get_class_by_keyword(keyword):
        """Use the default 'dependency injection' mechanism.  This system is
        not a requirement to use to create objects, all that is needed is
        interface compliance (see internal.conf).
        
        As the 'ICommon' implementation is itself typically instantiated by
        the same mechanism, there is some bootstrapping that needs to occur
        at the beginning of the program.  Modules should assume this has occured
        already (it would not be sane/legal to provide a broken common instance
        to the module, every module gets a common instance via __init__).
        """
        