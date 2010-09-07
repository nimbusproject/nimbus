import os
from lvrt_adapter import PlatformAdapter, PlatformInputAdapter
from workspacecontrol.api.exceptions import *
import lvrt_adapter_xen3

def testconfs_path(filename=None):
    current = os.path.abspath(__file__)
    current = "/".join(os.path.dirname(current+"/").split("/")[:-1])
    testdir = current + "/testnode/"
    if filename:
        return testdir + filename
    else:
        return testdir

class vmmadapter(PlatformAdapter):
    
    def __init__(self, params, common):
        PlatformAdapter.__init__(self, params, common)
        testuri = "test:///" + testconfs_path("testnode.xml") 
        self.connection_uri = testuri

    def validate(self):
        self.c.log.debug("validating libvirt mock adapter")
        vmm = self.get_vmm_connection()
        
        # doing a few random things to exercise the mock adapter
        
        allids = vmm.listDomainsID()
        for id in allids:
            avm = vmm.lookupByID(id)
            self.c.log.debug("found domain '%s'" % avm.name())
        
        prevlen = len(allids)
        
        testconf1 = testconfs_path("testdom-newone.xml")
        self.c.log.debug("creating test domain with '%s'" % testconf1)
        f = open(testconf1)
        newone = f.read()
        f.close()
        
        newvm = vmm.createXML(newone, 0)
        self.c.log.debug("launched '%s'" % newvm.name())
        
        allids = vmm.listDomainsID()
        if len(allids) != (prevlen + 1):
            raise UnexpectedError("create request did not register in listdomains (lencheck)")
        
        newvm2 = vmm.lookupByName("newone412")
        name = newvm2.name()
        if name != "newone412":
            raise UnexpectedError("lookup by name failed, returned VM with name '%s'" % name)
            
        self.c.log.debug("lookup by name was equal: '%s'" % name)
        self.c.log.debug("ID: '%d'" % newvm2.ID())
        self.c.log.debug("UUID: '%s'" % newvm2.UUIDString())
        self.c.log.debug("is active?: %s" % newvm2.isActive())
        
class intakeadapter(PlatformInputAdapter):
    
    def __init__(self, params, common):
        PlatformInputAdapter.__init__(self, params, common)
        self.xen3 = lvrt_adapter_xen3.intakeadapter(params, common)
        
    def fill_model(self, dom, local_file_set, nic_set, kernel):
        
        # delegates to lvrt_adapter_xen3.intakeadapter
        self.xen3.fill_model(dom, local_file_set, nic_set, kernel)
        
        # except for this bit
        dom._type = "test"
        
    
