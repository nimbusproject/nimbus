import os
import string
import sys
import zope.interface

import workspacecontrol.api.modules
from workspacecontrol.api.exceptions import *
import workspacecontrol.main.wc_args as wc_args

class AuthzKernel:
    def __init__(self):
        self.name = None
        self.abspath = None
        self.initrd_abspath = None

class DefaultKernelProcurement:
    """KernelProcurement is the wcmodule responsible for picking the proper
    kernel file(s) and making them accessible to the current VMM node before
    the deployment.
    """
    
    zope.interface.implements(workspacecontrol.api.modules.IKernelProcurement)
    
    def __init__(self, params, common):
        self.p = params
        self.c = common
        self.authz_kernels = None # list of AuthzKernel instances
        
    def validate(self):
        kerneldir = self.p.get_conf_or_none("kernels", "kerneldir")
        if not kerneldir:
            raise InvalidConfig("no kernels->kerneldir configuration")
            
        if not os.path.isabs(kerneldir):
            kerneldir = self.c.resolve_var_dir(kerneldir)
        self.c.log.debug("kernel directory: %s" % kerneldir)
            
        authz_conf = self.p.get_conf_or_none("kernels", "authz_kernels")
        if not authz_conf:
            raise InvalidConfig("no kernels->authz_kernels configuration")
        
        self.authz_kernels = []
        ak_list = authz_conf.split(",")
        for name in ak_list:
            name = string.strip(name)
            if os.path.isabs(name):
                raise InvalidConfig("cannot accept absolute path in kernels->authz_kernels configuration: '%s'" % name)
            authzk = AuthzKernel()
            authzk.name = name
            authzk.abspath = os.path.join(kerneldir, name)
            
            if not os.access(authzk.abspath, os.R_OK):
                raise InvalidConfig("cannot read the configured kernel file: '%s'" % authzk.abspath)
            
            self.authz_kernels.append(authzk)
            # (note if there are identical names, it will not matter)
            
        if len(self.authz_kernels) == 0:
            self.c.log.warn("no authorized kernels; can start hdimages only")
            return
            
        matchramdisk = self.p.get_conf_or_none("kernels", "matchramdisk")
        if matchramdisk:
            self.c.log.debug("looking for initrds, matchramdisk configuration '%s'" % matchramdisk)
            
            for authzk in self.authz_kernels:
                initrdfile = authzk.abspath + matchramdisk
                if os.access(initrdfile, os.F_OK):
                    authzk.initrd_abspath = initrdfile
                    if not os.access(initrdfile, os.R_OK):
                        raise InvalidConfig("initrd file exists but cannot be read by this user: '%s'" % initrdfile)
        else:
            self.c.log.debug("will not look for any initrds, no matchramdisk configuration")
            
        authz_txt = ""
        for authzk in self.authz_kernels:
            authz_txt += "  - '%s' (initrd: " % authzk.name
            if authzk.initrd_abspath:
                authz_txt += "yes)"
            else:
                authz_txt += "no)"
                
            authz_txt += "\n"
        self.c.log.debug("\n\nActive authorized kernels:\n%s" % authz_txt)
        
    def _check_hdimage(self, local_file_set, kernel_arg):
        
        found_rootdisk = False
        hdimage = False
        
        # simple check: if ~sda1, then partition, if ~xvda then hdimage
        for lf in local_file_set.flist():
            if lf.rootdisk:
                if found_rootdisk:
                    raise UnexpectedError("found more than one root disk, cannot proceed")
                found_rootdisk = True
                if not lf.mountpoint[-1].isdigit():
                    hdimage = True
                    self.c.log.debug("determined VM's root disk is hdimage")
            
        if hdimage:
            if kernel_arg:
                raise UnexpectedError("kernel override specified but root disk is a hard disk image...")
                
            # create a special kernel instance that triggers pygrub etc.
            kernel_cls = self.c.get_class_by_keyword("Kernel")
            kernel = kernel_cls()
            kernel.onboard_kernel = True
            return kernel
            
        # convenient time to make that sanity check:
        if not found_rootdisk:
            raise UnexpectedError("there is no root disk, cannot proceed")
            
        return None
        
    def _pick_an_authz_kernel(self, authzk, kernelargs_arg):
        kernel_cls = self.c.get_class_by_keyword("Kernel")
        kernel = kernel_cls()
        kernel.onboard_kernel = False
        kernel.kernel_path = authzk.abspath
        kernel.initrd_path = authzk.initrd_abspath
        kernel.kernel_args = kernelargs_arg
        return kernel
    
    def kernel_files(self, local_file_set):
        """
        local_file_set -- instance of LocalFileSet
        
        Return an IKernel instance appropriate to the inputs
        UnexpectedError if none can be found (may not return None)
        """
        
        # don't use "if not self.authz_kernels" because an empty list is OK
        if self.authz_kernels is None:
            self.validate()
        
        # Not carrying over "allow_guestkernel_override" setting, kernel is
        # always required to be in kerneldir for now.
        
        kernel_arg = self.p.get_arg_or_none(wc_args.KERNEL)
        
        kernel = self._check_hdimage(local_file_set, kernel_arg)
        if kernel:
            return kernel
        
        if len(self.authz_kernels) == 0:
            raise UnexpectedError("there are no authorized kernels and this is not a hdimage rootdisk: cannot proceed")
        
        kernelargs_arg = self.p.get_arg_or_none(wc_args.KERNELARGS)
        if kernelargs_arg:
            allowargs = self.p.get_conf_or_none("kernels", "allow_kernelargs")
            if allowargs != "true":
                raise InvalidInput("Supplied " + wc_args.KERNELARGS.long_syntax + " but kernel arguments have been disabled")
        
        if not kernel_arg:
            return self._pick_an_authz_kernel(self.authz_kernels[0], kernelargs_arg)
        
        for authzk in self.authz_kernels:
            if authzk.name == kernel_arg:
                return self._pick_an_authz_kernel(authzk, kernelargs_arg)
            
        raise UnexpectedError("kernel requested was '%s' but this is not in the authorized kernel list, try again with no special request to get the default kernel/initrd")
