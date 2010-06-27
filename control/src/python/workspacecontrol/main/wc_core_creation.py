from workspacecontrol.api.exceptions import *
import workspacecontrol.main.wc_args as wc_args
import workspacecontrol.defaults.NetworkLease as NetworkLease
import wc_daemonize
import wc_core_propagation

def create(vm_name, p, c, async, editing, images, kernels, localnet, netbootstrap, netlease, netsecurity, persistence, platform):
    
    if images.lengthy_obtain():
        # create+propagate
        # this function returns quickly, all work (for both create and
        # propagate) happens under daemonization
        _propagate_then_create(vm_name, p, c, async, editing, images, kernels, localnet, netbootstrap, netlease, netsecurity, persistence, platform)
    else:
        _create_local(vm_name, p, c, async, editing, images, kernels, localnet, netbootstrap, netlease, netsecurity, persistence, platform)
        
# ----------------------------------------------------------------------------
# _create_local()
# ----------------------------------------------------------------------------
        
def _create_local(vm_name, p, c, async, editing, images, kernels, localnet, netbootstrap, netlease, netsecurity, persistence, platform):
    
    c.log.debug("_create_local()")
    
    local_file_set = images.obtain()
    
    _common(local_file_set, vm_name, p, c, editing, kernels, localnet, netbootstrap, netlease, netsecurity, persistence, platform)
    
def _common(local_file_set, vm_name, p, c, editing, kernels, localnet, netbootstrap, netlease, netsecurity, persistence, platform):
    """To understand _common(), it might make sense to go look at
    "_common_withnetbootstrap()" first and work backwards
    """
    
    if c.trace:
        c.log.debug("_common()")
    
    kernel = kernels.kernel_files(local_file_set)
    if not kernel:
        raise ProgrammingError("no kernel was chosen")
        
    dbgstring = "kernel chosen, "
    if kernel.onboard_kernel:
        dbgstring += "onboard kernel"
    else:
        dbgstring += "path '%s'" % kernel.kernel_path
        if kernel.initrd_path:
            dbgstring += ", initrd path '%s'" % kernel.initrd_path
    c.log.debug(dbgstring)
        
    editing.process_after_procurement(local_file_set)
    if c.trace:
        c.log.debug("done: editing.process_after_procurement()")
        
    nicset_cls = c.get_class_by_keyword("NICSet")
    
    nic_names, network_names = _discover_nic_and_network_names(p, c)
    if len(nic_names) != len(network_names):
        raise ProgrammingError("expecting the same amount of nic_names and network_names")
    
    # once even one NIC is reserved, any error must release leases
    nic_set = nicset_cls([])
    try:
            
        for i,nic_name in enumerate(nic_names):
            nic = netlease.obtain(vm_name, nic_name, network_names[i])
            if not nic:
                raise ProgrammingError("no nic was leased")
                
            niclist = nic_set.niclist()
            niclist.append(nic)
            # not enough to append to list, the NicSet interface does not
            # guarantee you've got the "actual" list there, it could be a
            # 'defensively copied' view of it.
            nic_set = nicset_cls(niclist)
            
            oktorepl = False
            if i > 0:
                oktorepl = True
            persistence.store_nic_set(vm_name, nic_set, ok_to_replace=oktorepl)
        
        if c.trace:
            dbstring = "Obtained NIC leases: "
            for i,nic in enumerate(nic_set.niclist()):
                if i > 0:
                    dbstring += ", "
                dbstring += str(nic.ip)
            c.log.debug(dbstring)
        
        localnet.choose_vifnames(nic_set, vm_name)
        persistence.store_nic_set(vm_name, nic_set, ok_to_replace=True)
        
        _common_withnics(nic_set, kernel, local_file_set, c, localnet, netbootstrap, netsecurity, platform)
        
    except Exception,e:
        
        c.log.exception(e)
        try:
            c.log.error("Creation problem: going to back out net leases")
            netlease.release(vm_name, nic_set)
            c.log.error("Backed out net leases")
        except Exception,e2:
            c.log.exception(e2)
        raise e
    
def _common_withnics(nic_set, kernel, local_file_set, c, localnet, netbootstrap, netsecurity, platform):
    
    if c.trace:
        c.log.debug("_common_withnics()")
    
    for nic in nic_set.niclist():
        nic.bridge = localnet.ip_to_bridge(nic.ip)
    
    netbootstrap.setup(nic_set)
    try:
        _common_withnetbootstrap(nic_set, kernel, local_file_set, c, netsecurity, platform)
    except Exception,e:
        c.log.exception(e)
        try:
            c.log.error("Creation problem: going to back out net bootstrap")
            netbootstrap.teardown(nic_set)
            c.log.error("Backed out net bootstrap")
        except Exception,e2:
            c.log.exception(e2)
        raise e
    
def _common_withnetbootstrap(nic_set, kernel, local_file_set, c, netsecurity, platform):
    
    if c.trace:
        c.log.debug("_common_withnetbootstrap()")
    
    netsecurity.setup(nic_set)
    try:
        platform.create(local_file_set, nic_set, kernel)
    except Exception,e:
        c.log.exception(e)
        try:
            c.log.error("Creation problem: going to back out net security")
            netsecurity.teardown(nic_set)
            c.log.error("Backed out net security")
        except Exception,e2:
            c.log.exception(e2)
        raise e

def _discover_nic_and_network_names(p, c):
    
    # In the future these will provided by direct arguments/parameters, right
    # now this knowledge is embedded in the ugly networking parameter.
    # Relying on direct implementation knowledge, so instantiate by actual
    # class knowledge and not through D.I.
    
    arg = p.get_arg_or_none(wc_args.NETWORKING)
    if not arg:
        raise InvalidInput("networking is required")
    
    nic_names = []
    network_names = []
    
    ugly_temporarily = NetworkLease.DefaultNetworkLease(p, c)
    nics = ugly_temporarily._parse_arg(arg)
    for nic in nics:
        nic_names.append(nic.name)
        network_names.append(nic.network)
    return nic_names, network_names
    
# ----------------------------------------------------------------------------
# _propagate_then_create()
# ----------------------------------------------------------------------------
     
def _propagate_then_create(vm_name, p, c, async, editing, images, kernels, localnet, netbootstrap, netlease, netsecurity, persistence, platform):
    """This returns quickly, all work (for both create and propagate)
       happens under daemonization.
    """
    
    c.log.debug("_propagate_then_create()")
    
    if not images.lengthy_obtain():
        c.log.info("Propagation asked for but it is not required.")
        return
        
    func = _propagate_then_create_under_daemonization
    
    args = [vm_name, p, c, async, editing, images, kernels, localnet, netbootstrap, netlease, netsecurity, persistence, platform]
    
    wc_daemonize.daemonize(c, func, args)
    
# the following function runs directly under the daemonization harness
def _propagate_then_create_under_daemonization(vm_name, p, c, async, editing, images, kernels, localnet, netbootstrap, netlease, netsecurity, persistence, platform):
    
    local_file_set = wc_core_propagation.propagate_under_daemonization(vm_name, c, async, images)
    
    if not local_file_set:
        c.log.error("propagation failed, cannot propagate+create, exiting")
        return
    
    try:
        _common(local_file_set, vm_name, p, c, editing, kernels, localnet, netbootstrap, netlease, netsecurity, persistence, platform)
    except Exception,e:
        c.log.error("Problem with create during daemonized propagate+create")
        c.log.exception(e)
        
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        errstr = "Problem with create during daemonized propagate+create: %s: %s" % (str(exceptname), str(sys.exc_value))
        common.log.error(errstr)
        common.log.error("Notifying service of creation failure")
        try:
            async.notify(vm_name, "start", 7, errstr)
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            errstr = "Problem notifying service about creation failure: %s: %s" % (str(exceptname), str(sys.exc_value))
            common.log.error(errstr)
            
        return None
        
    try:
        async.notify(vm_name, "start", 0, None)
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        errstr = "Problem notifying service about creation success: %s: %s" % (str(exceptname), str(sys.exc_value))
        common.log.error(errstr)
        
    c.log.info("propagate+create was successful")
    