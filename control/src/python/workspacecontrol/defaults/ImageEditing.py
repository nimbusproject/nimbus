from commands import getstatusoutput
import os
import shutil
import stat
import sys
import zope.interface

import workspacecontrol.api.modules
from workspacecontrol.api.exceptions import *
import workspacecontrol.main.wc_args as wc_args

class DefaultImageEditing:
    
    """ImageEditing is the wcmodule responsible for taking a procured set of
    files and performing any last minute changes on them.  After a deployment,
    the teardown hook *may* be called (for example, it is not called if there
    is an immediate-destroy event because that needs no unpropagation).
    
    This includes, but is not limited to: mounting and editing images,
    compression, decompression, partition extension.
    
    ---------------
    
    This implementation of ImageEditing will run a mount+edit script and
    convert files to and from ".gz"
    
    """
    
    zope.interface.implements(workspacecontrol.api.modules.IImageEditing)
    
    def __init__(self, params, common):
        self.p = params
        self.c = common
        self.sudo_path = None
        self.mounttool_path = None
        self.mountdir = None
        self.tmpdir = None
        
    def validate(self):
        self.mounttool_path = self.p.get_conf_or_none("mount", "mounttool")
        if not self.mounttool_path:
            self.c.log.warn("no mount tool configuration, mount+edit functionality is disabled")
            
        # if functionality is disabled but arg exists, should fail program
        self._validate_args_if_exist()
            
        if self.mounttool_path:
            self._validate_exes()
            self._validate_mountdir()
            self._validate_tmpdir()
    
    # --------------------------------------------------------------------------
    # validate() IMPLs
    # --------------------------------------------------------------------------
      
    def _validate_args_if_exist(self):
        
        mnttasks = self.p.get_arg_or_none(wc_args.MOUNT_TASKS)
        if not mnttasks:
            return []
            
        if not self.mounttool_path:
            raise UnexpectedError("mount+edit tasks were requested but the functionality is disabled")
            
        # Given input string might be quoted to escape semicolons
        # for certain delivery methods (e.g., sh over ssh) and
        # some methods may not strip quotes (e.g., localhost, direct
        # exe invocation)
        # so strip extra quotes if present
        if mnttasks[0] == "'":
            mnttasks = mnttasks[1:]
            
        # (there is a pathological case where input was only a single quote...)
        if mnttasks and mnttasks[-1] == "'":
            mnttasks = mnttasks[:-1]
           
        tasks = mnttasks.split(';;')
        
        self.c.log.debug("found %d mount+copy tasks" % len(tasks))
        
        mnttask_list = []
        for task in tasks:
            parts = task.split(';')
            if len(parts) != 2:
                raise InvalidInput("fatal, invalid mount+copy task: '%s'" % task)
            else:
                mnttask_list.append( (parts[0],parts[1]) )
                
        for task in mnttask_list:
            self.c.log.info("mount+copy task: '%s' --> rootdisk: '%s'" % (task[0], task[1]))
                
        return mnttask_list
            
    def _validate_exes(self):
        if not os.path.isabs(self.mounttool_path):
            self.mounttool_path = self.c.resolve_libexec_dir(self.mounttool_path)
        
        if not os.access(self.mounttool_path, os.F_OK):
            raise InvalidConfig("mounttool does not exist: '%s'" % self.mounttool_path)
            
        if not os.access(self.mounttool_path, os.X_OK):
            raise InvalidConfig("mounttool is not executable: '%s'" % self.mounttool_path)

        self.c.log.info("mount+edit tool configured: %s" % self.mounttool_path)
        
        self.sudo_path = self.p.get_conf_or_none("sudo", "sudo")
        if not self.sudo_path:
            raise InvalidConfig("mount tool is configured but there is no sudo configuration")
            
        if not os.path.isabs(self.sudo_path):
            raise InvalidConfig("path to sudo must be absolute")
            
        if not os.access(self.sudo_path, os.F_OK):
            raise InvalidConfig("sudo is configured with an absolute path, but it does not seem to exist: '%s'" % self.sudo_path)
            
        if not os.access(self.sudo_path, os.X_OK):
            raise InvalidConfig("sudo is configured with an absolute path, but it does not seem executable: '%s'" % self.sudo_path)

        self.c.log.info("sudo configured: %s" % self.sudo_path)

    def _validate_mountdir(self):
        mountdir = self.p.get_conf_or_none("mount", "mountdir")
        if not mountdir:
            raise InvalidConfig("mounttool is configured but there is no mountdir configuration which is required")
            
        if not os.path.isabs(mountdir):
            mountdir = self.c.resolve_var_dir(mountdir)
            
        if not os.path.exists(mountdir):
            try:
                os.mkdir(mountdir)
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                errstr = "problem creating mountdir '%s': %s: %s" % (mountdir, str(exceptname), str(sys.exc_value))
                
                fullerrstr = "mountdir does not have valid permissions and cannot be made to have valid permissions: '%s'" % mountdir
                fullerrstr += errstr
                self.c.log.error(fullerrstr)
                raise IncompatibleEnvironment(fullerrstr)
            
        # Needs to be writable so we can make subdirectories.
        # Using one mnt directory for all workspaces would mean 
        # we'd have to lock the mounttool callout to prevent races
        if os.access(mountdir, os.W_OK | os.X_OK | os.R_OK):
            self.c.log.debug("mount directory is rwx-able: %s" % mountdir)
        else:
            try:
                os.chmod(mountdir, stat.S_IRUSR | stat.S_IWUSR | stat.S_IXUSR)
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                errstr = " - problem changing mountdir permissions '%s': %s: %s" % (mountdir, str(exceptname), str(sys.exc_value))
                
                fullerrstr = "mountdir does not have valid permissions and cannot be made to have valid permissions: '%s'" % mountdir
                fullerrstr += errstr
                self.c.log.error(fullerrstr)
                raise IncompatibleEnvironment(fullerrstr)
                
            self.c.log.debug("mount directory was made to be rwx-able: %s" % mountdir)
            
        self.mountdir = mountdir
                
    def _validate_tmpdir(self):
        tmpdir = self.p.get_conf_or_none("mount", "tmpdir")
        if not tmpdir:
            raise InvalidConfig("mounttool is configured but there is no tmpdir configuration which is required")
            
        if not os.path.isabs(tmpdir):
            tmpdir = self.c.resolve_var_dir(tmpdir)
            
        if not os.path.exists(tmpdir):
            try:
                os.mkdir(tmpdir)
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                errstr = " - problem creating tmpdir '%s': %s: %s" % (tmpdir, str(exceptname), str(sys.exc_value))
                
                fullerrstr = "tmpdir does not have valid permissions and cannot be made to have valid permissions: '%s'" % tmpdir
                fullerrstr += errstr
                self.c.log.error(fullerrstr)
                raise IncompatibleEnvironment(fullerrstr)
            
        if os.access(tmpdir, os.R_OK):
            self.c.log.debug("tmp directory (for mount tasks) is readable: %s" % tmpdir)
        else:
            raise IncompatibleEnvironment("tmp directory (for mount tasks) is not readable: %s" % tmpdir)
        
        self.tmpdir = tmpdir
    
    # --------------------------------------------------------------------------
    # process_after_procurement(), from ImageEditing interface
    # --------------------------------------------------------------------------
    
    def process_after_procurement(self, local_file_set, dryrun=False):
        """Do any necessary work after all files are local or otherwise
        accessible but before a VM launches.
        
        local_file_set -- instance of LocalFileSet
        
        Return nothing, local_file_set will be modified as necessary.
        """
        
        for lf in local_file_set.flist():
            if lf.path[-3:] == ".gz":
                lf.path = self._gunzip_file_inplace(lf.path, dryrun)
        
        # disabled
        if not self.mounttool_path:
            return
        
        mnttask_list = self._validate_args_if_exist()
        if len(mnttask_list) > 0:
            
            vm_name = self.p.get_arg_or_none(wc_args.NAME)
            if not vm_name:
                raise InvalidInput("The %s argument is required." % wc_args.NAME.long_syntax)
            
            rootdisk = None
            for lf in local_file_set.flist():
                if lf.rootdisk:
                    rootdisk = lf.path
                    break
            
            if not rootdisk:
                raise InvalidInput("there is no root disk to perform the mount+edit tasks on")
            
            self._doMountCopyTasks(rootdisk, vm_name, mnttask_list, dryrun)
        
    # --------------------------------------------------------------------------
    # process_after_shutdown(), from ImageEditing interface
    # --------------------------------------------------------------------------
    
    def process_after_shutdown(self, local_file_set, dryrun=False):
        """Do any necessary work after a VM shuts down and is being prepared
        for teardown.  Will not be called if there is an immediate-destroy
        event because that needs no unpropagation.
        
        local_file_set -- instance of LocalFileSet
        
        Return nothing, local_file_set will be modified as necessary.
        """
        
        for lf in local_file_set.flist():
            try:
                lf._unpropagation_target
            except AttributeError:
                raise ProgrammingError("this image editing implementation is tied to the default procurement implementation with respect to the process_after_shutdown method. If you are running into this error, either implement the '_unpropagation_target' attribute as well, or come up with new arguments to the program for expressing compression needs in a saner way than looking for '.gz' in paths (which is tenuous)")
            if lf._unpropagation_target[-3:] == ".gz":
                lf.path = self._gzip_file_inplace(lf.path, dryrun)
                self.c.log.debug("after gzip, file is now %s" % lf.path)
        

    # --------------------------------------------------------------------------
    # GZIP IMPL
    # --------------------------------------------------------------------------

    # returns newfilename
    def _gzip_file_inplace(self, path, dryrun):
        self.c.log.info("gzipping '%s'" % path)
        try:
            cmd = "gzip --fast %s" % path
            if dryrun:
                self.c.log.debug("dryrun, command is: %s" % cmd)
                return path + ".gz"
                
            (ret, output) = getstatusoutput(cmd)
            if ret:
                errmsg = "problem running command: '%s' ::: return code" % cmd
                errmsg += ": %d ::: output:\n%s" % (ret, output)
                self.c.log.error(errmsg)
                raise UnexpectedError(errmsg)
            else:
                self.c.log.info("gzip'd '%s'" + path)
                newpath = path + ".gz"
                if not os.path.exists(newpath):
                    errstr = "gzip'd %s but the expected result file does not exist: '%s'" % newpath
                    self.c.log.error(errstr)
                    raise UnexpectedError(errstr)
                return newpath
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            errstr = "problem gzipping '%s': %s: %s" % \
                   (path, str(exceptname), str(sys.exc_value))
            self.c.log.error(errstr)
            raise UnexpectedError(errstr)

    # returns newfilename
    def _gunzip_file_inplace(self, path, dryrun):
        self.c.log.info("gunzipping '%s'" % path)
        try:
            cmd = "gunzip %s" % path
            if dryrun:
                self.c.log.debug("dryrun, command is: %s" % cmd)
                return path[:-3]
            
            (ret, output) = getstatusoutput(cmd)
            if ret:
                errmsg = "problem running command: '%s' ::: return code" % cmd
                errmsg += ": %d ::: output:\n%s" % (ret, output)
                self.c.log.error(errmsg)
                raise UnexpectedError(errmsg)
            else:
                self.c.log.info("ungzip'd '%s'" + path)
                newpath = path[:-3] # remove '.gz'
                if not os.path.exists(newpath):
                    errstr = "gunzip'd %s but the expected result file does not exist: '%s'" % newpath
                    self.c.log.error(errstr)
                    raise UnexpectedError(errstr)
                return newpath
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            errstr = "problem gunzipping '%s': %s: %s" % \
                   (path, str(exceptname), str(sys.exc_value))
            self.c.log.error(errstr)
            raise UnexpectedError(errstr)


    # --------------------------------------------------------------------------
    # MOUNT/COPY IMPL
    # --------------------------------------------------------------------------

    def _doMountCopyTasks(self, imagepath, vm_name, mnttask_list, dryrun):
        """execute mount+copy tasks. failures here are fatal"""

        mntpath = self.mountdir + "/" + vm_name

        # VM specific mount directory
        # This is deleted after use, if it exists now there was an
        # anomaly in the past (interpreter crash) or a very rare
        # race condition.  We just fail if it already exists, that
        # rare race condition would not be produced by our service.
        # (also, VMM should fail out, it does not allow two VMs with same name)
        if os.path.exists(mntpath):
            err = "mountpoint directory already exists, should "
            err += "not be possible unless something is severely wrong"
            self.c.log.error(err)
            # try to provide some diagnostic information
            cmd = "ls -la %s" % mntpath
            self.c.log.error("diagnostic command = '%s'" % cmd)
            ret,output = getstatusoutput(cmd)
            outputstr = "ret code: %d, output: %s" % (ret,output)
            self.c.log.error(outputstr)
            err2 = cmd + ": " + outputstr
            raise UnexpectedError(err + err2)
            
        # create it
        try:
            os.mkdir(mntpath)
            self.c.log.debug("created mountpoint directory: %s" % mntpath)
            os.chmod(mntpath, stat.S_IRUSR | stat.S_IWUSR | stat.S_IXUSR)
            self.c.log.debug("chmod'd mountpoint directory: %s" % mntpath)
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            errstr = "problem creating directory for mount+edit tasks: '%s': %s: %s" % (mountdir, str(exceptname), str(sys.exc_value))
        
            self.c.log.error(errstr)
            raise IncompatibleEnvironment(errstr)

        try:
            for task in mnttask_list:
                src = os.path.join(self.tmpdir, task[0])
                self._doOneMountCopyTask(imagepath, src, task[1], mntpath, dryrun)
        finally:
            # would only fail if someone changed permissions while
            # the tasks ran
            self._deldirs(mntpath)

    def _doOneMountCopyTask(self, imagepath, src, dst, mntpath, dryrun):

        cmd = "%s %s one %s %s %s %s" % (self.sudo_path, self.mounttool_path, imagepath, mntpath, src, dst)

        if dryrun:
            self.c.log.debug("command = '%s'" % cmd)
            self.c.log.debug("(dryrun, didn't run that)")
            return
            
        if not os.path.exists(src):
            raise IncompatibleEnvironment("source file in mount+copy task does not exist: %s" % src)

        ret,output = getstatusoutput(cmd)
        if ret:
            errmsg = "problem running command: '%s' ::: return code" % cmd
            errmsg += ": %d ::: output:\n%s" % (ret, output)
            self.c.log.error(errmsg)
            raise IncompatibleEnvironment(errmsg)
        else:
            self.c.log.debug("done mount+copy task, altered successfully: %s" % cmd)

    def _deldirs(self, mntpath):
        try:
            self.c.log.debug("removing %s" % mntpath)
            shutil.rmtree(mntpath)
            self.c.log.debug("removed %s" % mntpath)
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            errstr = "problem removing mountdir: '%s': %s: %s" % (mntpath, str(exceptname), str(sys.exc_value))
            self.c.log.error(errstr)
            raise UnexpectedError(errstr)
                