import os
import zope.interface
from ConfigParser import NoOptionError, NoSectionError

import workspacecontrol.api.objects
import workspacecontrol.main.wc_args as wc_args

# -----------------------------------------------------------------------------
# DefaultParameters 
# -----------------------------------------------------------------------------

class DefaultParameters:
    
    zope.interface.implements(workspacecontrol.api.objects.IParameters)
    
    def __init__(self, allconfigs, opts):
        self.optdict = _create_optdict(opts)
        _munge_optdict(self.optdict)
        self.conf = allconfigs
    
    def get_arg_or_none(self, key):
        
        if not key:
            return None
            
        if isinstance(key, wc_args.ControlArg):
            key = key.name
            
        val = None
        if self.optdict and self.optdict.has_key(key):
            try:
                val = self.optdict[key]
            except:
                return None
        return val
    
    def get_conf_or_none(self, section, key):
        if not self.conf:
            return None
        if not section:
            return None
        if not key:
            return None
            
        try:
            aconf = self.conf.get(section, key)
        except NoSectionError:
            return None
        except NoOptionError:
            return None
            
        if not aconf:
            return None
            
        aconf = aconf.strip()
        if len(aconf) == 0:
            return None
            
        return aconf

def _create_optdict(opts):
    d = {}
    
    if not opts:
        return d
    
    for arg in wc_args.ALL_WC_ARGS_LIST:
        d[arg.name] = _get_one_attr(opts, arg.name)
    
    return d
    
def _get_one_attr(opts, name):
    try:
        val = getattr(opts, name)
        if not val:
            return None
        return val
    except:
        return None
    
    
def _munge_optdict(d):
    """Account for deprecated commandline arguments.  The given optdict is a
    'pure' representation of the given arguments, this method transforms it
    into a 'normalized' representation that the program understands.  This
    allows old arguments to work without anything but the arg intake knowing
    about the changes.
    """
    pass
    