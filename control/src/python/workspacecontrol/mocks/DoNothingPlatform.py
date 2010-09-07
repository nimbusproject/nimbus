import zope.interface

import workspacecontrol.api.modules
from workspacecontrol.api.exceptions import *
import workspacecontrol.main.wc_args as wc_args
from workspacecontrol.main import ACTIONS
from workspacecontrol.main import wc_core_persistence

class MockInfo:
    def __init__(self):
        self.somefield = "Object not used for anything yet"

class DoNothingPlatform:
    zope.interface.implements(workspacecontrol.api.modules.IPlatform)
    
    def __init__(self, params, common):
        if params == None:
            raise ProgrammingError("expecting params")
        if common == None:
            raise ProgrammingError("expecting common")
        self.p = params
        self.c = common
        self.persistence = wc_core_persistence.Persistence(self.p, self.c)
        self.xen3 = False
        self.kvm0 = False

    def _get_mockinfo_expecting(self):
        """Return (name, mockinfo)
        Raise UnexpectedError if there is no record of name in the system"""
        (name, mockinfo) = self._get_mockinfo_impl()
        if not mockinfo:
            raise UnexpectedError("There is no record of a VM with this name in the system: %s" % name)
        return (name, None)

    def _get_mockinfo_notexpecting(self):
        """Return (name, None)
        Raise UnexpectedError if there is a record of name in the system"""
        (name, mockinfo) = self._get_mockinfo_impl()
        if mockinfo:
            raise UnexpectedError("Name conflict: a VM with this name already exists in the system: %s" % name)
        return (name, None)

    def _get_mockinfo_impl(self, name=None):
        if not name:
            name = self.p.get_arg_or_none(wc_args.NAME)
        mockinfo = self.persistence.get_mock_vm(name)
        return (name, mockinfo)

    def validate(self):
        self._validate_inputs_early()
        self.persistence.validate()

    def create(self, local_file_set, nic_set, kernel):
        (name, mockinfo) = self._get_mockinfo_notexpecting()
        mockinfo = MockInfo()
        self.persistence.store_mock_vm(name, mockinfo)
        self.c.log.info("launched mock VM '%s'" % name)

    def destroy(self, running_vm):
        (name, mockinfo) = self._get_mockinfo_expecting()
        self.persistence.remove_mock_vm(name)
        self.c.log.info("destroyed mock VM '%s'" % name)
       
    def shutdown(self, running_vm):
        (name, mockinfo) = self._get_mockinfo_expecting()
        self.persistence.remove_mock_vm(name)
        self.c.log.info("shutdown mock VM '%s'" % name)
       
    def reboot(self, running_vm):
        self.c.log.info("reboot mock VM '%s'" % name)
       
    def pause(self, running_vm):
        self.c.log.info("paused mock VM '%s'" % name)
       
    def unpause(self, running_vm):
        self.c.log.info("unpaused mock VM '%s'" % name)
       
    def info(self, handle):
        (name, mockinfo) = self._get_mockinfo_impl(name=handle)
        if not mockinfo:
            self.c.log.info("no info found for handle '%s'" % handle)
            return None
        
        self.c.log.info("info, handle '%s'" % handle)
        rvm_cls = self.c.get_class_by_keyword("RunningVM")
        rvm = rvm_cls()
        rvm.wchandle = handle
        rvm.vmm_id = "fake id"
        rvm.vmm_uuid = "fake uuid"
        rvm.xmldesc = "<sorry />"
        rvm.ostype = "linux"
        rvm.running = True
        rvm.maxmem = 1
        rvm.curmem = 1
        rvm.numvcpus = 1
        rvm.cputime = 1
        return rvm

    def print_create_spec(self, local_file_set, nic_set, kernel):
        self.c.log.info("print create specification is not implemented")

# -----------------------------------------------------------------------------

    def _validate_inputs_early(self):
        """validate the inputs that can be validated ahead of time without
        any images, nics, etc.
        """
        
        action = self.p.get_arg_or_none(wc_args.ACTION)
        if not action:
            # this situation is undefined, could be under unit test
            # the actual user cmdline intake will require an action
            return
        
        if action in [ACTIONS.CREATE, ACTIONS.REMOVE, ACTIONS.INFO, ACTIONS.REBOOT, ACTIONS.PAUSE, ACTIONS.UNPAUSE, ACTIONS.PROPAGATE, ACTIONS.UNPROPAGATE, ACTIONS.PRINTXML]:
            name = self.p.get_arg_or_none(wc_args.NAME)
            if not name:
                raise InvalidInput("The %s action requires a name" % action)
        else:
            raise InvalidInput("Unknown action: '%s'" % action)
            
        if action == ACTIONS.CREATE or action == ACTIONS.PRINTXML:
            self._validate_create_basics()
    
        
    def _validate_create_basics(self):
        
        name = self.p.get_arg_or_none(wc_args.NAME)
        if not name:
            raise InvalidInput("No name given for create")
        self.c.log.debug("name for create: %s" % name)
        
        # ---------------------------------------------------------------------
        
        memory = self.p.get_arg_or_none(wc_args.MEMORY)
        if not memory:
            raise InvalidInput("No memory given for create")
        try:
            memory = int(memory)
        except:
            raise InvalidInput("memory given for create is not an integer: %s" % memory)
            
        # convert MB -> kB
        memory = memory * 1024
            
        self.c.log.debug("memory for create: %d" % memory)
        
        # ---------------------------------------------------------------------
        
        vcpus = self.p.get_arg_or_none(wc_args.VCPUS)
        if vcpus:
            self.c.log.debug("vcpus given on cmdline: %s" % vcpus)
        else:
            vcpus = self.p.get_conf_or_none("vmcreation", "num_cpu_per_vm")
            if vcpus:
                self.c.log.debug("vcpu number retrieved from config file: %s" % vcpus)
            else:
                self.c.log.debug("no vcpu number given by argument or configuration, using default of 1")
        
        if vcpus:
            try:
                vcpus = int(vcpus)
            except:
                raise InvalidInput("vcpus is not an integer: %s" % vcpus)
        else:
            vcpus = 1
            