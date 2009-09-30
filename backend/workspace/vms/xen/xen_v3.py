import logging

log = logging.getLogger("workspace.xen_v3")

from xen_v2 import xen_v2_manager,xen_v2_manager_config

# OO here is not as good as it could be
class xen_v3_manager(xen_v2_manager):
    def init(self, parameters):
        xen_v2_manager.__init__(self, parameters)
        
    def constructNetOptions(self, xmopts):
        log.debug("xen_v3_manager running")
        
class xen_v3_manager_config(xen_v2_manager_config):
    def __init__(self, conffile, action):
        xen_v2_manager_config.__init__(self, conffile, action)
        
    def xensudo(self, cmd):
        if self.sudopath:
            return self.sudopath + " " + cmd
        else:
            return cmd

    def xmsudo(self, cmd):
        if self.usexmsudo:
            return self.sudopath + " " + cmd
        else:
            return cmd

###########
# globals #
###########
        
def instance(parameters):
    global _instance
    try:
        _instance
    except:
        _instance = xen_v3_manager(parameters)
    return _instance

def parameters(conffile, action):
    global _parameters
    try:
        _parameters
    except:
        _parameters = xen_v3_manager_config(conffile, action)
    return _parameters

