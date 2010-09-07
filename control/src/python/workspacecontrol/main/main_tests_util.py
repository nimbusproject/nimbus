import os
from workspacecontrol.api.exceptions import *
from workspacecontrol.main import get_all_configs
from workspacecontrol.mocks import get_mock_mainconf
from workspacecontrol.main import get_class_by_keyword

def mockconfigs(basename="main.conf"):
    return get_all_configs(get_mock_mainconf(basename=basename))
    
def realconfigs():
    # only a best effort guess, tests assume it is under $basedir/etc
    return get_all_configs(get_mainconf_guess())
    
def get_pc(opts, allconfs):
    p_cls = get_class_by_keyword("Parameters", allconfigs=allconfs)
    p = p_cls(allconfs, opts)
    
    c_cls = get_class_by_keyword("Common", allconfigs=allconfs)
    c = c_cls(p)
    
    return (p, c)

# The tests don't take a 'main.conf' parameter anywhere from developer
def get_mainconf_guess():
    basedir = guess_basedir()
    return os.path.join(basedir, "etc/workspace-control/main.conf")

def _jump_up_dir(path):
    return "/".join(os.path.dirname(path+"/").split("/")[:-1])

def guess_basedir():
    # figure it out programmatically from location of this source file
    # this can be an unintuitive value
    current = os.path.abspath(__file__)
    while True:
        current = _jump_up_dir(current)
        if os.path.basename(current) == "src":
            # jump up one more time
            current = _jump_up_dir(current)
            return current
        if not os.path.basename(current):
            raise IncompatibleEnvironment("cannot find base directory")
