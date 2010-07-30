import fcntl
import os
import sys
import zope.interface
import libvirt

import workspacecontrol.api.modules
from workspacecontrol.api.exceptions import *
import workspacecontrol.main.wc_args as wc_args
from workspacecontrol.main import ACTIONS

import lvrt_adapter_xen3
import lvrt_adapter_kvm0
import lvrt_adapter_mock
import lvrt_model

class Platform:
    zope.interface.implements(workspacecontrol.api.modules.IPlatform)
    
    def __init__(self, params, common):
        
        self.xen3 = False
        self.kvm0 = False
        self.create_flock = False
        
        if params == None:
            raise ProgrammingError("expecting params")
        if common == None:
            raise ProgrammingError("expecting common")
            
        self.p = params
        self.c = common
        
        adapter_conf = self.p.get_conf_or_none("libvirt", "vmm")
        
        if not adapter_conf:
            raise InvalidConfig("Missing or invalid 'vmm' configuration in libvirt.conf")
            
        if adapter_conf == "xen3":
            self.adapter = lvrt_adapter_xen3.vmmadapter(params, common)
            self.intakeadapter = lvrt_adapter_xen3.intakeadapter(params, common)
            self.xen3 = True
            # Because of a race between mount-alter.sh and Xen scripts for
            # accessing loopback devices, we need to flock the same lock as
            # mount-alter.sh
            self.create_flock = True
        elif adapter_conf == "kvm0":
            self.adapter = lvrt_adapter_kvm0.vmmadapter(params, common)
            self.intakeadapter = lvrt_adapter_kvm0.intakeadapter(params, common)
            self.kvm0 = True
        elif adapter_conf == "mock":
            self.adapter = lvrt_adapter_mock.vmmadapter(params, common)
            self.intakeadapter = lvrt_adapter_mock.intakeadapter(params, common)
            self.xen3 = True
        else:
            raise InvalidConfig("Unknown 'vmm' configuration in libvirt conf: '%s'" % adapter_conf)
    
    def validate(self):
        
        self._validate_inputs_early()
        
        self.c.log.debug("validating libvirt Platform module")
        self.adapter.validate()
        vmm = self._vmm()
        domainIds = vmm.listDomainsID()
        for did in domainIds:
            avm = vmm.lookupByID(did)
            self.c.log.debug("Found VM: id #%s, name '%s'" % (did, avm.name()))
            
    def create(self, local_file_set, nic_set, kernel):
        """create launches a VM"""
        model = self._fill_model(local_file_set, nic_set, kernel)
        xml = model.toXML()
        
        self.c.log.debug("XML being sent to libvirt:\n\n%s\n" % xml)
        
        if self.c.dryrun:
            self.c.log.debug("dryrun, not sending")
            return

        newvm = None
        lockfile = None
        try:
            try:
                if self.create_flock:
                    lockfilepath = self.c.resolve_var_dir("lock/loopback.lock")
                    if not os.path.exists(lockfilepath):
                        raise IncompatibleEnvironment("cannot find lock directory or lock file, make sure lock/loopback.lock exists")
                    lockfile = open(lockfilepath, "r")
                    fcntl.flock(lockfile.fileno(), fcntl.LOCK_EX)
                newvm = self._vmm().createXML(xml, 0)
            except libvirt.libvirtError,e:
                shorterr = "Problem creating the VM: %s" % str(e)
                self.c.log.error(shorterr)
                self.c.log.exception(e)
                raise UnexpectedError(shorterr)
        finally:
            if lockfile:
                lockfile.close()
            
        self.c.log.info("launched '%s'" % newvm.name())
        
    def print_create_spec(self, local_file_set, nic_set, kernel):
        """If possible, print to stdout something that the platform adapter
        produces for the underlying mechanism's creation call(s).
        
        This is used for testing and debugging.  This is not a requirement to
        implement an IPlatform adapter, it could do nothing.
        """
        model = self._fill_model(local_file_set, nic_set, kernel)
        xml = model.toXML()
        print xml
        

    def destroy(self, running_vm):
        """destroy shuts a VM down instantly"""
        name = running_vm.wchandle
        vm = self._get_vm_by_handle(name)
        if not vm:
            err = "could not find VM with name '%s'" % name
            raise UnexpectedError(err)
        try:
            vm.destroy()
        except libvirt.libvirtError,e:
            shorterr = "Problem destroying the '%s' VM: %s" % (name, str(e))
            self.c.log.error(shorterr)
            self.c.log.exception(e)
            raise UnexpectedError(shorterr)
            
    def shutdown(self, running_vm):
        """shutdown shuts a VM down gracefully"""
        name = running_vm.wchandle
        vm = self._get_vm_by_handle(name)
        if not vm:
            err = "could not find VM with name '%s'" % name
            raise UnexpectedError(err)
        try:
            vm.shutdown()
        except libvirt.libvirtError,e:
            shorterr = "Problem shutting down the '%s' VM: %s" % (name, str(e))
            self.c.log.error(shorterr)
            self.c.log.exception(e)
            raise UnexpectedError(shorterr)
       
    def reboot(self, running_vm):
        """reboot reboots a running VM in place"""
        name = running_vm.wchandle
        vm = self._get_vm_by_handle(name)
        if not vm:
            err = "could not find VM with name '%s'" % name
            raise UnexpectedError(err)
        try:
            vm.reboot(0)
        except libvirt.libvirtError,e:
            shorterr = "Problem rebooting the '%s' VM: %s" % (name, str(e))
            self.c.log.error(shorterr)
            self.c.log.exception(e)
            raise UnexpectedError(shorterr)
       
    def pause(self, running_vm):
        """pause pauses a running VM in place"""
        name = running_vm.wchandle
        vm = self._get_vm_by_handle(name)
        if not vm:
            err = "could not find VM with name '%s'" % name
            raise UnexpectedError(err)
        try:
            vm.suspend()
        except libvirt.libvirtError,e:
            shorterr = "Problem suspending (pausing) the '%s' VM: %s" % (name, str(e))
            self.c.log.error(shorterr)
            self.c.log.exception(e)
            raise UnexpectedError(shorterr)
       
    def unpause(self, running_vm):
        """unpause unpauses a paused VM"""
        name = running_vm.wchandle
        vm = self._get_vm_by_handle(name)
        if not vm:
            err = "could not find VM with name '%s'" % name
            raise UnexpectedError(err)
        try:
            vm.resume()
        except libvirt.libvirtError,e:
            shorterr = "Problem resuming (unpausing) the '%s' VM: %s" % (name, str(e))
            self.c.log.error(shorterr)
            self.c.log.exception(e)
            raise UnexpectedError(shorterr)
       
    def info(self, handle):
        """info polls the current status of the VM
        
        Return instance of RunningVM or None if the handle was not found.
        """
        
        vm = self._get_vm_by_handle(handle)
        if not vm:
            return None
        
        self.c.log.debug("found VM with name '%s'" % handle)
            
        rvm_cls = self.c.get_class_by_keyword("RunningVM")
        rvm = rvm_cls()
        rvm.wchandle = handle
        rvm.vmm_id = vm.ID()
        rvm.vmm_uuid = vm.UUIDString()
        rvm.xmldesc = vm.XMLDesc(0)
        rvm.ostype = vm.OSType()
        
        infolist = vm.info()
        if not infolist:
            raise UnexpectedError("Problem obtaining VM information from libvirt")
        if len(infolist) != 5:
            raise UnexpectedError("Unrecognized VM information from libvirt")
        
        # from http://libvirt.org/python.html
        # (0) state: one of the state values (virDomainState)
        # (1) maxMemory: the maximum memory used by the domain
        # (2) memory: the current amount of memory used by the domain
        # (3) nbVirtCPU: the number of virtual CPU
        # (4) cpuTime: the time used by the domain in nanoseconds
        
        state = infolist[0] # see below
        rvm.maxmem = infolist[1] # that's all for this
        rvm.curmem = infolist[2] # that's all for this
        rvm.numvcpus = infolist[3] # that's all for this
        rvm.cputime = infolist[4] # that's all for this
        
        
        # see http://libvirt.org/html/libvirt-libvirt.html#virDomainState
        DOM_STATE_NOSTATE	= 	0	# no state
        DOM_STATE_RUNNING	= 	1	# the domain is running
        DOM_STATE_BLOCKED	= 	2	# the domain is blocked on resource
        DOM_STATE_PAUSED	= 	3	# the domain is paused by user
        DOM_STATE_SHUTDOWN	= 	4	# the domain is being shut down
        DOM_STATE_SHUTOFF	= 	5	# the domain is shut off
        DOM_STATE_CRASHED	= 	6	# the domain is crashed
        
        if state not in range(0,7):
            raise UnexpectedError("Unrecognized state information from libvirt: %s" % str(infolist[0]))
            
        if state == DOM_STATE_NOSTATE:
            # this is the case right after a graceful shutdown succeeds
            self.c.log.debug("found VM with name '%s' but it has no state -- from the perspective 'above' this means it was not found at all." % handle)
            return None
            
        if state == DOM_STATE_RUNNING:
            rvm.running = True
        if state == DOM_STATE_BLOCKED:
            rvm.blocked = True
        if state == DOM_STATE_PAUSED:
            rvm.paused = True
        if state == DOM_STATE_SHUTDOWN:
            rvm.shutting_down = True
        if state == DOM_STATE_SHUTOFF:
            rvm.shutoff = True
        if state == DOM_STATE_CRASHED:
            rvm.crashed = True
        
        return rvm
        
# -----------------------------------------------------------------------------

    def _vmm(self):
        try:
            return self.adapter.get_vmm_connection()
        except libvirt.libvirtError,e:
            shorterr = "Problem with connection to the VMM: %s" % str(e)
            self.c.log.error(shorterr)
            self.c.log.exception(e)
            raise UnexpectedError(shorterr)

    def _get_vm_by_handle(self, handle):
        if not handle:
            raise InvalidInput("No handle")
        handle = handle.strip()
        if not handle:
            raise InvalidInput("No handle")
        
        try:
            return self._vmm().lookupByName(handle)
        except:
            shorterr = "Could not find domain with name '%s'" % handle
            self.c.log.debug(shorterr)
            return None


# -----------------------------------------------------------------------------

    def _validate_inputs_early(self, dom=None):
        """validate the inputs that can be validated ahead of time without
        any images, nics, etc.
        
        dom -- if present, this method is overloaded to fill the model instance
        
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
            self._validate_create_basics(dom)
    
        
    def _validate_create_basics(self, dom):
        
        name = self.p.get_arg_or_none(wc_args.NAME)
        if not name:
            raise InvalidInput("No name given for create")
        if dom:
            dom.name = name
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
            
        if dom:
            dom.memory = memory
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
            
        if dom:
            dom.vcpu = vcpus
        
# -----------------------------------------------------------------------------

    def _fill_model(self, local_file_set, nic_set, kernel):
        """Construct a valid model object for creation, the valid mode object
        can be converted directly to XML (via lvrt_xml routines) that can be
        used as input to libvirt's virDomainCreateXML operation.
        
        Steps to obtain a valid model object: a) validate the inputs, b) adjust
        what will be sent based on available features/configurations, c) call
        the appropriate lvrt_adapter* object to do driver-specific work on the
        model object (there is particular syntax necessary depending on which
        VMM is in use).
        
        Return complete and valid instance of lvrt_model.Domain
        """
        
        dom = lvrt_model.Domain()
        dom.os = lvrt_model.OS()
        dom.devices = lvrt_model.Devices()
        
        self._validate_inputs_early(dom)
        
        for lf in local_file_set.flist():
            disk = lvrt_model.Disk()
            disk.source = lf.path
            disk.target = lf.mountpoint
            disk.readonly = not lf.read_write
            dom.devices.disks.append(disk)
        
        for nic in nic_set.niclist():
            interface = lvrt_model.Interface()
            interface.source = nic.bridge
            interface.mac = nic.mac
            interface.target = nic.vifname
            dom.devices.interfaces.append(interface)
        
        self.intakeadapter.fill_model(dom, local_file_set, nic_set, kernel)
        
        return dom
