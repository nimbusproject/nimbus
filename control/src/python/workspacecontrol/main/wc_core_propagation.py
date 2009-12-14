import sys
import wc_daemonize

def propagate(vm_name, common, async, images):
    if not images.lengthy_obtain():
        c.log.info("Propagation asked for but it is not required.")
        return
        
    args = [vm_name, common, async, images]
    
    wc_daemonize.daemonize(common, propagate_under_daemonization, args)
    
# The following method is called in two (known) circumstances
# 1. Directly under the daemonization harness
# 2. As part of the create+propagate logic in the wc_core_creation module,
#    another function drives the process under the daemonization harness and
#    then calls this as needed.  That expects the local_file_set return value.

def propagate_under_daemonization(vm_name, common, async, images):
    local_file_set = None
    try:
        local_file_set = images.obtain()
    except Exception,e:
        common.log.error("Problem propagating.")
        common.log.exception(e)
        
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        errstr = "Problem propagating: %s: %s" % (str(exceptname), str(sys.exc_value))
        common.log.error(errstr)
        common.log.error("Notifying service of propagation failure")
        try:
            async.notify(vm_name, "propagate", 7, errstr)
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            errstr = "Problem notifying service about propagation failure: %s: %s" % (str(exceptname), str(sys.exc_value))
            common.log.error(errstr)
            
        return None
        
    try:
        c.log.info("propagation was successful")
        async.notify(vm_name, "propagate", 0, None)
        c.log.info("notification of propagation success was successful")
        return local_file_set
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        errstr = "Problem notifying service about propagation success: %s: %s" % (str(exceptname), str(sys.exc_value))
        common.log.error(errstr)
        return None
    
def unpropagate(vm_name, common, async, images):
    if not images.lengthy_shutdown():
        c.log.info("Unpropagation asked for but it is not required.")
        return
    
    args = [vm_name, common, async, images]
    wc_daemonize.daemonize(common, unpropagate_under_daemonization, args)
    
def unpropagate_under_daemonization(vm_name, common, async, images):
    try:
        images.process_after_shutdown()
    except Exception,e:
        common.log.error("Problem unpropagating.")
        common.log.exception(e)
        
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        errstr = "Problem unpropagating: %s: %s" % (str(exceptname), str(sys.exc_value))
        common.log.error(errstr)
        common.log.error("Notifying service of unpropagation error")
        try:
            async.notify(vm_name, "unpropagate", 7, errstr)
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            errstr = "Problem notifying service about unpropagation failure: %s: %s" % (str(exceptname), str(sys.exc_value))
            common.log.error(errstr)
            
        return False
        
    try:
        c.log.info("unpropagate was successful")
        async.notify(vm_name, "unpropagate", 0, None)
        c.log.info("notification of unpropagation success was successful")
        return True
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        errstr = "Problem notifying service about unpropagation success: %s: %s" % (str(exceptname), str(sys.exc_value))
        common.log.error(errstr)
        return False
