import libvirt

class PlatformAdapter:
    
    def __init__(self, params, common):
        """
        params -- instance of Parameters
        
        common -- instance of Common
        """
        
        if params == None:
            raise ProgrammingError("expecting params")
        if common == None:
            raise ProgrammingError("expecting common")
            
        self.p = params
        self.c = common
        
        self.connection_uri = None
        self.vmm = None
    
    def validate(self):
        pass
    
    def get_vmm_connection(self):
        if self.vmm:
            return self.vmm
        
        if not self.connection_uri:
            raise InvalidConfig("There is no connection_uri setting")
        
        self.c.log.debug("creating libvirt connection with URI '%s'" % self.connection_uri)
        
        self.vmm = libvirt.open(self.connection_uri)
        if self.vmm == None:
            raise IncompatibleEnvironment("Cannot create libvirt connection")
        self.c.log.debug("created libvirt connection: %s" % self.vmm)
        
        capabilities = self.vmm.getCapabilities()
        self.c.log.debug("VMM capabilities:\n\n%s\n" % capabilities)
        
        node = self.vmm.getInfo()
        # sample from mock: ['i986', 8000, 50, 6000, 4, 4, 4, 2]
        phys = " - processor: %s\n" % node[0]
        phys += " - memory: %d\n" % node[1]
        phys += " - cpus: %d\n" % node[2]
        phys += " - cpu frequency: %d\n" % node[3]
        if node[4] != 1:
            phys += " - numa nodes: %d\n" % node[4]
        else:
            phys += " - uniform memory access\n"
        phys += " - cpu sockets per node: %d\n" % node[5]
        phys += " - cores per socket: %d\n" % node[6]
        phys += " - threads per core: %d\n" % node[7]
        self.c.log.debug("Physical node information:\n%s" % phys)
        
        return self.vmm
    
class PlatformInputAdapter:
    """Separate class for intake work ("fill model") that adapters do, so that
    a connection is not required to use/exercise the adapter's intake logic"""
    
    def __init__(self, params, common):
        """
        params -- instance of Parameters
        
        common -- instance of Common
        """
        
        if params == None:
            raise ProgrammingError("expecting params")
        if common == None:
            raise ProgrammingError("expecting common")
            
        self.p = params
        self.c = common
    
    def fill_model(dom, local_file_set, nic_set, kernel):
        """Assist conversion of workspace control API objects to the appropriate
        libvirt model.  The adapter is responsible for any VMM-specific
        adaptation.
        
        dom -- lvrt_model.Domain instance, the common parts of this are
        filled already
        
        local_file_set -- ILocalFileSet instance
        
        nic_set -- INICSet instance
        
        kernel -- IKernel instance
        """
        
        pass
    
    