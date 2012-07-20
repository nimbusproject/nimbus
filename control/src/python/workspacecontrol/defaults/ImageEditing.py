from commands import getstatusoutput
import os
import re
import shutil
import stat
import struct
import sys
import tempfile
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
        self.fdisk_path = None
        self.qcow2_enabled = False
        self.qemu_img_path = None
        self.mountdir = None
        self.tmpdir = None
        
    def validate(self):
        self.mounttool_path = self.p.get_conf_or_none("mount", "mounttool")
        if not self.mounttool_path:
            self.c.log.warn("no mount tool configuration, mount+edit functionality is disabled")
            
        self.fdisk_path = self.p.get_conf_or_none("mount", "fdisk")
        if not self.fdisk_path:
            self.c.log.warn("no fdisk configuration, mount+edit functionality for HD images is disabled")

        qcow2 = self.p.get_conf_or_none("mount", "qcow2")
        if qcow2 and qcow2.strip().lower() == "true":
            self.qcow2_enabled = True
        if not self.qcow2_enabled:
            self.c.log.warn("mount+edit functionality for qcow2 images is disabled")
            
        self.qemu_img_path = self.p.get_conf_or_none("cow", "qemu_img")
        if not self.qemu_img_path:
            self.c.log.warn("no qemu_img configuration, copy-on-write support is disabled")
        elif not self.qcow2_enabled:
            self.c.log.warn("cannot enable copy-on-write support without qcow2 support")

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

        self.c.log.debug("mount+edit tool configured: %s" % self.mounttool_path)
        
        self.sudo_path = self.p.get_conf_or_none("sudo", "sudo")
        if not self.sudo_path:
            raise InvalidConfig("mount tool is configured but there is no sudo configuration")
            
        if not os.path.isabs(self.sudo_path):
            raise InvalidConfig("path to sudo must be absolute")
            
        if not os.access(self.sudo_path, os.F_OK):
            raise InvalidConfig("sudo is configured with an absolute path, but it does not seem to exist: '%s'" % self.sudo_path)
            
        if not os.access(self.sudo_path, os.X_OK):
            raise InvalidConfig("sudo is configured with an absolute path, but it does not seem executable: '%s'" % self.sudo_path)

        self.c.log.debug("sudo configured for image editing: %s %s" % (self.sudo_path, self.mounttool_path))

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
    
    def process_after_procurement(self, local_file_set):
        """Do any necessary work after all files are local or otherwise
        accessible but before a VM launches.
        
        local_file_set -- instance of LocalFileSet
        
        Return nothing, local_file_set will be modified as necessary.
        """

        for lf in local_file_set.flist():
            if lf.path.count(".gz") > 0 :
                lf.path = self._gunzip_file_inplace(lf.path)
        
        # copy-on-write
        if self.qemu_img_path:
            for lf in local_file_set.flist():
                lf.path = self._create_cow_file(lf.path, local_file_set.instance_dir())

        # disabled
        if not self.mounttool_path:
            return
        
        mnttask_list = self._validate_args_if_exist()
        if len(mnttask_list) > 0:
            
            vm_name = self.p.get_arg_or_none(wc_args.NAME)
            if not vm_name:
                raise InvalidInput("The %s argument is required." % wc_args.NAME.long_syntax)
            
            rootdisk = None
            hdimage = False
            for lf in local_file_set.flist():
                if lf.rootdisk:
                    rootdisk = lf.path
                    if not lf.editable:
                        raise InvalidInput("mount+edit request but the file is not marked as editable: %s" % lf.path)
                    # simplistic check for hard disk image vs. partition...
                    if lf.mountpoint and not lf.mountpoint[-1].isdigit():
                        hdimage = True
                    break
            
            if not rootdisk:
                raise InvalidInput("there is no root disk to perform the mount+edit tasks on")
            
            self._doMountCopyTasks(rootdisk, vm_name, mnttask_list, hdimage)
        
    # --------------------------------------------------------------------------
    # process_after_shutdown(), from ImageEditing interface
    # --------------------------------------------------------------------------
    
    def process_after_shutdown(self, local_file_set):
        """Do any necessary work after a VM shuts down and is being prepared
        for teardown.  Will not be called if there is an immediate-destroy
        event because that needs no unpropagation.
        
        local_file_set -- instance of LocalFileSet
        
        Return nothing, local_file_set will be modified as necessary.
        """

        for lf in local_file_set.flist():
            instance_dir = local_file_set.instance_dir()
            if instance_dir is not None:
                image_name = os.path.basename(lf.path)
                image_local_path = os.path.join(instance_dir, image_name)

                cow_name = image_name + "__cow__.qcow2"
                cow_path = os.path.join(instance_dir, cow_name)

                # If a file with a suffix of __cow__.qcow2 exists in the
                # instance directory, it means that we were using
                # copy-on-write.
                if os.path.exists(cow_path):
                    # If the base image is from a shared location (started with
                    # file:///), make a copy in the instance directory first
                    if not os.path.exists(image_local_path):
                        shutil.copy(lf.path, image_local_path)
                    else:
                        # If the base image is linked from the cache, make a
                        # copy and replace the link by it
                        filestat = os.stat(image_local_path)
                        if filestat[stat.ST_NLINK] > 1:
                            tmpfile = tempfile.mkstemp(dir=instance_dir)
                            os.close(tmpfile[0])
                            tmpfilename = tmpfile[1]
                            shutil.copy(image_local_path, tmpfilename)
                            os.unlink(image_local_path)
                            os.rename(tmpfilename, image_local_path)
                            # Add write permissions to the image
                            os.chmod(image_local_path, 0600)

                    # Commit the changes back into the backing image.
                    lf.path = self._commit_cow_file(image_local_path, cow_path)

        for lf in local_file_set.flist():
            
            # The following edit is applicable for either case, if unprop target
            # is gz or even if not.
            # This is because the local "muxing" of the file was to bring the
            # source from 'x.gz' to 'x', so on the way back to the repo, the
            # transfer or gzip commands will need to start with the non-gz form.
            if lf.path[-3:] == ".gz":
                lf.path = lf.path[:-3]
            
            try:
                lf._unpropagation_target
            except AttributeError:
                raise ProgrammingError("this image editing implementation is tied to the default procurement implementation with respect to the process_after_shutdown method. If you are running into this error, either implement the '_unpropagation_target' attribute as well, or come up with new arguments to the program for expressing compression needs in a saner way than looking for '.gz' in paths (which is tenuous)")
            
            if lf._unpropagation_target[-3:] == ".gz":
                lf.path = self._gzip_file_inplace(lf.path)
                self.c.log.debug("after gzip, file is now %s" % lf.path)
        

    # --------------------------------------------------------------------------
    # GZIP IMPL
    # --------------------------------------------------------------------------

    # returns newfilename
    def _gzip_file_inplace(self, path):
        self.c.log.info("gzipping '%s'" % path)
        try:
            cmd = "gzip --fast %s" % path
            if self.c.dryrun:
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
    def _gunzip_file_inplace(self, path):
        self.c.log.info("gunzipping '%s'" % path)
        try:
            # Since gunzip chokes when you give it a file not ending in .gz,
            # remove anything after the last .gz (query string params etc)
            gzindex = path.rfind(".gz")
            clean_path = path[:gzindex] + ".gz"
            if clean_path != path:
                try:
                    shutil.move(path, clean_path)
                    path = clean_path
                except:
                    errmsg = "problem renaming %s to %s" % (path, clean_path)
                    log.exception(errmsg)
                    raise UnexpectedError(errmsg)

            cmd = "gunzip %s" % path
            if self.c.dryrun:
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
    # COPY-ON-WRITE IMPL
    # --------------------------------------------------------------------------

    # returns newfilename
    def _create_cow_file(self, path, instance_dir):

        if instance_dir is None:
            self.c.log.warn("skipping copy-on-write volume creation because instance_dir is None")
            return path

        self.c.log.info("creating copy-on-write volume for base image '%s'" % path)

        image_name = os.path.basename(path)
        cow_name = image_name + "__cow__.qcow2"
        newpath = os.path.join(instance_dir, cow_name)

        if os.path.exists(newpath):
            errstr = "copy-on-write file already exists: '%s'" % newpath
            self.c.log.error(errstr)
            raise UnexpectedError(errstr)

        # Create the copy-on-write volume
        cmd = "%s create -f qcow2 -o backing_file=%s %s" % (self.qemu_img_path, path, newpath)
        if self.c.dryrun:
            self.c.log.debug("dryrun, command is: %s" % cmd)
            return newpath

        (ret, output) = getstatusoutput(cmd)
        if ret:
            errmsg = "problem running command: '%s' ::: return code" % cmd
            errmsg += ": %d ::: output:\n%s" % (ret, output)
            self.c.log.error(errmsg)
            raise UnexpectedError(errmsg)
        else:
            errstr = "successfully created copy-on-write volume '%s' for image '%s'" % (newpath, path)
            self.c.log.info(errstr)

        return newpath

    def _commit_cow_file(self, image_local_path, cow_path):
        self.c.log.info("committing copy-on-write changes of '%s' into '%s'" % (cow_path, image_local_path))

        try:
            cmd = "%s | grep rebase" % self.qemu_img_path
            if self.c.dryrun:
                self.c.log.debug("dryrun, command is: %s" % cmd)
            else:
                (ret, output) = getstatusoutput(cmd)
                if ret:
                    errmsg = "%s is missing the rebase command required for unpropagation of copy-on-write images. " % self.qemu_img_path
                    errmsg += "QEMU 0.13 or later is needed."
                    self.c.log.error(errmsg)
                    raise UnexpectedError(errmsg)

            cmd = "%s rebase -f qcow2 -u -b %s %s" % (self.qemu_img_path, image_local_path, cow_path)
            if self.c.dryrun:
                self.c.log.debug("dryrun, command is: %s" % cmd)
            else:
                (ret, output) = getstatusoutput(cmd)
                if ret:
                    errmsg = "problem running command: '%s' ::: return code" % cmd
                    errmsg += ": %d ::: output:\n%s" % (ret, output)
                    self.c.log.error(errmsg)
                    raise UnexpectedError(errmsg)

            self.c.log.info("rebased '%s'" % cow_path)

            cmd = "%s commit %s" % (self.qemu_img_path, cow_path)
            if self.c.dryrun:
                self.c.log.debug("dryrun, command is: %s" % cmd)
            else:
                (ret, output) = getstatusoutput(cmd)
                if ret:
                    errmsg = "problem running command: '%s' ::: return code" % cmd
                    errmsg += ": %d ::: output:\n%s" % (ret, output)
                    self.c.log.error(errmsg)
                    raise UnexpectedError(errmsg)

            self.c.log.info("committed '%s' into '%s'" % (cow_path, image_local_path))
            return image_local_path

        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__
            except AttributeError:
                exceptname = exception_type
            errstr = "problem committing '%s': %s: %s" % \
                   (cow_path, str(exceptname), str(sys.exc_value))
            self.c.log.error(errstr)
            raise UnexpectedError(errstr)


    # --------------------------------------------------------------------------
    # MOUNT/COPY IMPL
    # --------------------------------------------------------------------------

    def _doMountCopyTasks(self, imagepath, vm_name, mnttask_list, hdimage):
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
                self._doOneMountCopyTask(imagepath, src, task[1], mntpath, hdimage)
        finally:
            # would only fail if someone changed permissions while
            # the tasks ran
            self._deldirs(mntpath)

    def _doOneMountCopyTask(self, imagepath, src, dst, mntpath, hdimage):

        if not hdimage:
            cmd = "%s %s one %s %s %s %s" % (self.sudo_path, self.mounttool_path, imagepath, mntpath, src, dst)
            error = self._doOneMountCopyInnerTask(src, cmd)
            if error:
                raise error
            else:
                return

        # Some hard disk formats actually mount like partitions, for example
        # the KVM 'raw' format.  We attempt to do partition like mounting
        # first and then if that fails, try the full blown fdisk + mount
        # mechanism.
        
        cmd = "%s %s one %s %s %s %s" % (self.sudo_path, self.mounttool_path, imagepath, mntpath, src, dst)
        warning = self._doOneMountCopyInnerTask(src, cmd)
        
        if not warning:
            # success with partition-style edit
            return
            
        error = None
        try:
            f = open(imagepath, 'r')
            magic = f.read(4)

            # Version number (1 or 2) is in big endian format.
            # We only support version 2 (qcow2).
            be_version = f.read(4)
            version = struct.unpack('>I', be_version)[0]
            f.close()

            if magic[0:3] == 'QFI':
                if version == 2:
                    if self.qcow2_enabled:
                        # Mounting the partition as a qcow2 image
                        cmd = "%s %s qcowone %s %s %s %s" % (self.sudo_path, self.mounttool_path, imagepath, mntpath, src, dst)
                        error = self._doOneMountCopyInnerTask(src, cmd)
                    else:
                        raise IncompatibleEnvironment("qcow2 image detected, but qcow2 support is disabled in mount.conf")
                else:
                    raise IncompatibleEnvironment("qcow image detected with unsupported version number %d" % version)
            else:
                offsetint = self._guess_offset(imagepath)
                cmd = "%s %s hdone %s %s %s %s %d" % (self.sudo_path, self.mounttool_path, imagepath, mntpath, src, dst, offsetint)
                error = self._doOneMountCopyInnerTask(src, cmd)
        except Exception,e:
            error = e
        
        # warning is always present ('true') at this point
        
        if not error:
            # success with HD-image-style edit
            return
        
        # error AND warning are present, print both
        
        combined = """
===========================================================================
            
Tried multiple methods of mounting the image file.

First, attempted to treat it like partition (no MBR) but that did not work:

===========================================================================
%s
===========================================================================

Then, attempted to look for the first partition in the partition table using
fdisk, but that did not work either:

===========================================================================
%s
===========================================================================
""" % (warning.msg, error.msg)
        raise IncompatibleEnvironment(combined)
            
    def _doOneMountCopyInnerTask(self, src, cmd):
        if self.c.dryrun:
            self.c.log.debug("command = '%s'" % cmd)
            self.c.log.debug("(dryrun, didn't run that)")
            return None

        if not os.path.exists(src):
            return IncompatibleEnvironment("source file in mount+copy task does not exist: %s" % src)

        try:
            self.c.log.debug("mount-alter, command is: %s" % cmd)
            ret,output = getstatusoutput(cmd)
            if output:
                self.c.log.debug("mount alter rc: %d output: %s" % (ret, output))

            if ret:
                errmsg = "problem running command: '%s' ::: return code" % cmd
                errmsg += ": %d ::: output:\n%s" % (ret, output)
                return IncompatibleEnvironment(errmsg)
            else:
                self.c.log.debug("done mount+copy task, altered successfully: %s" % cmd)
        except Exception,e:
            return e
        
        return None

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
                
    def _guess_offset(self, imagepath):
        
        self.c.log.debug("guessing offset for HD image %s" % (imagepath))
        if not self.fdisk_path:
            raise InvalidConfig("image editing is being requested but that functionality has been disabled for HD images due to the lack of an fdisk program configuration")
        
        cmd = "%s -lu %s" % (self.fdisk_path, imagepath)
        
        ret,output = getstatusoutput(cmd)
        if ret:
            errmsg = "problem running command: '%s' ::: return code" % cmd
            errmsg += ": %d ::: output:\n%s" % (ret, output)
            self.c.log.error(errmsg)
            raise IncompatibleEnvironment(errmsg)
        
        # fdisk will truncate the partition name to 79 characters if it's too
        # long. Match only the first 10 characters of imagepath to detect the
        # first partition line.
        part_pattern = re.compile(r'\n%s.*' % imagepath[:10])
        lines = []
        for m in part_pattern.finditer(output):
            lines.append(m.group())
        
        if len(lines) == 0:
            raise IncompatibleEnvironment("fdisk stdout is not parseable: '%s'" % output)
            
        firstparts = lines[0].split()
        if len(firstparts) < 5:
            raise IncompatibleEnvironment("fdisk stdout is not parseable: '%s'" % output)
            
        if firstparts[1] == "*":
            sector_count = firstparts[2]
        else:
            sector_count = firstparts[1]
            
        try:
            sector_count = int(sector_count)
        except:
            raise IncompatibleEnvironment("fdisk stdout is not parseable, sector_count is not an integer ('%s'), full output: '%s'" % (sector_count, output))
            
        offset = 512 * sector_count
        
        self.c.log.debug("offset guess is %d for HD image %s" % (offset, imagepath))
        
        return offset
