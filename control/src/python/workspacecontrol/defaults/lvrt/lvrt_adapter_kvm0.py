import os
from lvrt_adapter import PlatformAdapter, PlatformInputAdapter
from workspacecontrol.api.exceptions import *
import lvrt_model

class intakeadapter(PlatformInputAdapter):
    def __init__(self, params, common):
        PlatformInputAdapter.__init__(self, params, common)
        
    def fill_model(self, dom, local_file_set, nic_set, kernel):
        dom._type = "qemu"
        dom.os.type = "linux"
        
