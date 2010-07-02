from workspacecontrol.api.exceptions import *
from commands import getstatusoutput
import os

def _common_validation(p, c):
    # All of the following checks violate the "up front" validation philosophy
    # of the rest of the workspace-control components.
    
    tmplease_exe = p.get_conf_or_none("tmplease", "tmplease")
        
    if not tmplease_exe:
        raise InvalidConfig("physical partition leasing (tmplease) is enabled but there is no tmplease exe setting")
    
    if not os.path.isabs(tmplease_exe):
        tmplease_exe = c.resolve_libexec_dir(tmplease_exe)
    
    if not os.access(tmplease_exe, os.F_OK):
        raise InvalidConfig("tmplease tool does not exist: '%s'" % tmplease_exe)
        
    if not os.access(tmplease_exe, os.X_OK):
        raise InvalidConfig("tmplease tool is not executable: '%s'" % tmplease_exe)

    c.log.debug("tmplease tool configured: %s" % tmplease_exe)
    
    sudo_path = p.get_conf_or_none("sudo", "sudo")
    if not sudo_path:
        raise InvalidConfig("tmplease tool is configured but there is no sudo configuration")
        
    if not os.path.isabs(sudo_path):
        raise InvalidConfig("path to sudo must be absolute")
        
    if not os.access(sudo_path, os.F_OK):
        raise InvalidConfig("sudo is configured with an absolute path, but it does not seem to exist: '%s'" % sudo_path)
        
    if not os.access(sudo_path, os.X_OK):
        raise InvalidConfig("sudo is configured with an absolute path, but it does not seem executable: '%s'" % sudo_path)

    c.log.debug("sudo configured for tmplease: %s %s" % (sudo_path, tmplease_exe))
    
    return (tmplease_exe, sudo_path)

def _create_validation(p, c, local_file_set, vmname):
    # All of the following checks violate the "up front" validation philosophy
    # of the rest of the workspace-control components.
        
    (tmplease_exe, sudo_path) = _common_validation(p, c)
    
    vmpartition = p.get_conf_or_none("tmplease", "vmpartition")
    if not vmpartition:
        raise InvalidConfig("tmplease tool is configured but there is no vmpartition configuration")
    
    fail_if_present_conf = p.get_conf_or_none("tmplease", "fail_if_present")
    if not fail_if_present_conf:
        fail_if_present = False
    elif fail_if_present_conf.strip().lower() == "true":
        fail_if_present = True
    else:
        fail_if_present = False
        
    return (tmplease_exe, sudo_path, vmpartition, fail_if_present)

def is_enabled(p, c):
    enabled = False
    tmplease_enabled = p.get_conf_or_none("tmplease", "enabled")
    if tmplease_enabled and tmplease_enabled.strip().lower() == "true":
        enabled = True
    if not enabled:
        c.log.debug("no tmplease configuration (images.conf), physical partition leasing disabled")
        return False
    return True

def teardown(p, c, vmname):
    
    if not is_enabled(p, c):
        return
        
    (tmplease_exe, sudo_path) = _common_validation(p, c)
            
    cmd = "%s %s rem %s" % (sudo_path, tmplease_exe, vmname)
    c.log.debug("command = '%s'" % cmd)
    if c.dryrun:
        c.log.debug("(dryrun, didn't run that)")
        return

    ret,output = getstatusoutput(cmd)
    if ret:
        errmsg = "problem running command: '%s' ::: return code" % cmd
        errmsg += ": %d ::: output:\n%s" % (ret, output)
        c.log.error(errmsg)
        raise UnexpectedError(errmsg)
    
def setup(p, c, local_file_set, vmname):
    
    if not is_enabled(p, c):
        return local_file_set
        
    (tmplease_exe, sudo_path, vmpartition, fail_if_present) = \
            _create_validation(p, c, local_file_set, vmname)
    
    target_is_present = False
    for lf in local_file_set.flist():
        if lf.mountpoint == vmpartition:
            target_is_present = True

    if target_is_present:
        if fail_if_present:
            raise InvalidInput("There is a mountpoint in your deployment ('%s') that matches the partition that temp space will be mounted to." % vmpartition)
        else:
            c.log.warn("There is a mountpoint in the deployment ('%s') that matches the partition that temp space will be mounted to." % vmpartition)
            
            # DONE
            return local_file_set
            
    cmd = "%s %s add %s" % (sudo_path, tmplease_exe, vmname)
    c.log.debug("command = '%s'" % cmd)
    if c.dryrun:
        c.log.debug("(dryrun, didn't run that)")
        return local_file_set

    ret,output = getstatusoutput(cmd)
    if ret:
        errmsg = "problem running command: '%s' ::: return code" % cmd
        errmsg += ": %d ::: output:\n%s" % (ret, output)
        c.log.error(errmsg)
        raise UnexpectedError(errmsg)
        
    c.log.debug("leased tmp space physical partition successfully: %s" % cmd)
    c.log.debug("tmp space physical partition: %s" % output)
    output="/dev/sdx1"
    if not output:
        raise UnexpectedError("no output from partition leasing")
        
    if not output.startswith("/dev/"):
        raise UnexpectedError("output from partition leasing does not start with '/dev/'")
        
    if "\n" in output:
        raise UnexpectedError("output from partition leasing has newlines")
    
    if len(output) > 10 or len(output) < 7:
        raise UnexpectedError("output from partition leasing has suspicious length")
        
    lf_cls = c.get_class_by_keyword("LocalFile")
    lf = lf_cls()
        
    
    # -------------------------------------------------------------------------
    # This all represents an abstraction violation but that is OK: this feature
    # is something that needs to be "built in" to the image procurement module
    # in the future.  This is experimental...
    
    # this is a physical partition, different from blankspace feature
    lf._blankspace = 0
    lf._propagate_needed = False
    lf._propagation_source = None
    lf._unpropagate_needed = False
    lf._unpropagation_target = None
    
    # These are the fields the LocalFile interface expects:
    lf.path = output
    lf.mountpoint = vmpartition
    lf.rootdisk = False
    lf.editable = True
    lf.read_write = True
    lf.physical = True # <---
    
    lfs = local_file_set.flist()
    lfs.append(lf)
    local_file_set_cls = c.get_class_by_keyword("LocalFileSet")
    new_local_file_set = local_file_set_cls(lfs)
    
    return new_local_file_set
    
    