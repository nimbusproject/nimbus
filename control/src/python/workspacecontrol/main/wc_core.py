import string

from workspacecontrol.api.exceptions import *
import wc_core_creation
import wc_core_persistence
import wc_core_propagation
import wc_deprecated
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
                local_file_set = images.obtain()
                images.process_after_destroy(local_file_set)
            
            return
        
        if running_vm:
            if deleteall:
                platform.destroy(running_vm)
                running_vm = platform.info(vm_name)
                
            else:
                if running_vm.shutting_down:
                    c.log.warn("Already in the process of shutting down '%s'" % vm_name)
                
                if running_vm.running or running_vm.blocked or running_vm.paused:
                    platform.shutdown(running_vm)
            
                # TODO: wait loop... for how long: that was old
                #       checkshutdownpause config.  TODO: do with live system
                running_vm = platform.info(vm_name)
            
        if running_vm:
            raise ProgrammingError("Cannot proceed until the VMM is removed entirely from the hypervisor")
            
        # If deleteall is not requested, the VM will now be moved back to
        # 'propagated' from the service's perspective.  There is no other
        # work to do.
        if not deleteall:
            return
            
        # deleteall requested, vacate everything forcefully
        
        local_file_set = images.obtain()
        images.process_after_destroy(local_file_set)
        netbootstrap.teardown(nic_set)
        netsecurity.teardown(nic_set)
        netlease.release(nic_set)
        
        persistence.remove_nic_set(vm_name)
        
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
        
        wc_core_propagation.unpropagate(vm_name, c, async, images)
    
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
