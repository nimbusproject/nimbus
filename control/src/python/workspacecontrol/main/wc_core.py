import string
import sys
import time

from workspacecontrol.api.exceptions import *
import wc_core_creation
import wc_core_persistence
import wc_core_propagation
import wc_deprecated
import workspacecontrol.experimental.tmplease as tmplease
from workspacecontrol.main import get_class_by_keyword, get_all_configs, ACTIONS
import workspacecontrol.main.wc_args as wc_args
    
# -----------------------------------------------------------------------------
# CORE LOGIC (this is the whole program)
# -----------------------------------------------------------------------------

def core(opts, dbgmsgs=None):
    """Run workspace-control.
    
    From here 'down' there is no concept of a commandline program, only
    'args' which could be coming from any kind of protocol based request.
    
    To make such a thing, construct an opts object with the expected
    member names and values and pass it in to this method.
    
    See the 'wc_args' module and the defaults 'Parameters' implementations to
    fully understand arg intake.  See the 'wc_cmdline' module to see how args
    are taken in and how the result of the program (no exception or exception)
    is translated into a return code.
    """
    
    # -------------------------------------------------------------------------
    # SETUP Parameters
    # -------------------------------------------------------------------------
    
    if not opts:
        raise InvalidInput("No arguments")
        
    # in the default deployment, this is added by the .sh script wrapper 
    if not opts.conf:
        raise InvalidInput("The path to the 'main.conf' file is required, see --help.")
        
    ac = get_all_configs(opts.conf)
    
    p_cls = get_class_by_keyword("Parameters", allconfigs=ac)
    p = p_cls(ac, opts)
    
    # -------------------------------------------------------------------------
    # REQUIRED arguments
    # -------------------------------------------------------------------------
    
    # --conf is also required; already checked for above
    
    given_action = p.get_arg_or_none(wc_args.ACTION)
    if not given_action:
        raise InvalidInput("The %s argument is required, see help" % wc_args.ACTION.long_syntax)
        
    action = validate_action(given_action)
    
    given_vm_name = p.get_arg_or_none(wc_args.NAME)
    if not given_vm_name:
        raise InvalidInput("The %s argument is required, see help" % wc_args.NAME.long_syntax)
    
    vm_name = validate_name(given_vm_name)
    
    # -------------------------------------------------------------------------
    # Common
    # -------------------------------------------------------------------------
    
    c_cls = get_class_by_keyword("Common", allconfigs=ac)
    c = c_cls(p)
    
    # now there is a logger finally:
    if dbgmsgs:
        c.log.debug(dbgmsgs)
        
    try:
        _core(vm_name, action, p, c)
    except Exception,e:
        c.log.exception(e)
        raise
        
def _core(vm_name, action, p, c):
        
    # -------------------------------------------------------------------------
    # INSTANTIATE the rest of the needed instances
    # -------------------------------------------------------------------------
    
    async_cls = c.get_class_by_keyword("AsyncNotification")
    async = async_cls(p, c)
    
    image_edit_cls = c.get_class_by_keyword("ImageEditing")
    editing = image_edit_cls(p, c)
    
    images_cls = c.get_class_by_keyword("ImageProcurement")
    images = images_cls(p, c)
    
    kernels_cls = c.get_class_by_keyword("KernelProcurement")
    kernels = kernels_cls(p, c)
    
    localnet_cls = c.get_class_by_keyword("LocalNetworkSetup")
    localnet = localnet_cls(p, c)
    
    netbootstrap_cls = c.get_class_by_keyword("NetworkBootstrap")
    netbootstrap = netbootstrap_cls(p, c)
    
    netlease_cls = c.get_class_by_keyword("NetworkLease")
    netlease = netlease_cls(p, c)
    
    netsecurity_cls = c.get_class_by_keyword("NetworkSecurity")
    netsecurity = netsecurity_cls(p, c)
    
    platform_cls = c.get_class_by_keyword("Platform")
    platform = platform_cls(p, c)
    
    # The following classes are not used in this method, this is to ensure
    # ahead of time that an implementation is configured for each object.
    c.get_class_by_keyword("DNS")
    c.get_class_by_keyword("Kernel")
    c.get_class_by_keyword("LocalFile")
    c.get_class_by_keyword("LocalFileSet")
    c.get_class_by_keyword("NIC")
    c.get_class_by_keyword("NICSet")
    c.get_class_by_keyword("RunningVM")
    
    # -------------------------------------------------------------------------
    # VALIDATE
    # -------------------------------------------------------------------------
    
    c.log.info("Validating '%s' action for '%s'" % (action, vm_name))
    
    async.validate()
    editing.validate()
    images.validate()
    kernels.validate()
    localnet.validate()
    netbootstrap.validate()
    netlease.validate()
    netsecurity.validate()
    platform.validate()
    
    persistence = wc_core_persistence.Persistence(p, c)
    persistence.validate()
    
    if p.get_arg_or_none(wc_args.VALIDATE_ONLY):
        # done.
        return
    
    # -------------------------------------------------------------------------
    # BRANCH on action
    # -------------------------------------------------------------------------
    
    if c.dryrun:
        c.log.info("Performing DRYRUN '%s' action for '%s'" % (action, vm_name))
    else:
        c.log.info("Performing '%s' action for '%s'" % (action, vm_name))
        
    running_vm = platform.info(vm_name)
    nic_set = persistence.get_nic_set(vm_name)
    
    if running_vm and not nic_set:
        infostr = running_vm_dump(running_vm)
        raise UnexpectedError("There is a VM running with this handle but there is no record of it in the program.  Cannot proceed.\n\n%s" % infostr)
    
    if action == ACTIONS.CREATE:
        
        wc_core_creation.create(vm_name, p, c, async, editing, images, kernels, localnet, netbootstrap, netlease, netsecurity, persistence, platform)
        
    elif action == ACTIONS.PRINTXML:
        
        if running_vm:
            c.log.warn("Received printxml request for VM with name '%s' but that was found running." % vm_name)
            
        wc_core_creation.printspec(vm_name, p, c, async, editing, images, kernels, localnet, netbootstrap, netlease, netsecurity, persistence, platform)
        
    elif action == ACTIONS.REMOVE:
        
        deleteall = p.get_arg_or_none(wc_args.DELETE_ALL)

        if not nic_set:
            # This is not an error because the 'job is done' (this applies to
            # situations like pilot/LRM where we want "assurance") 
            c.log.warn("Received shutdown/destroy request for a VM with name '%s', but there is no record of it in the program." % vm_name)
            
            # Note that this logic is actually "if not running_vm and not
            # nic_set" -- the "if running_vm and not nic_set" was already
            # screened and that is an error and a very different situation
            # than this.
            
            # Still can be files left over when there is no nic_set.  For 
            # example propagation happened but not create.
            if deleteall:
                vacate_images(c, images)
                c.log.info("vacated '%s' images from workspace-control (no nic_set)" % vm_name)
            return
        
        if running_vm:
            try:
                if deleteall:
                    platform.destroy(running_vm)
                    c.log.info("%s VM was destroyed" % vm_name)
                else:
                    graceful_shutdown(p, c, platform, vm_name, running_vm)
                    c.log.info("%s VM was gracefully shutdown" % vm_name)
            except Exception,e:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                errstr = "Issue with shutdown/destroy: %s: %s" % (str(exceptname), str(sys.exc_value))
                c.log.error(errstr)
        
        vacate_networking(c, netbootstrap, netsecurity, netlease, vm_name, nic_set, persistence)
        c.log.info("vacated '%s' from workspace-control (networking)" % vm_name)
        
        vacate_tmplease(p, c, vm_name)
                
        # If deleteall is not requested, the VM will now be moved back to
        # 'propagated' from the service's perspective.  There is no other
        # work to do.
        if not deleteall:
            return
            
        vacate_images(c, images)
        c.log.info("vacated '%s' from workspace-control (images)" % vm_name) 
        
    elif action == ACTIONS.INFO:
        
        if running_vm:
            print running_vm_dump(running_vm)
        else:
            raise UnexpectedError("%s not found, cannot run info" % vm_name)
            
    elif action == ACTIONS.REBOOT:
        
        if running_vm:
            platform.reboot(running_vm)
        else:
            raise UnexpectedError("%s not found, cannot reboot." % vm_name)
            
    elif action == ACTIONS.PAUSE:
        
        if running_vm:
            platform.pause(running_vm)
        else:
            raise UnexpectedError("%s not found, cannot pause." % vm_name)
            
    elif action == ACTIONS.UNPAUSE:
        
        if running_vm:
            platform.unpause(running_vm)
        else:
            raise UnexpectedError("%s not found, cannot unpause." % vm_name)
            
    elif action == ACTIONS.PROPAGATE:
        
        if running_vm:
            raise UnexpectedError("Received propagation request for VM with name '%s' but that was found running." % vm_name)
        
        wc_core_propagation.propagate(vm_name, c, async, images)
            
    elif action == ACTIONS.UNPROPAGATE:
        
        if running_vm:
            raise UnexpectedError("Received unpropagation request for VM with name '%s' but that was found running." % vm_name)
        
        wc_core_propagation.unpropagate(vm_name, c, async, images, editing)

    else:
        raise ProgrammingError("unhandled action %s" % action)

# -----------------------------------------------------------------------------
# For INFO action
# -----------------------------------------------------------------------------

def running_vm_dump(running_vm):
    if not running_vm:
        raise ProgrammingError("no running_vm")
        
    attrs = ["vmm_id", "vmm_uuid", "xmldesc", "curmem", "maxmem", "numvcpus", "cputime", "running", "blocked", "paused", "shutting_down", "shutoff", "crashed"]
    
    ret = "All information: %s\n" % running_vm.wchandle
    for attr in attrs:
        ret += "-----------------------------------------------------------\n"
        
        attrstr = getattr(running_vm, attr)
        
        if attrstr:
            ret += "%s:\n" % attr
            ret += "%s\n" % attrstr
        else:
            ret += "%s is empty.\n" % attr
        ret += "-----------------------------------------------------------\n"
        
    return ret

# -----------------------------------------------------------------------------
# For REMOVE action
# -----------------------------------------------------------------------------

def graceful_shutdown(p, c, platform, vm_name, running_vm):
    
    if running_vm.shutting_down:
        c.log.warn("Already in the process of shutting down '%s'" % vm_name)
    
    if running_vm.running or running_vm.blocked or running_vm.paused:
        platform.shutdown(running_vm)

    defaultwait = 15
    shutdown_wait = p.get_conf_or_none("vmshutdown", "shutdown_wait")
    if not shutdown_wait:
        shutdown_wait = defaultwait
    else:
        try:
            shutdown_wait = int(shutdown_wait)
        except:
            raise UnexpectedError("the configuration for 'shutdown_wait' is not an integer, this needs to be the number of seconds a graceful shutdown should wait before killing a VM.  Current config is '%s'" % str(shutdown_wait))
            
    # Check every $interval seconds if it's shut down yet.
    # If shutdown_wait number of seconds pass by and the platform still thinks
    # it exists in any capacity, destroy.
    interval = 0.5
    count = shutdown_wait
    while count > 0:
        time.sleep(interval)
        c.log.debug("checking on VM '%s'" % vm_name)
        running_vm = platform.info(vm_name)
        if running_vm:
            if running_vm.shutoff:
                c.log.debug("VM '%s' is now in the shutoff state" % vm_name)
                return
        else:
            c.log.debug("VM '%s' not present anymore" % vm_name)
            return
        count -= interval

    # while loop finished without returning:
    c.log.error("forced to kill the '%s' VM, the configured amount of time (%d seconds) has passed without a graceful shutdown" % (vm_name, shutdown_wait))
    platform.destroy(running_vm)
    c.log.error("destroyed successfully: %s" % vm_name)

def vacate_images(c, images):
    
    try:
        local_file_set = images.obtain()
        images.process_after_destroy(local_file_set)
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        errstr = "Possible issue with image teardown: %s: %s" % (str(exceptname), str(sys.exc_value))
        c.log.error(errstr)
        
def vacate_networking(c, netbootstrap, netsecurity, netlease, vm_name, nic_set, persistence):
        
    try:
        netbootstrap.teardown(nic_set)
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        errstr = "Possible issue with netbootstrap teardown: %s: %s" % (str(exceptname), str(sys.exc_value))
        c.log.error(errstr)
        
    try:
        netsecurity.teardown(nic_set)
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        errstr = "Possible issue with netsecurity teardown: %s: %s" % (str(exceptname), str(sys.exc_value))
        c.log.error(errstr)
        
    try:
        netlease.release(vm_name, nic_set)
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        errstr = "Possible issue with netlease teardown: %s: %s" % (str(exceptname), str(sys.exc_value))
        c.log.error(errstr)
    
    persistence.remove_nic_set(vm_name)
    c.log.debug("removed VM from persistence")

def vacate_tmplease(p, c, vm_name):
    
    try:
        tmplease.teardown(p, c, vm_name)
        c.log.debug("removed tmplease, if any")
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        errstr = "Possible issue with tmpplease teardown: %s: %s" % (str(exceptname), str(sys.exc_value))
        c.log.error(errstr)
    

# -----------------------------------------------------------------------------
# GLOBAL VALIDATIONS
# -----------------------------------------------------------------------------

def validate_name(name):
    """Return normalized name.
    No spaces allowed.
    Only letters, -, and numbers allowed.
    Length may not be >13
    
    These restrictions are because w-c uses the name for more than logging:
    things like directory names and interface names -- in current nimbus scheme
    of "wrksp-123" this caps possible instances deployed at 10million and NICs
    at 9 per VM.
    
    """
    name = string.strip(name)
    name = string.lower(name)
    if not name:
        raise InvalidInput("name is missing/empty")
        
    if name[0] not in string.lowercase:
        raise InvalidInput("name must begin with a letter")
        
    ok = string.lowercase + "-" + string.digits
    for character in name:
        if character not in ok:
            raise InvalidInput("name is invalid, character '%s' is not allowed. The only allowed characters are '%s'" % (character, ok))
    
    maxlen = 13
    if len(name) > maxlen:
        raise InvalidInput("name is invalid, too long. Must be less than %d" % (maxlen+1))
        
    return name

def validate_action(action):
    action = string.strip(action)
    action = string.lower(action)
    if not action:
        raise InvalidInput("action is missing/empty")
                          
    if action not in ACTIONS.ALL:
        raise InvalidInput("Unknown action: '%s'" % action)
        
    return action
