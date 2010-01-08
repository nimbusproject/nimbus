import os
from lvrt_adapter import PlatformAdapter, PlatformInputAdapter
from workspacecontrol.api.exceptions import *
import lvrt_model

class vmmadapter(PlatformAdapter):
    
    def __init__(self, params, common):
        PlatformAdapter.__init__(self, params, common)
        other_uri = self.p.get_conf_or_none("libvirt_connections", "kvm0")
        if other_uri:
            self.connection_uri = other_uri
        else:
            self.connection_uri = "qemu:///system"
        self.c.log.debug("KVM libvirt URI: '%s'" % self.connection_uri)
            
    def validate(self):
        self.c.log.debug("validating libvirt kvm adapter")

class intakeadapter(PlatformInputAdapter):
    def __init__(self, params, common):
        PlatformInputAdapter.__init__(self, params, common)
        
    def fill_model(self, dom, local_file_set, nic_set, kernel):
        dom._type = "kvm"
        dom.os.type = "hvm"
        
