#Copyright 1999-2006 University of Chicago
#
#Licensed under the Apache License, Version 2.0 (the "License"); you may not
#use this file except in compliance with the License. You may obtain a copy
#of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
#Unless required by applicable law or agreed to in writing, software
#distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#License for the specific language governing permissions and limitations
#under the License.


# TODO: this file should be 3 or 4 files

# system modules
from commands import getstatusoutput
import ConfigParser
import logging
import os
import pickle
import resource
import random
import stat
import string
import sys
import time

# our modules
import workspace
from workspace.vms.images import partition
from workspace.err import *
from workspace.util import config,control,bashEscape,removeDir
from xen_networking import *

# IPy is version 0.42 found @ http://c0re.23.nu/c0de/IPy/
# It is also licensed under a BSD style license
# including under workspace to prevent extra dependency/version conflicts
from workspace.util.IPy import IP

# Default maximum for the number of available file descriptors.
MAXFD = 1024

# The standard I/O file descriptors are redirected to /dev/null by default.
if (hasattr(os, "devnull")):
    REDIRECT_TO = os.devnull
else:
    REDIRECT_TO = "/dev/null"

log = logging.getLogger("workspace.xen_v2")

class xen_v2_manager:
    def __init__(self, parameters):
        self.opts = parameters

    ###################
    # workspaceInfo() #
    ###################
    def workspaceInfo(self):
        """For now, just return list -l output"""
        cmd = self.opts.xmpath + " list -l"
        fstdin, fstdout, fstderr = os.popen3(self.opts.xmsudo(cmd))
        fstdin.close()
        stdout = fstdout.read()
        fstdout.close()
        stderr = fstderr.read()
        fstderr.close()

        log.debug("stdout:\n" + stdout)
        if stderr != '':
            log.debug("stderr: " + stderr)

        return stdout


    ####################
    # propagateImage() #
    ####################
    def propagateImage(self, dryrun):
        self.add_errcode = 0
        self.add_output = ""
        if dryrun:
            return
        func_list = []
        arg_list = []
        if self.opts.image[:9] == "gsiftp://":
            if not self.opts.gucpath:
                raise EnvironmentProblem("propagating needs gsiftp but "
                                         "globus-url-copy is either not configured or invalid")
            func_list.append(self.guc_pull)
            arg_list.append(dryrun)
        elif self.opts.image[:6] == "scp://":
            if not self.opts.scppath:
                raise EnvironmentProblem("propagating needs scp but "
                                         "scp is either not configured or invalid")
            func_list.append(self.scp_pull)
            arg_list.append(dryrun)
        else:
            raise EnvironmentProblem("propagating without scp or gsiftp "
                                     "image specification is unsupported")
        log.debug("entering daemonized mode")
        self.actiondone = "propagate"
        func_list.append(self.notifyService)
        arg_list.append(dryrun)
        self.daemonize(func_list, arg_list)


    ######################
    # unpropagateImage() #
    ######################
    def unpropagateImage(self, dryrun):
        self.add_errcode = 0
        self.add_output = ""
        if dryrun:
            return
        func_list = []
        arg_list = []
        if self.opts.image[:9] == "gsiftp://":
            if not self.opts.gucpath:
                raise EnvironmentProblem("unpropagating needs gsiftp but "
                                         "globus-url-copy is either not configured or invalid")
            func_list.append(self.guc_push)
            arg_list.append(dryrun)

        elif self.opts.image[:6] == "scp://":
            if not self.opts.scppath:
                raise EnvironmentProblem("unpropagating needs scp but "
                                         "scp is either not configured or invalid")
            func_list.append(self.scp_push)
            arg_list.append(dryrun)
        else:
            raise EnvironmentProblem("unpropagating without scp or gsiftp "
                                     "image specification is unsupported")
        log.debug("entering daemonized mode")
        self.actiondone = "unpropagate"
        func_list.append(self.notifyService)
        arg_list.append(dryrun)
        self.daemonize(func_list, arg_list)

    # returns (True/False, newfilename/None)
    def gzip_file_inplace(self, path):
        log.info("gzipping '%s'" + path)
        try:
            cmd = "gzip --fast %s" % path
            (ret, output) = getstatusoutput(cmd)
            if ret:
                errmsg = "problem running command: '%s' ::: return code" % cmd
                errmsg += ": %d ::: output:\n%s" % (ret, output)
                self.xfer_errcode = ret
                self.xfer_output = output
                return (False, None)
            else:
                log.info("gzip'd '%s'" + path)
                newpath = path + ".gz"
                if not os.path.exists(newpath):
                    self.xfer_errcode =-1
                    errstr = "gzip'd %s but the expected result file does not exist: '%s'" % newpath
                    self.xfer_output = errstr
                    log.error(errstr)
                    return (False, None)
                return (True, newpath)
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            errstr = "problem gzipping '%s': %s: %s" % \
                   (path, str(exceptname), str(sys.exc_value))
            self.event(errstr, False)
            self.xfer_errcode =-1
            self.xfer_output = errstr
            return (False, None)

    # returns (True/False, newfilename/None)
    def gunzip_file_inplace(self, path):
        log.info("gunzipping '%s'" + path)
        try:
            cmd = "gunzip %s" % path
            (ret, output) = getstatusoutput(cmd)
            if ret:
                errmsg = "problem running command: '%s' ::: return code" % cmd
                errmsg += ": %d ::: output:\n%s" % (ret, output)
                self.xfer_errcode = ret
                self.xfer_output = output
                return (False, None)
            else:
                log.info("ungzip'd '%s'" + path)
                newpath = path[:-3] # remove '.gz'
                if not os.path.exists(newpath):
                    self.xfer_errcode =-1
                    errstr = "gunzip'd %s but the expected result file does not exist: '%s'" % newpath
                    self.xfer_output = errstr
                    log.error(errstr)
                    return (False, None)
                return (True, newpath)
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            errstr = "problem gunzipping '%s': %s: %s" % \
                   (path, str(exceptname), str(sys.exc_value))
            self.event(errstr, False)
            self.xfer_errcode =-1
            self.xfer_output = errstr
            return (False, None)

    ##############
    # guc_pull() #
    ##############
    def guc_pull(self, dryrun):
        """expects a check has already been done if self.opts.gucpath
           is configured and executable"""

        dst = 'file://' + self.opts.securelocaldir + '/' + self.opts.name
        if not os.path.exists(dst[7:]):
            os.mkdir(dst[7:])

        fnameindex = string.rfind(self.opts.image, '/')
        dst += '/' + self.opts.image[fnameindex+1:]

        log.debug("destination file is '%s'" % dst)

        # todo: perhaps optimize the transfer using options such as -tcp-bs etc
        cmd = self.opts.gucpath + " " + self.opts.image + ' ' + dst 
        log.info("running transfer command '%s'" % cmd)
        return self.pull(dryrun, cmd, dst[7:])

    ##############
    # guc_push() #
    ##############
    def guc_push(self, dryrun):
        """expects a check has already been done if self.opts.gucpath
           is configured and executable"""

        srcdir = 'file://' + self.opts.securelocaldir + '/' + self.opts.name
        fnameindex = string.rfind(self.opts.image, '/')
        src = srcdir + '/' + self.opts.image[fnameindex+1:]

        destination = self.opts.image
        if self.opts.unproptargets:
            destination = self.opts.unproptargets

        if src[-3:] == ".gz":
            src = src[:-3]

        if destination[-3:] == ".gz":
            (success, newname) = self.gzip_file_inplace(src)
            if not success:
                return self.xfer_errcode
            src = newname

        if not os.path.exists(src):
            self.xfer_errcode =-1
            self.xfer_output = "no local file to unpropagate: %s" % src
            log.error("no local file to unpropagate: %s" % src)
            return self.xfer_errcode

        log.debug("source file is '%s'" % src)
        log.debug("destination file is '%s'" % destination)

        # todo: perhaps optimize the transfer using options such as -tcp-bs etc
        cmd = self.opts.gucpath + ' ' + src + ' ' + destination
        log.info("running transfer command '%s'" % cmd)
        return self.push(dryrun, cmd)

    ##############
    # scp_pull() #
    ##############
    def scp_pull(self, dryrun):
        """expects a check has already been done if self.opts.scppath
           is configured and executable"""

        dst = self.opts.securelocaldir + '/' + self.opts.name + '/'
        if not os.path.exists(dst):
            os.mkdir(dst)

        fnameindex = string.rfind(self.opts.image, '/')
        dst += '/' + self.opts.image[fnameindex+1:]

        log.debug("destination file is '%s'" % dst)
        cmd = self.scp_cmd(dryrun, dst, self.opts.image, False)
        log.info("running transfer command '%s'" % cmd)
        return self.pull(dryrun, cmd, dst)

    ##############
    # scp_push() #
    ##############
    def scp_push(self, dryrun):
        """expects a check has already been done if self.opts.scppath
           is configured and executable"""

        srcdir = self.opts.securelocaldir + '/' + self.opts.name + '/'
        fnameindex = string.rfind(self.opts.image, '/')
        src = srcdir + '/' + self.opts.image[fnameindex+1:]

        destination = self.opts.image
        if self.opts.unproptargets:
            destination = self.opts.unproptargets

        if src[-3:] == ".gz":
            src = src[:-3]

        if destination[-3:] == ".gz":
            (success, newname) = self.gzip_file_inplace(src)
            if not success:
                return self.xfer_errcode
            src = newname

        if not os.path.exists(src):
            self.xfer_errcode =-1
            self.xfer_output = "no local file to unpropagate: %s" % src
            log.error("no local file to unpropagate: %s" % src)
            return self.xfer_errcode

        log.debug("source file is '%s'" % src)
        log.debug("destination file is '%s'" % destination)

        cmd = self.scp_cmd(dryrun, src, destination, True)
        log.info("running transfer command '%s'" % cmd)
        return self.push(dryrun, cmd)

    ### scp_cmd() - from scp_push() and scp_pull()
    def scp_cmd(self, dryrun, local, remote, push):

        # we're receiving a URL for of scp://host:port/file
        # scp instead needs host -P port remote local

        xfer_host = None
        xfer_user = None
        xfer_port = 22
        xfer_path = None

        given = remote[6:]
        colon_index = string.find(given, ':')
        if colon_index == -1:
            # no port
            path_index = string.find(given, '/')
            host = given[:path_index]
            xfer_path = given[path_index:]
        else:
            # found a port
            host = given[:colon_index]
            given = given[colon_index+1:]
            path_index = string.find(given, '/')
            if path_index == -1:
                self.xfer_errcode =-1
                self.xfer_output = "invalid image url, no path? " + remote
                log.error("invalid image url, no path? " + remote)
                return self.xfer_errcode
            port = given[:path_index]
            try:
                xfer_port = int(port)
            except:
                self.xfer_errcode =-1
                self.xfer_output = "port, but not an integer? " + remote
                log.error("port, but not an integer? " + remote)
                return self.xfer_errcode
            xfer_path = given[path_index:]

        log.debug("looking for user")
        # host var could contain user specification
        at_index = string.find(host, '@')
        if at_index == -1:
            xfer_host = host
        else:
            xfer_user = host[:at_index]
            xfer_host = host[at_index+1:]

        if xfer_user:
            log.debug("client wants override of scp account") 

            if not self.opts.scpuser_override:
                errmsg = "client specified in SCP URL %s, " % remote
                errmsg += "but scpuser_override is configured to "
                errmsg += "false, meaning the default account "
                errmsg += "is not allowed to be overriden"
                log.error(errmsg)
                self.xfer_errcode =-1
                self.xfer_output
            else:
                log.debug("scpuser_override is true, so allowing client to specify this account: %s" % xfer_user) 
        else:
            log.debug("client did not specify account") 

            # if default is not specified, we just uses current account
            if self.opts.scpuser:
                log.debug("using the default scp account") 
                xfer_user = self.opts.scpuser
            else:
                log.debug("using the program runner for scp account") 

        log.debug("SCP user %s, host %s, port %d, path %s" 
                  % (xfer_user, xfer_host, xfer_port, xfer_path))

        cmd = self.opts.scppath + " -P %d " % xfer_port

        if push:
            cmd += local + ' '

        if xfer_user:
            cmd += xfer_user + "@"

        # never make path relative to remote homedir
        if xfer_path[0] != '/':
            tail = ":/" + xfer_path
        else:
            tail = ":" + xfer_path
        cmd += xfer_host + tail

        if not push:
            cmd +=  ' ' + local

        return cmd

    ### pull() - from guc_pull() and scp_pull()
    def pull(self, dryrun, cmd, dst):
        self.xfer_errcode = 0
        self.xfer_output = ""
        if not dryrun:
            (self.xfer_errcode, self.xfer_output) = getstatusoutput(cmd)
            log.info("error code = %d" % self.xfer_errcode)
            if self.xfer_errcode:
                log.error("error output:\n%s\n" % self.xfer_output)
        else:
            log.debug("dryrun, not running transfer command")


        if self.xfer_errcode == 0 and dst[-3:] == '.gz':
            (success, newname) = self.gunzip_file_inplace(dst)
            if success:
                dst = newname

        if self.xfer_errcode == 0:
            self.opts.image = "file://" + dst
        else:
            log.error("problem running transfer, backing out IP/MAC reserves")
            remove_tracked(dryrun, self.opts)

        return self.xfer_errcode

    ### push() - from guc_push() and scp_push()
    def push(self, dryrun, cmd):
        self.xfer_errcode = 0
        self.xfer_output = ""
        if not dryrun:
            (self.xfer_errcode, self.xfer_output) = getstatusoutput(cmd)
            log.info("error code = %d" % self.xfer_errcode)
            if self.xfer_errcode:
                log.error("error output:\n%s\n" % self.xfer_output)
            else:
                self.deleteWorkspaceFiles()
        else:
            log.debug("dryrun, not running transfer command")

        if self.xfer_errcode != 0:
            log.error("problem running transfer, backing out IP/MAC reserves")
            remove_tracked(dryrun, self.opts)
        return self.xfer_errcode

    ###################
    # notifyService() #
    ###################
    def notifyService(self, dryrun):
        i1 = string.find(self.opts.notify, ':')
        userhost = self.opts.notify[:i1]
        i2 = string.find(self.opts.notify, '/')
        port = self.opts.notify[i1+1:i2]
        path = self.opts.notify[i2:]
        sshargs = '-p ' + port + ' ' + userhost

        error = ""
        if self.xfer_errcode:
            error = "TRANSFER FAILED "
            if self.xfer_output:
                # remove newlines, replace with another token
                lines = self.xfer_output.splitlines()
                a = lambda x: x + " ]eol[ "
                error += ''.join(map(a, lines))
                error = "'" + error + "'"
            else:
                error += "No error output is available"

        if self.add_errcode:
            # using += "just in case", but error should be empty at this point
            error += "CREATE FAILED "
            if self.add_output:
                # remove newlines, replace with another token
                lines = self.add_output.splitlines()
                a = lambda x: x + " ]eol[ "
                error += ''.join(map(a, lines))
                error = "'" + error + "'"
            else:
                error += "No error output is available"

        error = bashEscape(error)

        if not self.actiondone:
            log.error("self.actiondone should never be empty here")
        elif self.actiondone == "propagate":
            code = self.xfer_errcode
        elif self.actiondone == "unpropagate":
            code = self.xfer_errcode
        elif self.actiondone == "start":
            code = self.add_errcode

        exeargs = [path, 'write', self.opts.name, self.actiondone, str(code), error]

        cmd = self.opts.sshpath + " " + sshargs + " " + ' '.join(exeargs)

        log.debug("running notification command '%s'" % cmd)

        notif_errcode = 0
        if dryrun:
            log.debug("dryrun, not running notification command")
            return

        (notif_errcode, notif_output) = getstatusoutput(cmd)
        log.info("notification command error code = %d" % notif_errcode)
        if notif_errcode:
            log.error("notification command error output:\n%s\n" % notif_output)

    ###############
    # daemonize() #
    ###############
    def daemonize(self, func_list, arg_list):

        # all log entries were duplicated without closing here first
        if self.opts.logfilehandler:
            self.opts.logfilehandler.close()

        pid = os.fork()

        if not pid:
            # To become the session leader of this new session and the
            # process group leader of the new process group, we call
            # os.setsid().  The process is also guaranteed not to have 
            # a controlling terminal.
            os.setsid()

            # Fork a second child and exit immediately to prevent zombies.
            # This causes the second child process to be orphaned, making the
            # init process responsible for its cleanup.  And, since the first
            # child is a session leader without a controlling terminal, it's
            # possible for it to acquire one by opening a terminal in the
            # future (System V-based systems).  This second fork guarantees
            # that the child is no longer a session leader, preventing the
            # daemon from ever acquiring a controlling terminal.
            pid = os.fork()

            if (pid != 0):
            # exit() or _exit()?  See below.
                os._exit(0) # Exit parent (the 1st child) of the 2nd child.
        else:
            # exit() or _exit()?
            # _exit is like exit(), but it doesn't call any functions
            # registered with atexit (and on_exit) or any registered signal
            # handlers.  It also closes any open file descriptors.  Using
            # exit() may cause all stdio streams to be flushed twice and any
            # temporary files may be unexpectedly removed.  It's therefore
            # recommended that child branches of a fork() and the parent
            # branch(es) of a daemon use _exit().
            os._exit(0)     # Exit parent of the first child.


        # find max # file descriptors
        maxfd = resource.getrlimit(resource.RLIMIT_NOFILE)[1]
        if (maxfd == resource.RLIM_INFINITY):
            maxfd = MAXFD

        # Iterate through and close all file descriptors.
        for fd in range(0, maxfd):
            try:
                os.close(fd)
            except OSError: # ERROR, fd wasn't open to begin with (ignored)
                pass

        # This call to open is guaranteed to return the lowest file
        # descriptor, which will be 0 (stdin), since it was closed above.
        os.open(REDIRECT_TO, os.O_RDWR)

        # Duplicate stdin to stdout and stderr
        os.dup2(0, 1)
        os.dup2(0, 2)

        # ----------------------------------------
        # work below is done in a daemonized mode:
        # ----------------------------------------

        if self.opts.logfilepath:
            ch = logging.FileHandler(self.opts.logfilepath)
            ch.setLevel(logging.DEBUG)
            formatter = logging.Formatter("%(asctime)s - %(levelname)s - "
                                          "%(name)s (%(lineno)d) - %(message)s")
            ch.setFormatter(formatter)
            log.addHandler(ch)

        for i,func in enumerate(func_list):
            if arg_list[i] == None:
                ret = func()
            else:
                ret = func(arg_list[i])


    ##################
    # addWorkspace() #
    ##################
    def addWorkspace(self, dryrun):
        xmopts = ["create"]

        # In the future, we could make dryrun be "start paused"
        # and then destroy, which gives us a much better idea
        # of whether or not everything is OK because xm's "-n" 
        # option does not actually try and instantiate the VM.

        # Also, that may be a useful option in general, constituting
        # the difference between a "setup & allocate resources" and
        # "setup, allocate resources and GO" command -- the latter
        # is what we do now for create.
        if dryrun:
            xmopts.append("-n")

        log.debug("addWorkspace called. dryrun = %s" % dryrun)

        if self.opts.startpaused:
            xmopts.append("-p")

        xmopts.append("name=%s" % self.opts.name)
        xmopts.append("vcpus=%d" % self.opts.numcpus)
        xmopts.append("on_crash=restart")
        xmopts.append("memory=%d" % self.opts.memory)

        if self.opts.hdimage:
            xmopts.append("bootloader='%s'" % self.opts.pygrubpath)
        else:
            xmopts.append("'root=/dev/%s ro'" % self.opts.imagemount)

            kernel = None
            if self.opts.kernel[:7] == "file://":
                kernel = self.opts.kernel[7:]
            if not kernel:
                log.debug("PROBLEM: no kernel, backing out IP/MAC reservations"
                          " (if they were made)")
                remove_tracked(dryrun, self.opts)
                raise EnvironmentProblem("no disk?")
            xmopts.append("kernel='%s'" % kernel)

            if self.opts.ramdisk:
                ramdisk = self.opts.ramdisk
                if self.opts.ramdisk[:7] == "file://":
                    ramdisk = self.opts.ramdisk[7:]
                xmopts.append("ramdisk='%s'" % ramdisk)

        disk = None
        log.debug("root image is '%s'" % self.opts.image)

        if self.opts.image[:7] == "file://":
	    # this should be more cleanly handled, and handled elsewhere.
	    if self.opts.image[-3:] == '.gz':
	        self.opts.image = self.opts.image[:-3]

            if not self.opts.xenmounttype:
                disk = "file:" + self.opts.image[7:]
            else:
                disk = self.opts.xenmounttype + ":" + self.opts.image[7:]
            disk += "," + self.opts.imagemount
            disk += ",w"
            # for notification purpose
        elif self.opts.image[:9] == "gsiftp://":
            log.debug("image not local calling propagate")
            if not self.opts.gucpath:
                remove_tracked(dryrun, self.opts)
                raise EnvironmentProblem("propagating needs gsiftp but "
                                         "globus-url-copy is either not configured or invalid")
            self.propagateAndCreate(dryrun)
            return
        elif self.opts.image[:6] == "scp://":
            log.debug("image not local calling propagate")
            if not self.opts.scppath:
                remove_tracked(dryrun, self.opts)
                raise EnvironmentProblem("propagating needs scp but "
                                         "scp is either not configured or invalid")
            self.propagateAndCreate(dryrun)
            return

        if not disk:
            log.debug("PROBLEM: no disk, backing out IP/MAC reservations"
                      " (if they were made)")
            remove_tracked(dryrun, self.opts)
            raise EnvironmentProblem("no disk?")
        xmopts.append("disk='%s'" % disk)

        if len(self.opts.images) > 1:
            # TODO: this assumes a lot, come back when switch to partition
            # object is completed
            for x,partition in enumerate(self.opts.images[1:]):
                disk = "file:" + partition.path
                disk += "," + self.opts.imagemounts[x+1]

                if partition.blankspace:
                    # assumes created in instance dir (but stable assumption)
                    dst = self.opts.securelocaldir + '/' + self.opts.name + '/'
                    if not os.path.exists(dst):
                        os.mkdir(dst)

                    cmd = "%s %s %s" % (self.opts.blankcreatepath, partition.path, partition.blankspace)
                    log.debug("running '%s'" % cmd)

                    try:
                        ret,output = getstatusoutput(cmd)
                    except:
                        raise

                    if ret:
                        errmsg = "problem running command: '%s' ::: return code" % cmd
                        errmsg += ": %d ::: output:\n%s" % (ret, output)
                        remove_tracked(dryrun, self.opts)
                        raise EnvironmentProblem(errmsg)
                    else:
                        log.debug("blank partition of size %dM created at '%s'" % (partition.blankspace, partition.path))

                    disk += ",w"
                else:
                    if partition.isreadonly:
                        disk += ",r"
                    else:
                        log.error("writeable, non-propagated, non-rootdisk, non-blankspace partition?")
                        disk += ",w"

                xmopts.append("disk='%s'" % disk)

        if self.opts.networking:
            self.constructNet(xmopts, dryrun)

        if self.opts.kernelargs:
            arg = self.opts.kernelargs[0]
            for i in self.opts.kernelargs[1:]:
                arg += ' ' + i
            xmopts.append("extra='%s'" % arg)

        if len(self.opts.mnttask_list) > 0:
            self.doMountCopyTasks(dryrun)

        # (use "--skipdtd" when xm has xen-api backend to save time or if nodes
        #  have no network access) (xm w/ xen-api backend not supported without
        #  return code)

        # xm expects a file, ok if it's null
        xmopts.append("-f /dev/null")

        log.debug("options: " + str(xmopts))

        cmd = self.opts.xmpath
        for i in xmopts:
            cmd += ' ' + i

        cmd = self.opts.xmsudo(cmd)

        log.debug("cmd: " + cmd)

        fstdin, fstdout, fstderr = os.popen3(cmd)
        fstdin.close()
        stdout = fstdout.read()
        fstdout.close()
        stderr = fstderr.read()
        fstderr.close()

        log.debug("stdout: " + stdout)
        log.debug("stderr: " + stderr)

        if dryrun:
            return 0

        if stdout.find("Started domain") != -1:  #unpleasant
            if self.opts.persistencedir:
                # overwrite anything there already with the same name
                afile = self.opts.persistencedir + "/" + self.opts.name
                if os.path.exists(afile):
                    log.debug("found a file in the persistence directory "
                              "with the workspace's name, deleting it")
                    # we found that this dir is writable in validatePersistence
                    os.remove(afile)
                if persist(self.opts):
                    log.debug("persisted successfully")
                else:
                    # failing out normally here would be wrong, because
                    # the VM has been started already -- ret code 9
                    log.debug("did not persist successfully, returning "
                              "return code 9")
                    return 9
            log.info("Successful instantiation")
            return 0
        else:
            log.debug("PROBLEM: backing out IP/MAC reservations"
                      " (if they were made)")
            remove_tracked(dryrun, self.opts)
            raise RuntimeProblem(stderr)



    ### doMountCopyTasks() - from addWorkspace()
    def doMountCopyTasks(self, dryrun):
        """execute mount+copy tasks. failures here are fatal"""

        if not self.opts.sudopath:
            remove_tracked(dryrun, self.opts)
            raise EnvironmentProblem("mount + copy tasks require sudo")
        elif not self.opts.mountpath:
            remove_tracked(dryrun, self.opts)
            raise EnvironmentProblem("mount + copy tasks require mounttool")
        elif not self.opts.mountdir:
            remove_tracked(dryrun, self.opts)
            raise EnvironmentProblem("mount + copy tasks require mountdir")
        elif not self.opts.tmpdir:
            remove_tracked(dryrun, self.opts)
            raise EnvironmentProblem("mount + copy tasks require tmpdir")

        # Workspace specific mount directory
        # This is deleted after use, if it exists now there was an
        # anomaly in the past (interpreter crash) or a very rare
        # race condition.  We just fail if it already exists, that
        # rare race condition would not be produced by our service.
        # (also, Xen will fail out, it does not allow two VMs with same name)
        mntpath = self.opts.mountdir + "/" + self.opts.name

        if os.path.exists(mntpath):
            log.error("mountpoint directory already exists, should "
                      "not be possible unless something is severely wrong")
            # try to provide some diagnostic information
            cmd = "ls -la %s" % self.opts.mountdir
            log.error("diagnostic command = '%s'" % cmd)
            ret,output = getstatusoutput(cmd)
            log.error("ret code: %d, output: %s" % (ret,output))
            return False
        else:
            try:
                os.mkdir(mntpath)
                os.chmod(mntpath, stat.S_IRUSR | stat.S_IWUSR | stat.S_IXUSR)
            except:
                remove_tracked(dryrun, self.opts)
                raise

        try:
            for task in self.opts.mnttask_list:
                src = self.opts.tmpdir + "/" + task[0]
                self.doOneMountCopyTask(src, task[1], mntpath, dryrun)
        finally:
            if not self.deldirs(None, mntpath):
                remove_tracked(dryrun, self.opts)
                raise EnvironmentProblem("couldn't remove mntpath")

    ### doOneMountCopyTask() - from doMountCopyTasks()
    def doOneMountCopyTask(self, src, dst, mntpath, dryrun):

        if not os.path.exists(src):
            remove_tracked(dryrun, self.opts)
            raise EnvironmentProblem("source file in mount+copy task does not exist: %s" % src)

        cmd = "%s %s one %s %s %s %s" % (self.opts.sudopath, self.opts.mountpath, self.opts.image[7:], mntpath, src, dst)

        if dryrun:
            log.debug("command = '%s'" % cmd)
            log.debug("(dryrun, didn't run that)")
            return

        try:
            ret,output = getstatusoutput(cmd)
        except:
            raise

        if ret:
            errmsg = "problem running command: '%s' ::: return code" % cmd
            errmsg += ": %d ::: output:\n%s" % (ret, output)
            log.error(errmsg)
            remove_tracked(dryrun, self.opts)
            raise EnvironmentProblem(errmsg)
        else:
            log.debug("done mount+copy task, altered successfully: %s" % cmd)

    ### deldirs() - from doOneMountCopyTask()
    def deldirs(self, tmppath, mntpath):
        retval = True
        if tmppath:
            try:
                removeDir(tmppath, True, log.exception)
                log.debug("removed %s" % tmppath)
            except:
                log.exception("problem removing tmpdir '%s'" % tmppath)
                retval = False
        if mntpath:
            try:
                removeDir(mntpath, False, log.exception)
                log.debug("removed %s" % mntpath)
            except:
                log.exception("problem removing mountdir '%s'" % mntpath)
                retval = False
        return retval

    ### constructNetOptions() - from constructNet()
    def constructNetOptions(self, xmopts):
        """construct net options applicable to whole VM 
           (overriden in xen_v3.py)"""
        xmopts.append("nics=%d" % len(self.opts.networking))

    ### constructNet() - from addWorkspace()
    def constructNet(self, xmopts, dryrun):
        self.constructNetOptions(xmopts)

        configinfo = ""
        for nictag,nic in enumerate(self.opts.networking):
            if dryrun:
                if self.opts.mactrack:
                    log.debug("dryrun, backing out MAC reservation '%s' for "
                              "association '%s'" % (nic.mac, nic.association))
                    self.opts.mactrack.retireMAC(nic.association, nic.mac)
                if self.opts.iptrack and nic.ip:
                    log.debug("dryrun, backing out IP reservation '%s' for "
                              "association '%s'" % (nic.ip, nic.association))
                    self.opts.iptrack.retireIP(nic.association, nic.ip)

            if nic.configuration_mode in (ConfigurationMode.INDEPENDENT, ConfigurationMode.STATIC):
                if nic.mac and nic.bridge:
                    xmopts.append("vif=mac=%s,bridge=%s,vifname=%s" 
                                  % (nic.mac, nic.bridge,nic.vifname))
                else:
                # There is a bug in "xm create vif", workaround is to
                # require MAC specification.
                # we also require MAC and bridge now for DHCP support.
                    remove_tracked(dryrun, self.opts)
                    raise EnvironmentProblem("both MAC and bridge are required")

            # in the future when we have worked out the connectivity
            # representations in the wsdl, we will call on ebtables or
            # iptables for any kind of networking situation.  Here, only
            # static assignments via DHCP are getting the anti-spoofing
            # rules: those and other rules should be generalized.

            if nic.configuration_mode == ConfigurationMode.STATIC:

                if not self.opts.dhcpconfigpath:
                    remove_tracked(dryrun, self.opts)
                    raise EnvironmentProblem("static networking assignments require dhcp-config")

                if not self.opts.sudopath:
                    remove_tracked(dryrun, self.opts)
                    raise EnvironmentProblem("dhcp-config requires sudo")

                if nic.broadcast:
                    brd = nic.broadcast
                else:
                    brd = "none"

                if nic.netmask:
                    netmask = nic.netmask
                else:
                    netmask = "none"

                if nic.gateway:
                    gtwy = nic.gateway
                else:
                    gtwy = "none"

                if nic.dns:
                    dns = nic.dns
                else:
                    dns = "none"

                dns2 = "none"

                if nic.hostname:
                    hostname = nic.hostname
                else:
                    hostname = self.opts.name # ...

                cmd = "%s %s add %s %s %s %s %s %s %s %s %s %s" % (self.opts.sudopath, self.opts.dhcpconfigpath, nic.vifname, nic.ip, nic.dhcpvifname, nic.mac, brd, netmask, gtwy, nic.hostname, dns, dns2)

                if dryrun:
                    log.debug("command = '%s'" % cmd)
                    log.debug("(dryrun, didn't run that)")
                    continue

                try:
                    ret,output = getstatusoutput(cmd)
                except:
                    raise

                if ret:
                    errmsg = "problem running command: '%s' ::: return code" % cmd
                    errmsg += ": %d ::: output:\n%s" % (ret, output)
                    log.error(errmsg)
                    remove_tracked(dryrun, self.opts)
                    raise EnvironmentProblem(errmsg)
                else:
                    log.debug("altered successfully: %s" % cmd)

        if self.opts.mactrack and dryrun:
            persistMAC(self.opts)
        if self.opts.iptrack and dryrun:
            persistIP(self.opts)

    ########################
    # propagateAndCreate() #
    ########################
    def propagateAndCreate(self, dryrun):
        func_list = []   
        arg_list = []
        func_list.append(self.propagateAndCreateDaemonized)
        arg_list.append(dryrun)
        log.debug("entering daemonized mode")
        self.daemonize(func_list, arg_list)

    ##################################
    # propagateAndCreateDaemonized() #
    ##################################
    def propagateAndCreateDaemonized(self, dryrun):
        """Wrapper to encapsulate error logic while daemonized"""

        self.add_errcode = 0
        self.add_output = ""

        self.actiondone = "start"

        if self.opts.image[:9] == "gsiftp://":
            ret = self.guc_pull(dryrun)
        elif self.opts.image[:6] == "scp://":
            ret = self.scp_pull(dryrun)

        if ret != 0:
            self.actiondone = "propagate" # must report as a propagation error
            remove_tracked(dryrun, self.opts)
        else:
            try:
                self.add_errcode = self.addWorkspace(dryrun)
            except:
                self.add_errcode = 1
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                self.add_output = "%s: %s\n" % (str(exceptname), str(sys.exc_value))

        self.notifyService(dryrun)


    #####################
    # removeWorkspace() #
    #####################
    def removeWorkspace(self, dryrun):
        createopts = None

        if not self.opts.persistencedir:
            log.debug("no persistence, going to destroy %s" % self.opts.name)
        else:
            path = self.opts.persistencedir + "/" + self.opts.name
            createopts = depersist(path)
            if not createopts:
                log.debug("could not depersist from file '%s' for workspace"
                          "'%s', this probably means --remove was already "
                          " called or --create was never called"
                          " for '%s'" % (path, self.opts.name, self.opts.name))
            else:
                if createopts.name != self.opts.name:
                    msg = "inconsistency? de-persisted based on '%s', but pickled object has name = '%s'" % (self.opts.name, createopts.name)
                    log.debug(msg)
                    raise RuntimeProblem(msg)
                log.debug("create options name: %s" % createopts.name)
                log.debug("create options image: %s" % createopts.image)
                if createopts.networking:
                    log.debug("create options net: %d nics" % len(createopts.networking))
                # TODO: allow for registering functions to invoke at teardown

        if dryrun:
            log.debug("dryrun, so not destroying '%s'" % self.opts.name)
            return

        destroyproblem = None
        notpresent = False
        try:
            notpresent = self.killWorkspace(dryrun)
        except RuntimeProblem, e:
            destroyproblem = e

        if self.opts.persistencedir:
            log.debug("removing persisted object for this VM: %s" % path)
            try:
                if not validpath(path):
                    if not self.opts.deleteall:
                        raise Exception("path is invalid: %s" % path)
                os.remove(path)
            except:
                log.exception("problem removing persisted object:")

        if createopts:
            log.debug("removing tracked MACs/IPs:")
            remove_tracked(dryrun, createopts)

        if self.opts.deleteall:
            self.deleteWorkspaceFiles()

        if destroyproblem:
            raise destroyproblem
        elif self.opts.persistencedir and not createopts and notpresent:
            log.debug("persistence configured, workspace object not in "
                      "persistence dir, VM not present -- probably means "
                      "remove already called or create never called: "
                      " returning special retcode 18")
            return 18
        elif notpresent:
            log.debug("VM not present, workspace object was in persistence "
                      "dir, so likely it shut itself down: returning special "
                      "retcode 27")
            return 27
        else:
            return 0

    ### killWorkspace() - from removeWorkspace() and push()
    def deleteWorkspaceFiles(self):
        if self.opts.mountdir:
            mntpath = self.opts.mountdir + "/" + self.opts.name
            if os.path.exists(mntpath):
                log.error("mountpoint directory for workspace exists, "
                          "not possible unless something is severely wrong "
                          "OR this remove+deleteall command is in a race with "
                          "creating the workspace -- rm -rf'ing it")
                try:
                    removeDir(mntpath, True, log.exception)
                    log.debug("removed %s" % mntpath)
                except:
                    log.exception("problem removing mntpath '%s'" % mntpath)
            else:
                log.debug("no mnt dir (expected in all but rarest circumstances)")

        if self.opts.tmpdir:
            tmppath = self.opts.tmpdir + "/" + self.opts.name
            if os.path.exists(tmppath):
                log.debug("removing workspace's secure tmp dir '%s'" % tmppath)
                try:
                    removeDir(tmppath, True, log.exception)
                    log.debug("removed %s" % tmppath)
                except:
                    log.exception("problem removing tmppath '%s'" % tmppath)
            else:
                log.debug("no tmp dir '%s'" % tmppath)

        secdir = self.opts.securelocaldir
        secpath = self.opts.securelocaldir + "/" + self.opts.name
        if (secpath):
            if os.path.exists(secpath):
                log.debug("removing workspace's secure img dir: %s" % secpath)
                try:
                    removeDir(secpath, True, log.exception)
                    log.debug("removed %s" % secpath)
                except:
                    log.exception("problem removing propagated image dir (secureimages dir) '%s'" % secpath)
            else:
                log.debug("no secure img dir: %s" % secpath)

    ### killWorkspace() - from removeWorkspace()
    def killWorkspace(self, dryrun):

        # destroy or try graceful shutdown?
        notpresent = False
        if not self.opts.checkshutdown or self.opts.deleteall:
            cmd = self.opts.xmpath + ' destroy ' + self.opts.name
            cmd = self.opts.xmsudo(cmd)
            ret, output = getstatusoutput(cmd)
            if ret:
                msg = "problem running: '%s' ::: returns %d ::: output: %s" % (cmd,ret,output)
                log.error(msg)
                if not self.opts.deleteall:
                    raise RuntimeProblem(msg)
            else:
                log.debug("destroyed successfully: %s" % cmd)
        else:
            # first try to gracefully shutdown
            # the -w flag means process will hang until shutdown
            cmd = "%s shutdown -H -w %s" % (self.opts.xmpath, self.opts.name) 
            cmd = self.opts.xmsudo(cmd)

            pid = os.fork()
            if not pid:
                (errcode, output) = getstatusoutput(cmd)
                if errcode == 256:
                    errcode = 1
                log.info("(shutdown process) error code = %d" % errcode)
                if errcode:
                    log.error("(shutdown process) error output:\n%s\n" % output)
                os._exit(errcode)

            # Check every $interval seconds if it's shut down yet.
            # If checkshutdownpause seconds pass by and no return
            # from the child, destroy the domain
            done = False
            kill = True
            interval = 0.5
            count = self.opts.checkshutdownpause
            while count > 0 and kill:
                time.sleep(interval)
                log.debug("checking on child")
                x = os.waitpid(pid, os.WNOHANG)
                if x != (0,0):
                    if x[1] != 0:
                        msg = "child exited with error: %d" % x[1]
                        log.debug(msg)
                        notpresent = True
                        log.debug("VM not present")
                    else:
                        log.debug("child exited normally")
                    kill = False
                count -= interval

            if kill:
                cmd = self.opts.xmpath + ' destroy ' + self.opts.name
                cmd = self.opts.xmsudo(cmd)
                ret, output = getstatusoutput(cmd)
                if ret:
                    msg = "problem running: '%s' ::: returns %d ::: output: %s" % (cmd,ret,output)
                    log.error(msg)
                    raise RuntimeProblem(msg)
                else:
                    log.debug("destroyed successfully: %s" % cmd)

        return notpresent

    #####################
    # rebootWorkspace() #
    #####################
    def rebootWorkspace(self, dryrun):
        if dryrun:
            log.debug("dryrun, so not rebooting '%s'" % self.opts.name)
            return

        cmd = self.opts.xmpath + " reboot %s" % self.opts.name

        cmd = self.opts.xmsudo(cmd)

        log.debug("cmd: " + cmd)

        fstdin, fstdout, fstderr = os.popen3(cmd)
        fstdin.close()
        stdout = fstdout.read()
        fstdout.close()
        stderr = fstderr.read()
        fstderr.close()

        log.debug("stdout: " + stdout)
        log.debug("stderr: " + stderr)

        if stderr.find("invalid domain") != -1:  #unpleasant
            return 3
        else:
            return 0

    ####################
    # pauseWorkspace() #
    ####################
    def pauseWorkspace(self, dryrun):
        if dryrun:
            log.debug("dryrun, so not pausing '%s'" % self.opts.name)
            return

        cmd = self.opts.xmpath + " pause %s" % self.opts.name

        cmd = self.opts.xmsudo(cmd)

        log.debug("cmd: " + cmd)

        fstdin, fstdout, fstderr = os.popen3(cmd)
        fstdin.close()
        stdout = fstdout.read()
        fstdout.close()
        stderr = fstderr.read()
        fstderr.close()

        log.debug("stdout: " + stdout)
        log.debug("stderr: " + stderr)

        if stderr.find("invalid domain") != -1:  #unpleasant
            return 3
        else:
            return 0

    ######################
    # unpauseWorkspace() #
    ######################
    def unpauseWorkspace(self, dryrun):
        if dryrun:
            log.debug("dryrun, so not unpausing '%s'" % self.opts.name)
            return

        xmopts = ["unpause"]
        xmopts.append("%s" % self.opts.name)

        cmd = self.opts.xmpath
        for i in xmopts:
            cmd += ' ' + i

        cmd = self.opts.xmsudo(cmd)

        log.debug("cmd: " + cmd)

        fstdin, fstdout, fstderr = os.popen3(cmd)
        fstdin.close()
        stdout = fstdout.read()
        fstdout.close()
        stderr = fstderr.read()
        fstderr.close()

        log.debug("stdout: " + stdout)
        log.debug("stderr: " + stderr)

        if stderr.find("invalid domain") != -1:  #unpleasant
            return 3
        else:
            return 0

# todo: break out functionality, class too big
class xen_v2_manager_config(control.parameters):
    def __init__(self, conffile, action):
        self.conffile = conffile
        self.action = action

        # cmdline options
        self.name = None
        self.images = None
        self.imagemounts = None
        self.kernel = None
        self.kernelargs = None
        self.ramdisk = None
        self.persistencedir = None
        self.memory = None
        self.numcpus = 1
        self.checkshutdown = None
        self.checkshutdownpause = None
        self.networking = None
        self.notify = None
        self.logfilepath = None
        self.logfilehandler = None
        self.startpaused = False
        self.deleteall = False
        self.mnttasks = None
        self.unproptargets = None

        # other
        self.localdir = None
        self.securelocaldir = None
        self.nics = 1
        self.bridgemap = None
        self.dhcpvifmap = None
        self.valid_ips_dict = None
        self.iptrack = None
        self.mactrack = None
        self.actiondone = None
        self.errfile = None
        self.sshpath = None
        self.gucpath = None
        self.scppath = None
        self.scpuser = None
        self.sudopath = None
        self.usexmsudo = True
        self.mountdir = None
        self.mountpath = None
        self.tmpdir = None
        self.dhcpconfigpath = None
        self.blankcreatepath = None
        self.pygrubpath = None
        self.hdimage = False
        self.trymatchingramdisk = False

        # list of tuples (uuid, targetpath)
        self.mnttask_list = []


    ###################
    # cmdline options #
    ###################

    def set_name(self, name):
        log.debug("name: %s" % name)
        self.name = name

    def set_images(self, img):
        log.debug("images: %s" % img)
        self.images = img

    def set_imagemounts(self, img):
        log.debug("imagemounts: %s" % img)
        self.imagemounts = img

    def set_kernel(self, kernel):
        log.debug("kernel: %s" % kernel)
        self.kernel = kernel

    def set_kernelarguments(self, args):
        log.debug("kernel arguments: %s" % args)
        self.kernelargs = args

    def set_ramdisk(self, ramdisk):
        log.debug("ramdisk(initrd): %s" % ramdisk)
        self.ramdisk = ramdisk 

    def set_persistencedir(self, persistencedir):
        log.debug("persistencedir: %s" % persistencedir)
        self.persistencedir = persistencedir

    def set_networking(self, networking):
        log.debug("networking: %s" % networking)
        self.networking = networking

    def set_memory(self, memory):
        log.debug("memory: %s" % memory)
        self.memory = memory

    def set_checkshutdown(self, shut):
        log.debug("checkshutdown: %s" % shut)
        self.checkshutdown = shut

    def set_checkshutdownpause(self, shutpause):
        log.debug("checkshutdownpause: %s" % shutpause)
        self.checkshutdownpause = shutpause

    def set_notify(self, notify):
        log.debug("notify: %s" % notify)
        self.notify = notify

    def set_logfile(self, logfilepath, logfilehandler):
        log.debug("logfile path: %s" % logfilepath)
        self.logfilepath = logfilepath
        self.logfilehandler = logfilehandler

    def set_startpaused(self, sp):
        log.debug("startpaused: %s" % sp)
        self.startpaused = sp

    def set_deleteall(self, delete):
        log.debug("deleteall: %s" % delete)
        self.deleteall = delete

    def set_mnttasks(self, mnttasks):
        log.debug("mnttasks: %s" % mnttasks)
        self.mnttasks = mnttasks

    def set_unproptargets(self, unproptargets):
        log.debug("unproptargets: %s" % unproptargets)
        self.unproptargets = unproptargets

    # ---------------------------------------------------------------------- #

    def xensudo(self, cmd):
        return cmd

    def xmsudo(self, cmd):
        if self.usexmsudo:
            return self.sudopath + " " + cmd
        else:
            return cmd

    # TODO: lock on persistencedir

    ##############
    # validate() #
    ##############
    def validate(self):
        log.debug("beginning validation")

        self.okstr = "\nValid:\n------\n"
        self.errorstr = "\nInvalid:\n--------\n"
        self.tripexception = False

        # the validation routines are grouped:
        self.validateAll()
        if self.action in (workspace.CREATE):
            self.validateCreate()
        elif self.action in (workspace.REMOVE):
            self.validateRemove()
        elif self.action in (workspace.REBOOT):
            self.validateReboot()
        elif self.action in (workspace.PAUSE):
            self.validatePause()
        elif self.action in (workspace.UNPAUSE):
            self.validateUnpause()
        elif self.action in (workspace.PROPAGATE):
            self.validatePropagate()
        elif self.action in (workspace.UNPROPAGATE):
            self.validateUnpropagate()

        # Done.
        if self.tripexception:
            raise EnvironmentProblem(self.okstr + self.errorstr)
        else:
            if self.errorstr == "\nInvalid:\n--------\n":
                return self.okstr
            else:
                return self.okstr + self.errorstr

    # ---------------------------------------------------------------------- #

    # NOTE: order of validation routines can be important

    def validateAll(self):
        self.validateName()
        self.validateXen()
        self.validateDHCP()

    def validateCreate(self):
        self.validatePersistence()
        self.validateNameCreate()
        self.validatePropagatePaths()
        self.validateImages()
        self.validateMountpoints()
        self.validateKernel()
        self.validateInitrd()
        self.validateMountDir()
        self.validateTempDir()
        self.validateMountTasks()
        self.validateMemory()
        self.validateNumcpu()
        self.validateNotify()
        self.validateNetworking()

    def validateRemove(self):
        self.validateMountDir()
        self.validateTempDir()
        self.validatePropagatePaths()

        # OK if it was true, we needed the paths, do what is possible
        self.tripexception = False

        self.validatePersistence()
        self.validateNameRemove()
        self.loadShutdownBehavior()

    def validateReboot(self):
        self.event("no reboot validation")

    def validatePause(self):
        self.event("no pause validation")

    def validateUnpause(self):
        self.event("no unpause validation")

    def validatePropagate(self):
        self.validatePropagatePaths()
        self.validateImages()
        self.validateNotify()

    def validateUnpropagate(self):
        self.validatePropagatePaths()
        self.validateImages()
        self.validateNotify()
        self.validateUnpropTargets()

    # ---------------------------------------------------------------------- #
    # for validateAll()                                                      #
    # ---------------------------------------------------------------------- #

    ##################
    # validateName() #
    ##################
    def validateName(self):
        if not self.name:
            self.event("fatal, no workspace name", False)
            self.tripexception = True
            return

        if '/' in self.name:
            self.event("fatal, workspace name '%s' contains '/'"
                       % self.name, False)
            self.tripexception = True
            return

        if '..' in self.name:
            self.event("fatal, workspace name '%s' contains '..'"
                       % self.name, False)
            self.tripexception = True
            return


    #################
    # validateXen() #
    #################
    def validateXen(self):
        # no point enumerating errors if there is a problem reading
        # conf file in the first place, just throws exception
        self.config = ConfigParser.ConfigParser()
        self.config.read(self.conffile)

        self.event("conffile parsed: %s" % self.conffile)

        # Family?
        try:
            self.family = self.config.get("general","family")
            self.event("found family: %s" % self.family)
        except:
            self.family = None
            self.event("no value given for conf family")

        # pygrub?
        try:
            self.pygrubpath = self.config.get("xenpaths","pygrub")
            self.event("found pygrub: %s" % self.pygrubpath)
        except:
            # hd image support disabled
            self.pygrubpath = None
            self.event("HD image support disabled: no value given for pygrub path")

        # sudo
        try:
            self.usexmsudo = self.getValue("systempaths","xmsudo")
            self.event("found xmsudo conf: %s" % self.usexmsudo)
            if self.usexmsudo and self.usexmsudo.lower() == 'no':
                self.usexmsudo = False
            else:
                self.usexmsudo = True
        except:
            self.usexmsudo = True
            self.event("no config for xmsudo, assuming sudo is used for xm")

        try:
            self.sudopath = self.getValue("systempaths","sudo")
            self.event("found sudo conf: %s" % self.sudopath)
            if not os.path.exists(self.sudopath):
                self.event("sudo conf '%s' does not exist on the "
                           "filesystem" % self.sudopath, False)
                self.tripexception = True
        except:
            self.sudopath = None
            self.event("no config for sudo")

        if self.sudopath:
            # change to isabs
            if self.sudopath[0] != '/':
                self.event("sudo path must be absolute path", False)
                self.sudopath = None
                self.tripexception = True

        # xm path
        try:
            self.xmpath = self.getValue("xenpaths","xm")
            self.event("found xm conf: %s" % self.xmpath)
            if not os.path.exists(self.xmpath):
                self.event("xm conf '%s' does not exist on the "
                           "filesystem" % self.xmpath, False)
                self.tripexception = True
        except:
            self.xmpath = None
            self.event("no value given for xm path", False)
            self.tripexception = True

        if self.xmpath:
            if self.xmpath[0] != '/':
                self.event("xm path must be absolute path", False)
                self.xmpath = None
                self.tripexception = True

        # xend path
        try:
            self.xendpath = self.getValue("xenpaths","xend")
            self.event("found xend conf: %s" % self.xendpath)
            if not os.path.exists(self.xendpath):
                self.event("xend conf '%s' does not exist on the "
                           "filesystem" % self.xendpath, False)
                self.tripexception = True
        except:
            self.xendpath = None
            self.event("no value given for xend path", False)
            self.tripexception = True      

        if self.xendpath:
            if self.xendpath[0] != '/':
                self.event("xend path must be absolute path", False)
                self.xendpath = None
                self.tripexception = True

        # executable tests
        #exes = [self.xmpath, self.xendpath]
        #try:
        #    for i in exes:
        #        x = os.access(i, os.X_OK)
        #        if x:
        #            self.event("'%s' is executable" % i)
        #        else:
        #            self.event("'%s' is NOT executable" % i, False)
        #            self.tripexception = True
        #except:
        #    exception_type = sys.exc_type
        #    try:
        #        exceptname = exception_type.__name__ 
        #    except AttributeError:
        #        exceptname = exception_type
        #    self.event("Unkown problem testing executable: %s: %s\n" % 
        #    (str(exceptname), str(sys.exc_value)), False)
        #    self.tripexception = True

        self.xmstdout = None

        try:
            self.assume_xend = self.getValue("behavior","assume_xend")
        except NoValue:
            self.assume_xend = True
            return
        else:
            if string.lower(self.assume_xend) == "false":
                self.assume_xend = False
            else:
                return

        # xend test / possible restart
        try:
            cmd = "%s list" % self.xmpath
            cmd = self.xmsudo(cmd)
            ret,self.xmstdout = getstatusoutput(cmd)

            if ret:
                # messages about sudo really only belong in xen_v3
                self.event("cannot contact xend", False)
                if self.xmstdout:
                    self.event("output: %s" % self.xmstdout, False)
                    if "root access" in self.xmstdout:
                        self.event("sudo seems to be misconfigured", False)
                        self.tripexception = True
                        return
                    if "trust you have received the usual" in self.xmstdout:
                        self.event("sudo seems to be misconfigured", False)
                        self.tripexception = True
                        return
                try:
                    restart_xend = self.getValue("behavior","restart_xend")
                except NoValue,err:
                    self.event("fatal: cannot contact xend, restart_xend not "
                               "configured, but default is false, not "
                               "restarting",False)
                    self.tripexception = True
                else:
                    if string.lower(restart_xend) == "true":
                        cmd = "%s restart" % self.xendpath
                        ret, outstr = getstatusoutput(self.xensudo(cmd))
                        if ret:
                            self.event("fatal: attempted to restart xend "
                                       "and failed, output: %s" % outstr,False)
                            self.tripexception = True
                        else:
                            self.event("restarted xend")
                            time.sleep(self.xendRestartTime())
                            cmd = "%s list" % self.xmpath
                            fstdin,fstdout,fstderr = \
                                  os.popen3(self.xmsudo(cmd))

                            fstdin.close()
                            self.xmstdout = fstdout.read()
                            fstdout.close()
                            stderr = fstderr.read()
                            fstderr.close()

                            if self.xmstdout == '':
                                self.event("fatal: restarted xend, but xm "
                                           "list fails", False)
                                if stderr:
                                    self.event("stderr: %s" % stderr, False)
                                self.tripexception = True
                            else:
                                self.event("restarted xend, running now")
                                self.event("xm list output:\n%s"
                                           % self.xmstdout)
                    else:
                        self.event("fatal: cannot contact xend, restart xend "
                                   "configured, but not to 'true', not "
                                   "restarting", False)
                        self.tripexception = True
            else:
                self.event("xend is running")
                self.event("xm list output:\n%s" % self.xmstdout)
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            self.event("unknown problem testing xm/xend: %s: %s\n" 
                       % (str(exceptname), str(sys.exc_value)), False)
            self.tripexception = True


    ##################
    # validateDHCP() #
    ##################
    def validateDHCP(self):
        ok = True
        try:
            self.dhcpconfigpath = self.getValue("systempaths","dhcpconfig")
            self.event("found dhcpconfig conf: %s" % self.sshpath)
        except NoValue:
            self.event("no configuration for dhcp-config", False)
            ok = False

        if ok and not os.path.exists(self.dhcpconfigpath):
            self.event("dhcpconfig '%s' does not exist on the "
                       "filesystem" % self.dhcpconfigpath, False)
            ok = False

        if ok and not os.path.isabs(self.dhcpconfigpath):
            self.event("dhcpconfig must be absolute path", False)
            ok = False

        if not ok:
            self.dhcpconfigpath = None
            self.tripexception = True
            return

    #########################
    # validatePersistence() #
    #########################
    def validatePersistence(self):
        if not self.persistencedir:
            log.debug("no given persistence dir, checking configuration file")
            try:
                self.persistencedir = \
                    self.getValue("persistence","persistencedir")
            except NoValue:
                self.persistencedir = None
                self.event("no configuration for persistence directory, "
                           "no persistence")

        if not self.persistencedir:
            return

        if self.persistencedir[0] != '/':
            self.event("persistencedir must be absolute path", False)
            self.persistencedir = None
            self.tripexception = True
            return

        if not os.path.exists(self.persistencedir):
            self.event("persistence is configured, but '%s' does not"
                       " exist on the filesystem, attemping to create "
                       " it" % self.persistencedir, False)
            try:
                os.mkdir(self.persistencedir)
                os.chmod(self.persistencedir, stat.S_IRUSR | stat.S_IWUSR | stat.S_IXUSR)
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                self.event("Problem creating persistencedir: %s: %s\n" 
                           % (str(exceptname), str(sys.exc_value)), False)
                self.tripexception = True
                return
            self.event("(created persistence directory '%s')"
                       % self.persistencedir,False)
            self.event("created persistence directory '%s'" 
                       % self.persistencedir)

        x = os.access(self.persistencedir, os.W_OK | os.X_OK | os.R_OK)
        if x:
            self.event("'%s' exists on the filesystem and is "
                       "rwx-able" % self.persistencedir)
        else:
            self.event("fatal, persistence is configured, '%s' exists on "
                       "the filesystem but is not rwx" 
                       % self.persistencedir, False)
            self.tripexception = True
            return

    # end: for validateAll()


    # ---------------------------------------------------------------------- #
    # for validateCreate()                                                   #
    # ---------------------------------------------------------------------- #

    ####################
    # validateKernel() #
    ####################
    def validateKernel(self):
        """kernel and ramdisks (initrds)"""

        if self.hdimage:
            self.event("hdimage, kernel not needed")
            if self.kernel:
                self.event("hdimage but kernel specified?", False)
                self.tripexception = True
            return


        try:
            paths = self.getValue("images","guestkernels")
            self.guestkernelpaths = paths.split(",")
            newlist = []
            for i in self.guestkernelpaths:
                i = string.strip(i)
                if i[0] == "/":
                    newlist.append("file://"+i)
                elif i[:6] == "nfs://":
                    newlist.append(i)
                elif i[:7] == "file://":
                    newlist.append(i)
                else:
                    self.event("guest kernel in list has unsupported scheme, "
                               "not absolute path or in ('file://', "
                               "'nfs://'), throwing entry out")
            self.guestkernelpaths = newlist
            self.event("found %d guest kernel(s) configuration(s): %s" % 
                       (len(self.guestkernelpaths), self.guestkernelpaths))

        except:
            self.event("no value given for guest kernels")

        try:
            allowoverride = \
                          self.getValue("behavior","allow_guestkernel_override")
            if string.lower(allowoverride) == "true":
                allowoverride = True
            else:
                allowoverride = False
        except:
            self.event("no value given for allow_guestkernel_override, "
                       "assuming false")
            allowoverride = False

        if allowoverride:
            self.event("not using kernel authz list")
        else:
            self.event("using kernel authz list")

        isfile = False

        if not self.kernel:
            self.event("no kernel specified, attempting to use default")
            if len(self.guestkernelpaths) > 0:
                self.kernel = self.guestkernelpaths[0]
                self.event("default: '%s'" % self.guestkernelpaths[0])
                isfile = True
            else:
                self.event("fatal, no default kernel", False)
                self.tripexception = True
        else:
            # assumes Unix-like paths for now 
            # (even though Python has ways of abstracting file separators)
            relative = False
            if self.kernel[0] == '/':
                self.event("absolute path to kernel specified: %s" 
                           % self.kernel)
                self.kernel = "file://" + self.kernel
            elif self.kernel[:8] == 'file:///':
                self.event("absolute path to kernel specified w/ "
                           "file:// -->  %s" % self.kernel)
            elif self.kernel[:6] == 'nfs://':
                # For kernels, this means mount an nfs mountpoint and then get
                # the file. syntax? TBD. 
                # This option is not supported for kernels yet
                self.event("fatal, kernel specified w/ nfs:// -->  "
                           "%s" % self.kernel, False)
                self.tripexception = True
                self.kernel = None
            elif self.kernel[:7] == 'file://':
                self.event("kernel is relative path w/ file://")
                self.kernel = self.kernel[7:]
                relative = True
            else:
                relative = True

            if relative or self.kernel[:7] == 'file://':
                isfile = True
                if not self.checkSecurePath(self.kernel):
                    self.kernel = None

            if relative:
                # assuming relative path to local image repository for kernel
                if not self.localdir:
                    try:
                        self.localdir = self.getValue("images","localdir")
                    except:
                        self.event("no value given for local image directory, "
                                   "but kernel specified is relative "
                                   "path", False)
                        self.tripexception = True
                        self.kernel = None
                if self.localdir:
                    if self.localdir[-1] != '/':
                        self.kernel = 'file://' + self.localdir + \
                            '/' + self.kernel
                    else:
                        self.kernel = 'file://' + self.localdir + self.kernel

            self.event("kernel specified: %s" % self.kernel)
            if not allowoverride:
                if self.kernel not in self.guestkernelpaths:
                    self.event("fatal: specified kernel '%s' is not "
                               "authorized" % self.kernel, False)
                    self.tripexception = True
                    self.kernel = None

        # if there was a problem already, kernel is set to None
        if not self.kernel:
            return

        # at this point, assuming kernel string is stripped
        self.handleImage(self.kernel, isfile)

        try:
            match_kernel_suffix = self.getValue("images","matchramdisk")
            if match_kernel_suffix:
                self.event("asked to look for initrds with suffix '%s'" % match_kernel_suffix)
        except:
            self.event("no value given for matchramdisk")
            match_kernel_suffix = None

        if match_kernel_suffix and self.ramdisk:
            self.event("ramdisk supplied by caller, it overrides any 'matched'"
                       " initrd searching")
        elif match_kernel_suffix:
            self.trymatchingramdisk = True
            self.ramdisk = self.kernel + match_kernel_suffix
            # validateInitrd() takes over from here

    ####################
    # validateInitrd() #
    ####################
    def validateInitrd(self):
        if not self.ramdisk:
            self.event("no ramdisk configured")
            return

        if self.hdimage:
            self.event("hdimage but ramdisk specified?", False)
            self.tripexception = True
            return

        if self.trymatchingramdisk:
            self.event("ramdisk will be sought via matching")

        relative = False
        if self.ramdisk[0] == "/":
            self.ramdisk = "file://" + self.ramdisk
            self.event("ramdisk setting is absolute path")
        elif self.ramdisk[:6] == "nfs://":
            self.event("ramdisk setting is nfs")
            self.event("fatal, ramdisk specified w/ "
                       "nfs:// -->  %s" % self.ramdisk, False)
            self.tripexception = True
        elif self.ramdisk[:8] == "file:///":
            self.event("ramdisk setting is absolute path w/ file://")
        elif self.ramdisk[:7] == "file://":
            self.event("ramdisk setting is relative path w/ file://")
            self.ramdisk = self.ramdisk[7:]
            relative = True
        else:
            relative = True

        isfile = False
        if relative or self.kernel[:7] == 'file://':
            self.checkSecurePath(self.ramdisk)
            isfile = True

        if relative:
            # if none of the above, assuming relative path to 
            # local image repository for initrd
            if not self.localdir:
                try:
                    self.localdir = self.getValue("images","localdir")
                except NoValue:
                    self.event("fatal, no value given for local image "
                               "directory, but image specified is relative "
                               "path", False)
                    self.tripexception = True
            if self.localdir:
                if self.localdir[-1] != '/':
                    self.ramdisk = 'file://' + self.localdir + \
                        '/' + self.ramdisk
                else:
                    self.ramdisk = 'file://' + self.localdir + self.ramdisk

        self.handleImage(self.ramdisk, isfile, self.trymatchingramdisk)

    ####################
    # validateNotify() #
    ####################
    # self.notify should take the form username@hostname:port/path (where 
    # hostname is a fqdn)
    def validateNotify(self):
        if not self.images:
            self.event("fatal, no images configured",False)
            self.tripexception = True
            return

        if self.image[:9] == "gsiftp://" or self.image[:6] == "scp://":
            if not self.notify:
                self.event("fatal, no user/host configured for notify", False)
                self.tripexception = True
                return

            # if string.find(self.notify, '@') == -1:
            #     self.event("fatal, no user configured for notify", False)
            #     self.tripexception = True

            if string.find(self.notify, ':') == -1:
                self.event("fatal, no port configured for notify", False)
                self.tripexception = True
            if string.find(self.notify, '/') == -1:
                self.event("fatal, no path configured for notify", False)
                self.tripexception = True

            # ssh path
            try:
                self.sshpath = self.getValue("systempaths","ssh")
                self.event("found ssh conf: %s" % self.sshpath)
                if not os.path.exists(self.sshpath):
                    self.event("ssh conf '%s' does not exist on the "
                               "filesystem" % self.sshpath, False)
                    self.tripexception = True
            except:
                self.sshpath = None
                self.event("no value given for ssh path", False)
                self.tripexception = True
                return

            if self.sshpath:
                if self.sshpath[0] != '/':
                    self.event("sshpath must be absolute path", False)
                    self.sshpath = None
                    self.tripexception = True
                    return

            exes = [self.sshpath]
            try:
                # will even work in a setuid environment
                for i in exes:
                    x = os.access(i, os.X_OK)
                    if x:
                        self.event("'%s' is executable" % i)
                    else:
                        self.event("'%s' is NOT executable" % i, False)
                        self.tripexception = True
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                self.event("Unkown problem testing executable: %s: %s\n" % 
                           (str(exceptname), str(sys.exc_value)), False)
                self.tripexception = True

    ##########################
    # validatePropagateDir() #
    ##########################
    def validatePropagatePaths(self):
        if not self.securelocaldir:
            try:
                self.securelocaldir = self.getValue("images","securelocaldir")
            except NoValue:
                self.event("fatal, no value given for local secure image "
                           "directory", False)
                self.tripexception = True

        if self.securelocaldir:
            if self.securelocaldir[0] != '/':
                self.event("securelocaldir must be absolute path", False)
                self.securelocaldir = None
                self.tripexception = True
                return

        if not os.path.exists(self.securelocaldir):
            self.event("securelocaldir is configured, but '%s' does not"
                       " exist on the filesystem, attemping to create "
                       " it" % self.securelocaldir, False)
            try:
                os.mkdir(self.securelocaldir)
                os.chmod(self.securelocaldir, stat.S_IRUSR | stat.S_IWUSR | stat.S_IXUSR)
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                self.event("Problem creating securelocaldir: %s: %s\n" 
                           % (str(exceptname), str(sys.exc_value)), False)
                self.tripexception = True
                return

            self.event("(created securelocaldir '%s')" 
                       % self.securelocaldir, False)
            self.event("created securelocaldir '%s'" % self.securelocaldir)

        x = os.access(self.securelocaldir, os.W_OK | os.X_OK | os.R_OK)
        if x:
            self.event("'%s' exists on the filesystem and is "
                       "rwx-able" % self.securelocaldir)
        else:
            self.event("fatal, '%s' exists on the filesystem but is not rwx" 
                       % self.securelocaldir, False)
            self.tripexception = True
            return

        try:
            self.gucpath = self.getValue("systempaths","guc")
            self.event("found guc conf: %s" % self.gucpath)
            if not os.path.exists(self.gucpath):
                self.event("'%s' does not exist on the "
                           "filesystem" % self.gucpath, False)
                self.tripexception = True
        except:
            self.gucpath = None

        if self.gucpath:
            if self.gucpath[0] != '/':
                self.event("if configured, globus-url-copy must be absolute path", False)
                self.gucpath = None
                self.tripexception = True
                return

        try:
            self.scppath = self.getValue("systempaths","scp")
            self.event("found scp conf: %s" % self.scppath)
            if not os.path.exists(self.scppath):
                self.event("'%s' does not exist on the "
                           "filesystem" % self.scppath, False)
                self.tripexception = True
        except:
            self.scppath = None

        if self.scppath:
            if self.scppath[0] != '/':
                self.event("if configured, scp must be absolute path", False)
                self.scppath = None
                self.tripexception = True
                return
            else:
                try:
                    self.scpuser = self.getValue("behavior","scp_user")
                    self.event("found scp default user: %s" % self.scpuser)
                except:
                    self.scpuser = None
                    self.event("no value given for scp default user")

                try:
                    self.scpuser_override = self.getValue("behavior","scp_user_override")
                except NoValue:
                    self.scpuser_override = True
                else:
                    if string.lower(self.scpuser_override) == "false":
                        self.scpuser_override = False
                    else:
                        self.scpuser_override = True

                if self.scpuser_override:
                    self.event("caller can override default scp user")
                else:
                    self.event("caller can NOT override default scp user")

        exes = [self.gucpath, self.scppath]
        try:
            for i in exes:
                if i == None:
                    continue

                # will even work in a setuid environment
                x = os.access(i, os.X_OK)
                if x:
                    self.event("'%s' is executable" % i)
                else:
                    self.event("'%s' is NOT executable" % i, False)
                    self.tripexception = True
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            self.event("Unkown problem testing executable: %s: %s\n" % 
                       (str(exceptname), str(sys.exc_value)), False)
            self.tripexception = True

    ###########################
    # validateUnpropTargets() #
    ###########################
    def validateUnpropTargets(self):
        if not self.unproptargets:
            self.event("no given unpropagation target, assuming replacement of source")
            return

        if ';' in self.unproptargets:
            self.event("only one unpropagation target accepted currently", False)
            self.tripexception = True
            return

        # check if appropriate propagation mechanism is configured
        #self.gucpath or self.scppath 
        #if self.image[:9] == "gsiftp://" or self.image[:6] == "scp://":

        if self.unproptargets[:9] == "gsiftp://":
            if not self.gucpath:
                self.event("supplied unpropagation target "
                           "requires gsiftp but globus-url-copy is either not "
                           "configured or invalid", False)
                self.tripexception = True
                return

        elif self.unproptargets[:6] == "scp://":
            if not self.scppath:
                self.event("supplied unpropagation target requires scp but "
                           "it is either not configured or invalid", False)
                self.tripexception = True
                return

        else:
            self.event("unsupported unpropagation target: '%s'" % self.unproptargets, False)
            self.tripexception = True
            return

    ####################
    # validateImages() #
    ####################
    def validateImages(self):
        if not self.images:
            self.event("fatal, no images configured",False)
            self.tripexception = True
            return

        # parse images string to group of images

        # given input string might be quoted to escape semicolons
        # for certain delivery methods (e.g., sh over ssh) and
        # some methods may not strip quotes (e.g., localhost, direct
        # exe invocation)
        # so strip extra quotes if present
        if self.images[0] == "'":
            self.images = self.images[1:]

        # (there is a pathological case where input was only a single quote...)
        if self.images and self.images[-1] == "'":
            self.images = self.images[:-1]

        imgstrs = self.images.split(';;')

        i = 0
        imgs = []
        for imgstr in imgstrs:
            i += 1
            logstr = "IMG #%d" % i
            img = self.validatePartition(logstr, imgstr)
            if img:
                self.event("%s is valid" % logstr, True)
                imgs.append(img)
            else:
                self.event("%s is invalid" % logstr, False)
                # exception should have been tripped already

        self.event("** found %d valid partitions/HD images" % (len(imgs)), True)

        if len(imgs) < 1:
            self.event("no valid partitions/HD images", False)
            self.tripexception = True
            self.images = []
            self.image = None
            return

        self.images = imgs

        # TODO: for now as enhancement, just (potentially) propagating
        #       the first one which should be root partition or boot disk image
        self.image = self.images[0].givenpath
        self.event("main partition/HD image = '%s'" % self.image)

        try:
            self.xenmounttype = self.getValue("behavior","xenmounttype")
            self.event("found xenmounttype: '%s'" % self.xenmounttype)
        except:
            self.xenmounttype = None
            self.event("no value given for xenmounttype, assuming 'file'")

    ### validatePartition() - from validateImages()
    def validatePartition(self, logstr, imgstr):

        img = partition()

        # if 'ro' is not a field, assumed to be 'rw'
        parts = imgstr.split(';')
        if len(parts) > 1:
            if parts[1] == 'ro':
                img.isreadonly = True
        imgstr = parts[0]

        self.event("Examining file (partition/HD image): '%s'" % imgstr)

        if imgstr[0] == "/":
            img.path = imgstr
            imgstr = "file://" + imgstr
            img.givenpath = imgstr
            img.scheme = "file"
            self.event("  partition/HD is absolute path")

        elif imgstr[:8] == "file:///":
            img.givenpath = imgstr
            img.scheme = "file"
            img.path = imgstr[8:]
            self.event("  partition/HD is absolute path w/ file://")

        elif imgstr[:7] == "file://":
            img.givenpath = imgstr
            img.path = imgstr[7:]
            img.scheme = "file"
            self.event("  partition/HD is relative path w/ file://")
            img.relativepath = True

        elif imgstr[:14] == "blankcreate://":

            if not self.blankcreatepath:
                try:
                    self.blankcreatepath = \
                        self.getValue("systempaths","blankcreate")
                except NoValue:
                    self.blankcreatepath = None
                    self.event("fatal, no configuration for blankcreate"
                               "script, but blankcreate requested", False)
                    self.tripexception = True
                    return None
                i = self.blankcreatepath
                if not os.access(i, os.X_OK):
                    self.event("'%s' is NOT executable" % i, False)
                    self.tripexception = True
                    return None

            img.givenpath = imgstr
            img.path = imgstr[14:]
            if len(img.path) > 0:
                if img.path[0] == "/":
                    self.event("  blank partition creation can only happen in secure workspace-specific local directory (absolute path used: '%s')" % img.path, False)
                    self.tripexception = True
                    return None
            else:
                self.event("  partition has no name ('%s' given)" % img.givenpath, False)
                self.tripexception = True
                return None
            img.scheme = "blankcreate"

            try:
                size = img.path.split("-size-")[1]
                img.blankspace = int(size)
            except:
                self.event("  blank partition name is expected to have embedded size", False)
                self.tripexception = True
                return None

            if self.securelocaldir[-1] != '/':
                img.path = self.securelocaldir + '/' + \
                   self.name + '/' + img.path
            else:
                img.path = self.securelocaldir + \
                   self.name + '/' + img.path

            self.event("  partition of size %dM is going to be created (blankcreate) at '%s'" % (img.blankspace, img.path))

        elif imgstr[:9] == "gsiftp://":
            self.event("  partition/HD is specified w/ gsiftp://")
            img.givenpath = imgstr
            img.path = imgstr[9:]
            img.scheme = "gsiftp"
            img.needspropagation = True

        elif imgstr[:6] == "scp://":
            self.event("  partition/HD is specified w/ scp://")
            img.givenpath = imgstr
            img.path = imgstr[6:]
            img.scheme = "scp"
            img.needspropagation = True

        else:
            # requiring file:// for relative instead of defaulting
            # to relative -- in case some other, invalid scheme is used
            self.event("fatal, image specified is not absolute path "
                       "and uses unknown scheme (or perhaps no scheme): %s" 
                       % imgstr, False)
            self.tripexception = True
            return None

        if len(img.path) < 1:
            self.event("  partition has no name ('%s' given)" % img.givenpath, False)
            self.tripexception = True
            return None

        if img.isreadonly:
            self.event("  partition is read-only")
        else:
            self.event("  partition is read-write")


        isfile = False
        if img.scheme == 'file':
            isfile = True
            if not self.checkSecurePath(img.path):
                return None

        if img.relativepath and isfile:
            # relative image -- first try workspace-specific 
            # securelocaldir (if propagated, the image will be there)
            # then fallback to local image repository
            secureimg = img.path
            if self.securelocaldir[-1] != '/':
                secureimg = 'file://' + self.securelocaldir + '/' + \
                          self.name + '/' + secureimg
            else:
                secureimg = 'file://' + self.securelocaldir + \
                          self.name + '/' + secureimg

            foundinsecdir = False
            if os.path.exists(secureimg[7:]):
                foundinsecdir = True
                log.debug("found file relative to secure dir: %s" % secureimg)
                img.path = secureimg[7:]
                # TODO: remove once change-over to partition object is complete
                img.givenpath = secureimg
            elif secureimg[-3:] == '.gz':
                trywithoutgz = secureimg[7:]
                trywithoutgz = trywithoutgz[:-3]
                if os.path.exists(trywithoutgz):
                    foundinsecdir = True
                    log.debug("found file relative to secure dir: %s" % trywithoutgz)
                    img.path = trywithoutgz
                    img.givenpath = secureimg

            if not foundinsecdir:
                log.debug("relative path image NOT in secure dir: %s" % secureimg)

                if not self.localdir:
                    try:
                        self.localdir = self.getValue("images","localdir")
                    except NoValue:
                        self.event("fatal, no value given for local image "
                                   "directory, image specified with relative path "
                                   "and image file not in securelocaldir", False)
                        self.tripexception = True
                        return None
                if self.localdir:
                    # TODO: remove givenpath set, once change-over to 
                    # partition object is complete
                    img.givenpath = "file://" + self.localdir + '/' + img.path
                    img.path = self.localdir + '/' + img.path

        # can't check if this is on filesystem yet, it is not created
        if img.blankspace:
            return img

        if not self.handleImage(img.path, isfile):
            return None
        else:
            return img

    #########################
    # validateMountpoints() #
    #########################
    def validateMountpoints(self):
        if not self.imagemounts:
            self.event("fatal, no configuration for mountpoint(s) to "
                       "present image(s) as",False)
            self.tripexception = True
            return

        # TODO: parse imagemounts

        # parse imagemounts string to group of imagemounts

        # given input string might be quoted to escape semicolons
        # for certain delivery methods (e.g., sh over ssh) and
        # some methods may not strip quotes (e.g., localhost, direct
        # exe invocation)
        # so strip extra quotes if present
        if self.imagemounts[0] == "'":
            self.imagemounts = self.imagemounts[1:]

        # (there is a pathological case where input was only a single quote...)
        if self.imagemounts and self.imagemounts[-1] == "'":
            self.imagemounts = self.imagemounts[:-1]

        mountstrs = self.imagemounts.split(';;')

        self.imagemounts = []
        for mountstr in mountstrs:
            if mountstr[:5] == "/dev/":
                prev = mountstr
                mountstr = mountstr[5:]
                self.event("stripped '/dev' from '%s', now = '%s'" 
                           % (prev, mountstr))
            self.imagemounts.append(mountstr)

        if len(self.imagemounts) < 1:
            self.event("no mountpoints", False)
            self.tripexception = True
            self.imagemounts = []
            self.imagemount = None
            return

        nummts = len(self.imagemounts)
        numparts = len(self.images)
        if nummts != numparts:
            self.event("fatal, number of mountpoints (%d) does not match number of valid partitions/HD images (%d)" % (nummts,numparts), False)
            self.tripexception = True
            return

        # TODO: for now, to support enhancement, assuming first
        #       one is rootdisk
        self.imagemount = self.imagemounts[0]
        self.event("imagemount = '%s'" % self.imagemount)

        # if target is ~~sda1 then pygrub is not used and kernel is required
        if self.imagemount[-1].isdigit():
            self.hdimage = False
        else:
            self.hdimage = True
            if self.pygrubpath:
                self.event("HD image")
            else:
                self.event("HD image but HD image support has been disabled, can not continue", False)
                self.tripexception = True

    ######################
    # validateMountDir() #
    ######################
    def validateMountDir(self):
        try:
            self.mountpath = self.getValue("systempaths","mounttool")
        except NoValue:
            self.mountpath = None

        if not self.mountpath:
            self.event("no configuration for mounttool, features are disabled")
            return
        else:
            if self.mountpath[0] != '/':
                self.event("mounttool path must be absolute path", False)
                self.mountpath = None
                self.tripexception = True
                return

        try:
            self.mountdir = self.getValue("systempaths","mountdir")
        except NoValue:
            self.mountdir = None

        if not self.mountdir:
            self.event("no configuration for mountdir, features are disabled")
            return
        else:
            if self.mountdir[0] != '/':
                self.event("mountdir must be absolute path", False)
                self.mountdir = None
                self.tripexception = True
                return

        if not os.path.exists(self.mountdir):
            self.event("mount directory is configured, but '%s' does not"
                       " exist on the filesystem, attemping to create "
                       " it" % self.mountdir, False)
            try:
                os.mkdir(self.mountdir)
                os.chmod(self.mountdir, stat.S_IRUSR | stat.S_IWUSR | stat.S_IXUSR)
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                self.event("Problem creating mountdir: %s: %s\n" 
                           % (str(exceptname), str(sys.exc_value)), False)
                self.mountdir = None
                self.tripexception = True
                return

            self.event("(created mountdir '%s')" % self.mountdir, False)
            self.event("created mountdir '%s'" % self.mountdir)

        # Needs to be writable so we can make subdirectories.
        # Using one mnt directory for all workspaces would mean 
        # we'd have to lock the mounttool callout to prevent races
        x = os.access(self.mountdir, os.W_OK | os.X_OK | os.R_OK)
        if x:
            self.event("'%s' exists on the filesystem and is "
                       "rwx-able" % self.mountdir)
        else:
            self.event("fatal, '%s' exists on the filesystem but is not rwx" 
                       % self.mountdir, False)
            self.mountdir = None
            self.tripexception = True
            return

    #####################
    # validateTempDir() #
    #####################
    def validateTempDir(self):
        # TODO: collapse this and near-duplicate function validateMountDir()
        try:
            self.tmpdir = self.getValue("systempaths","tmpdir")
        except NoValue:
            self.tmpdir = None

        if not self.tmpdir:
            self.event("no configuration for tmpdir, features are disabled")
            return
        else:
            if self.tmpdir[0] != '/':
                self.event("tmpdir must be absolute path", False)
                self.tmpdir = None
                self.tripexception = True
                return

        if not os.path.exists(self.tmpdir):
            self.event("tmp directory is configured, but '%s' does not"
                       " exist on the filesystem, attemping to create "
                       " it" % self.tmpdir, False)
            try:
                os.mkdir(self.tmpdir)
                os.chmod(self.tmpdir, stat.S_IRUSR | stat.S_IWUSR | stat.S_IXUSR)
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                self.event("Problem creating tmpdir: %s: %s\n" 
                           % (str(exceptname), str(sys.exc_value)), False)
                self.tmpdir = None
                self.tripexception = True
                return

            self.event("(created tmpdir '%s')" % self.tmpdir, False)
            self.event("created tmpdir '%s'" % self.tmpdir)

        # Needs to be writable so we can make subdirectories.
        # Using one mnt directory for all workspaces would mean 
        # we'd have to lock the mounttool callout to prevent races
        x = os.access(self.tmpdir, os.W_OK | os.X_OK | os.R_OK)
        if x:
            self.event("'%s' exists on the filesystem and is "
                       "rwx-able" % self.tmpdir)
        else:
            self.event("fatal, '%s' exists on the filesystem but is not rwx" 
                       % self.tmpdir, False)
            self.tmpdir = None
            self.tripexception = True
            return


    ########################
    # validateMountTasks() #
    ########################
    def validateMountTasks(self):
        if not self.mnttasks:
            return

        if not self.mountpath:
            self.event("fatal, mount+copy tasks assigned but there is no mounttool configuration", False)
            self.tripexception = True
            return

        if not self.tmpdir:
            self.event("fatal, mount+copy tasks assigned but there is no tmpdir configuration", False)
            self.tripexception = True
            return

        # given input string might be quoted to escape semicolons
        # for certain delivery methods (e.g., sh over ssh) and
        # some methods may not strip quotes (e.g., localhost, direct
        # exe invocation)
        # so strip extra quotes if present
        if self.mnttasks[0] == "'":
            self.mnttasks = self.mnttasks[1:]

        # (there is a pathological case where input was only a single quote...)
        if self.mnttasks and self.mnttasks[-1] == "'":
            self.mnttasks = self.mnttasks[:-1]

        tasks = self.mnttasks.split(';;')
        self.event("found %d mount+copy tasks" % len(tasks))
        for task in tasks:
            parts = task.split(';')
            if len(parts) != 2:
                self.event("fatal, invalid mount+copy task: '%s'" % task, False)
                self.tripexception = True
            else:
                self.mnttask_list.append( (parts[0],parts[1]) )

        # TODO: check that the file is there

    ####################
    # validateMemory() #
    ####################
    def validateMemory(self):
        try:
            self.maxvmram = self.getValue("behavior", "find_maxvmram")
        except NoValue:
            self.event("no configuration for finding max VM RAM, "
                       "default is false")
            self.maxvmram = "false"

        if string.lower(self.maxvmram) == "true":
            self.maxvmram = True
        else:
            self.maxvmram = False

        try:
            self.memory = int(self.memory)
        except:
            if self.memory:
                self.event("fatal, memory requested not an "
                           "integer: %s" % self.memory, False)
            self.memory = None

        maxavailable = None
        if self.maxvmram:
            if self.assume_xend == False:
                maxavailable = self.xendNonDom0Mem()
                self.event("max ram available: %d" % maxavailable)
            else:
                self.event("cannot retrieve current used VM RAM, 'assume_xend'"
                           "is set to true in configuration", False)

            if not self.memory:
                try:
                    givemax = self.getValue("behavior","give_maxmem")
                except NoValue:
                    self.event("no configuration for allowing max mem to VMs "
                               "if memory is not specified, assuming false")
                    givemax = "false"

                if string.lower(givemax) == "true":
                    self.memory = maxavailable

        if self.memory:
            self.event("memory requested: %d" % self.memory)
            if maxavailable != None:
                if self.memory <= maxavailable:
                    self.event("memory available for vm creation will be "
                               "OK (likely)")
                else:
                    self.event("probably fatal, memory not available for vm"
                               "creation, requesting %d, only %d available" 
                               % (self.memory,maxavailable), False)
                    # let it fail at runtime
                    #self.tripexception = True
        else:
            self.event("fatal, no memory configuration", False)
            self.tripexception = True

    ####################
    # validateNumcpu() #
    ####################
    def validateNumcpu(self):
        num = 1
        numstr = None
        try:
            numstr = self.getValue("behavior", "num_cpu_per_vm")
        except NoValue:
            self.event("no configuration for num CPUs, default is 1")
        if numstr:
            try:
                num = int(numstr)
            except:
                self.event("given numcpu config is invalid, setting to 1")
        if num < 1:
            self.event("numcpu config is less than zero? setting to 1")
            num = 1
        self.numcpus = num

    ########################
    # validateNameCreate() #
    ########################
    def validateNameCreate(self):
        if not self.xmstdout:
            self.event("no xm output is available (perhaps assume_xend "
                       "is true)")
            return None

        # hack, come back to this later.
        if self.xmstdout.find(self.name) == -1:
            self.event("name is not present in xm list")
        else:
            self.event("may be fatal, name is already present in xm "
                       "list", False)

    ########################
    # validateNetworking() #
    ########################
    def validateNetworking(self):
        if not self.networking:
            log.debug("no networking present, checking for default")
            require_networking = False
            try:
                require_networking = \
                                   self.getValue("behavior","require_networking")
                if string.lower(require_networking) == "true":
                    require_networking = True
            except NoValue:
                log.debug("no configuration for require_networking, "
                          "assuming false")

            try:
                self.networking = \
                    self.getValue("behavior","default_networking")
            except NoValue:
                self.event("no configuration for default networking, "
                           "networking disabled", False)
                if require_networking:
                    self.event("fatal, networking is required, but none "
                               "supplied and no default", False)
                    self.tripexception = True
                else:
                    self.networking = None
                return

        self.valid_ips_dict = None
        self.macassociation_dict = { }
        self.ipassociation_dict = { }

        # may check IP against authz/sanity
        if not self.bridgemap or not self.dhcpvifmap:
            self.validateAssociations()

        if not self.mactrack:
            self.validate_MAC_tracking()

        if not self.iptrack:
            self.validate_IP_tracking()

        # if associations have changed, reflect this in tracking objects
        if self.mactrack or self.iptrack:

            for x in self.macassociation_dict.keys():
                if self.mactrack:
                    if self.mactrack.newAssociation(x, self.macassociation_dict[x]):
                        self.event("added association '%s' to MAC tracking" 
                                   % x, True)
                    else:
                        log.debug("association '%s' was already in MAC "
                                  "tracking" % x)
                if self.iptrack:
                    if self.iptrack.newAssociation(x):
                        self.event("added association '%s' to IP "
                                   "tracking" % x, True)
                    else:
                        log.debug("association '%s' was already in IP "
                                  "tracking" % x)     

            if self.mactrack:
                for y in self.mactrack.macdict.keys():
                    if not y in self.macassociation_dict.keys():
                        self.mactrack.retireAssocation(y)
                        self.event("association '%s' was removed from MAC "
                                   "tracking, it is no longer in conf file" 
                                   % y, True)
            if self.iptrack:
                for y in self.iptrack.ipdict.keys():
                    if not y in self.macassociation_dict.keys():
                        self.iptrack.retireAssocation(y)
                        self.event("association '%s' was removed from IP "
                                   "tracking, it is no longer in conf file" 
                                   % y, True)                        
            if self.mactrack:
                persistMAC(self)
            if self.iptrack:
                persistIP(self)            


        # given input string might be quoted to escape semicolons
        # for certain delivery methods (e.g., sh over ssh) and
        # some methods may not strip quotes (e.g., localhost, direct
        # exe invocation)
        # so strip extra quotes if present
        if self.networking[0] == "'":
            self.networking = self.networking[1:]

        # (there is a pathological case where input was only a single quote...)
        if self.networking and self.networking[-1] == "'":
            self.networking = self.networking[:-1]

        # process all the given settings
        nicstrings = self.networking.split(';;')

        # cannot use map w/o losing the NIC# ?
        #nics = map(self.validateNIC, nicstrings)

        i = 0
        nics = []
        for nicstr in nicstrings:
            i += 1
            logstr = "NIC #%d" % i
            nic = self.validateNIC(logstr, nicstr)
            if nic:
                # need to know vifname ahead of time for ebtables rules
                # (NOTE: interface name cannot exceed 15 char)
                nic.vifname = self.name + "-%d" % i
                self.event("%s: chose '%s' for vif name" % (logstr,nic.vifname))

                if nic.mac:
                    self.event("%s is valid, MAC=%s" % (logstr, nic.mac), True)
                else:
                    self.event("%s is valid, MAC will be chosen by Xen" 
                               % logstr, True)

                nics.append(nic)
            else:
                self.event("%s is invalid" % logstr, False)
                # exception should have been tripped already

        self.event("** found %d valid NICs" % (len(nics)), True)

        self.networking = nics

        if self.tripexception:
            self.event("failure is triggered, backing out any networking "
                       "reservations", False)
            remove_tracked(False, self)

    #################
    # validateNIC() #
    #################
    def validateNIC(self, logstring, nicstring):

        log.debug("%s: ________________" % logstring)
        log.debug("%s: processing input: %s" % (logstring, nicstring))

        opts = nicstring.split(';')

        if len(opts) < 5:
            self.event("fatal, %s: NIC does not have the 5 minimum fields, "
                       "full input: %s" % (logstring,nicstring), False)
            self.tripexception = True
            return None

        nic = xen_nic()

        # this can be any string and is not currently used
        nic.name = opts[0]
        log.debug("%s: name = %s" % (logstring, nic.name))

        # this can be any string
        nic.association = (opts[1])
        log.debug("%s: association = %s" % (logstring, nic.association))
        if self.mactrack:
            if not nic.association in self.mactrack.macdict.keys():
                self.event("fatal, %s: association '%s' is not registered in "
                           "MAC tracking, but MAC tracking is enabled"
                           % (logstring, nic.association), False)
                self.tripexception = True

        if string.lower(opts[2]) != "any":
            nic.mac = opts[2]
            log.debug("%s: MAC = %s" % (logstring, nic.mac))
            if not self.checkMAC(nic.mac):
                self.event("fatal, %s: MAC is invalid: %s" 
                           % (logstring, nic.mac), False)
                self.tripexception = True
                return None
            if self.mactrack:
                new = self.mactrack.addMAC(nic.association, nic.mac)
                if not new:
                    self.event("fatal, %s: MAC is already in use for "
                               "association '%s'" 
                               % (logstring, nic.association), False)
                    self.tripexception = True
                    return None
                else:
                    persistMAC(self)

        else:
            if not self.mactrack:
                log.debug("%s: MAC will be assigned by Xen" % logstring)
            else:
                nic.mac = self.mactrack.newMAC(nic.association)
                self.event("%s: MAC not given, so assigned: %s" 
                           % (logstring, nic.mac), True)
                persistMAC(self)

        nic.nic_type = assignNicType(string.upper(opts[3]))
        if nic.nic_type == None:
            self.event("%s: fatal, NIC type (field 4) unknown" 
                       % logstring, False)
            self.tripexception = True
            return None

        # todo: factor out bridge assignments from association routine

        if nic.nic_type == NicType.BRIDGED:
            log.debug("%s: NIC will be bridged" % logstring)
        else:
            self.event("%s: NIC type is not 'bridged', no other type is "
                       "supported in this version" % logstring, False)
            self.tripexception = True
            return None

        nic.configuration_mode = assignConfigurationMode(string.upper(opts[4]))
        if nic.configuration_mode == None:
            self.event("%s: fatal, configuration mode (field 5) unknown" 
                       % logstring, False)
            self.tripexception = True
            return None

        if nic.configuration_mode == ConfigurationMode.INDEPENDENT:
            log.debug("%s: NIC will be configured independently inside VM" 
                      % logstring)
        elif nic.configuration_mode == ConfigurationMode.STATIC:
            log.debug("%s: NIC will be configured statically" % logstring)
        else:
            self.event("%s: NIC configuration mode is not 'independent' or "
                       "'static', no other type is supported in this version" 
                       % logstring, False)
            self.tripexception = True
            return None

        if nic.configuration_mode in (ConfigurationMode.INDEPENDENT, ConfigurationMode.STATIC):

            # independent and static need all settings even if 'null'
            if len(opts) != 15:
                self.event("%s: fatal, networking mode configured 'static' or "
                           "'independent', but not enough settings are supplied " % logstring, False)
                self.tripexception = True
                return None

            # TODO: support 0.0.0.0/# form for netmask
            tripd = False

            # the ip
            log.debug("%s: checking IP %s" % (logstring, opts[5]))
            if not self.checkIP(opts[5]):
                self.event("%s: fatal, IP setting %s is an invalid IP" % (logstring, opts[5]), False)
                self.tripexception = True
                tripd = True

            log.debug("%s: checking gateway %s" % (logstring, opts[6]))
            if opts[6].strip().lower() == "null":
                self.event("%s: gateway not set, perhaps being added to an isolated subnet" % logstring)
                opts[6] = None
            elif not self.checkIP(opts[6]):
                self.event("%s: fatal, gateway setting %s is an invalid IP" % (logstring, opts[6]), False)
                self.tripexception = True
                tripd = True            

            # tries to give appropriate default
            log.debug("%s: checking broadcast %s" % (logstring, opts[7]))
            if opts[7].strip().lower() == "null":
                # derive the broadcast from given IP 
                log.debug("%s: guessing broadcast from IP %s" % (logstring, opts[5]))
                (clz, guess) = broadcastGuess(opts[5])
                if guess != None:
                    self.event("%s: no broadcast given, IP %s is a class %s"
                               " address, assigning %s" % (logstring, opts[5], clz, guess))
                    opts[7] = guess
                else:
                    self.event("%s: fatal, given IP %s was not class A, B, "
                               "or C?" % (logstring, opts[5]), False)
                    self.tripexception = True
                    tripd = True

            elif not self.checkIP(opts[7]):
                self.event("%s: fatal, broadcast setting %s is an invalid IP" % (logstring, opts[7]), False)
                self.tripexception = True
                tripd = True


            # tries to give appropriate default
            log.debug("%s: checking subnet mask %s" % (logstring, opts[8]))
            if opts[8].strip().lower() == "null":
                # derive the broadcast from given IP 
                log.debug("%s: guessing subnet mask from IP %s" % (logstring, opts[5]))
                (clz, guess) = subnetGuess(opts[5])
                if guess != None:
                    self.event("%s: no subnet mask given, IP %s is a class %s"
                               " address, assigning %s" % (logstring, opts[5], clz, guess))
                    opts[8] = guess
                else:
                    self.event("%s: fatal, given IP %s was not class A, B, "
                               "or C?" % (logstring, opts[5]), False)
                    self.tripexception = True
                    tripd = True

            elif not self.checkIP(opts[8]):
                self.event("%s: fatal, subnet mask setting %s is an invalid IP" % (logstring, opts[8]), False)
                self.tripexception = True
                tripd = True

            log.debug("%s: checking dns %s" % (logstring,opts[9]))
            if opts[9].strip().lower() == "null":
                self.event("%s: dns not set" % logstring)
                opts[9] = None
            elif not self.checkIP(opts[9]):
                self.event("%s: fatal, dns setting %s is an invalid IP" % (logstring, opts[9]), False)
                self.tripexception = True
                tripd = True

            log.debug("%s: checking hostname %s" % (logstring, opts[10]))
            if opts[10].strip().lower() == "null":
                self.event("%s: hostname not set" % logstring)
                opts[10] = None

            allfour = 0
            log.debug("%s: checking certname %s" % (logstring, opts[11]))
            if opts[11].strip().lower() == "null":
                self.event("%s: certname not set" % logstring)
                opts[11] = None
            else:
                allfour += 1

            log.debug("%s: checking keyname %s" % (logstring, opts[12]))
            if opts[12].strip().lower() == "null":
                self.event("%s: keyname not set" % logstring)
                opts[12] = None
            else:
                allfour += 1

            log.debug("%s: checking certpath %s" % (logstring, opts[13]))
            if opts[13].strip().lower() == "null":
                self.event("%s: certpath not set" % logstring)
                opts[13] = None
            else:
                allfour += 1

            log.debug("%s: checking keypath %s" % (logstring,opts[14]))
            if opts[14].strip().lower() == "null":
                self.event("%s: keypath not set" % logstring)
                opts[14] = None
            else:
                allfour += 1

            if allfour != 0:
                if allfour != 4:
                    self.event("%s: fatal, either send no cert configs or "
                               "send all four: certname=%s, keyname=%s, certpath=%s, "
                               "keypath=%s" 
                               % (logstring, opts[11],opts[12],opts[13],opts[14]), False)
                    self.tripexception = True
                    tripd = True

            if tripd:
                return None

            nic.ip = opts[5]
            nic.gateway = opts[6]
            nic.broadcast = opts[7]
            nic.netmask = opts[8]
            nic.dns = opts[9]
            nic.hostname = opts[10]
            nic.certname = opts[11]
            nic.keyname = opts[12]
            nic.certpath = opts[13]
            nic.keypath = opts[14]

            if self.bridgemap:
                try:
                    nic.bridge = self.bridgemap[nic.association]
                    self.event("%s: assigning bridge '%s'" 
                               % (logstring,nic.bridge), True)
                except KeyError:
                    nic.bridge = None
                    self.event("%s: no bridge assignment?" 
                               % (logstring), False)
                    self.tripexception = True
                    return None

            if self.dhcpvifmap:
                try:
                    nic.dhcpvifname = self.dhcpvifmap[nic.association]
                    self.event("%s: assigning dhcp vif '%s'" 
                               % (logstring, nic.dhcpvifname), True)
                except KeyError:
                    nic.dhcpvifname = None
                    self.event("%s: no dhcpvif assignment?" 
                               % (logstring), False)
                    self.tripexception = True
                    return None

            if self.valid_ips_dict:
                try:
                    okranges = self.valid_ips_dict[nic.association]
                except KeyError:
                    okranges = None
                    self.event("fatal, check_ip_ranges is configured, but no "
                               "configuration for association %s" 
                               % nic.association, False)
                    self.tripexception = True
                    return None

                if okranges:
                    log.debug("found validity list for association '%s': %s" 
                              % (nic.association, okranges))
                    ok = False
                    for ip in okranges:
                        if nic.ip in ip:
                            log.debug("IP %s is in IP range %s" % (nic.ip, ip))
                            ok = True

                    if not ok:
                        self.event("fatal, IP %s is NOT in any ranges "
                                   "configured for association '%s'" 
                                   % (nic.ip, nic.association), False)
                        self.tripexception = True
                        return None

            if self.iptrack:
                new = self.iptrack.addIP(nic.association, nic.ip)
                if not new:
                    self.event("fatal, %s: IP already in use for association "
                               "'%s'" % (logstring, nic.association), False)
                    self.tripexception = True
                    return None
                else:
                    persistIP(self)

        return nic

    ###########################
    # validate_MAC_tracking() #
    ###########################
    def validate_MAC_tracking(self):
        """Retrieves or sets up persistence object for MAC tracking. 
           Bug in "xm create vif" necessitates MAC creation and
           tracking for all unspecified MACs, 
           see addWorkspace -> 'if self.opts.networking:' code"""

        if self.mactrack:
            return

        trackMACs = True
        try:
            trackMACs = self.getValue("networking","track_MAC_assignments")
            if string.lower(trackMACs) == "false":
                trackMACs = False
                log.debug("networking:track_MAC_assignments is set to false")
        except NoValue:
            log.debug("no configuration for networking:track_MAC_assignments, "
                      "assuming true")

        if not trackMACs:
            return

        if not self.persistencedir:
            self.event("persistence is not configured, but required for MAC "
                       "tracking", False)
            self.tripexception = True
            return

        depersistMAC(self)
        if not self.mactrack:
            self.event("cannot instantiate MAC tracking", False)
            self.tripexception = True

    ##########################
    # validate_IP_tracking() #
    ##########################
    def validate_IP_tracking(self):
        """Retrieves or sets up persistence object for IP tracking."""
        if self.iptrack:
            return

        trackIPs = True
        try:
            trackIPs = self.getValue("networking","track_IP_assignments")
            if string.lower(trackIPs) == "false":
                trackIPs = False
                log.debug("networking:track_IP_assignments is set to false")
        except NoValue:
            log.debug("no configuration for networking:track_IP_assignments, "
                      "assuming true")

        if not trackIPs:
            return

        if not self.persistencedir:
            self.event("persistence is not configured, but required for "
                       "IP tracking", False)
            self.tripexception = True
            return

        depersistIP(self)
        if not self.iptrack:
            self.event("cannot instantiate IP tracking", False)
            self.tripexception = True

    ##########################
    # validateAssociations() #
    ##########################
    def validateAssociations(self):
        """set up dict of associations for checking that the assigned 
           IP setting is in a sane/authorized range
           set up dict of association->bridge and association->dhcpvif"""

        check_ip_ranges = True
        try:
            check_ip_ranges = self.getValue("networking","check_ip_ranges")
            if string.lower(check_ip_ranges) == "false":
                check_ip_ranges = False
                self.valid_ips_dict = None
                log.debug("networking:check_ip_ranges is set to false")
        except NoValue:
            log.debug("no configuration for networking:check_ip_ranges, "
                      "assuming true")


        self.bridgemap = { }
        self.dhcpvifmap = { }

        if check_ip_ranges:
            self.valid_ips_dict = { }

        # find associations
        i = -1
        while True:
            i += 1
            try:
                association = self.getValue("networking", "association_%d" % i)
                #log.debug("found assocation_%d = %s" % (i, association))
            except NoValue:
                log.debug("no configuration for assocation_%d, "
                          "ending search" % i)
                break
            if association:
                whole = association.split(";")
                name = whole[0]
                log.debug("found association_%d: %s" % (i,name))

                if len(whole) != 5:
                    self.event("not enough fields for assocation_%d = %s, "
                               "skipping it" % (i,name), False)
                    continue

                # bridge
                br = string.strip(whole[1])
                log.debug("found bridge '%s' for %s" % (br, name))
                self.bridgemap[name] = br

                # dhcpvif
                vif = string.strip(whole[2])
                log.debug("found dhcp vif '%s' for %s" % (vif, name))
                self.dhcpvifmap[name] = vif

                # macs
                whole[3] = string.strip(whole[3])
                if string.lower(whole[3]) == "none":
                    self.macassociation_dict[name] = None
                else:
                    self.macassociation_dict[name] = whole[3]

                # IPs
                if check_ip_ranges:
                    if self.valid_ips_dict.has_key(name):
                        self.event("%s is already configured, skipping this "
                                   "repeat" % name, False)
                        continue

                    values = whole[4].split(",")
                    valid_values = []

                    for value in values:
                        try:
                            ip = IP(value)
                            log.debug("found valid value for association_%d: "
                                      "%s" % (i, value))
                        except ValueError:
                            self.event("content for assocation_%d is invalid, "
                                       "skipping it: %s" % (i, value), False)
                            continue
                        valid_values.append(ip)

                    self.valid_ips_dict[name] = valid_values
        if len(self.bridgemap) == 0:
            raise EnvironmentProblem("no valid associations")
        #log.debug("*** found %d association(s)" % len(self.valid_ips_dict))


    # end: for validateCreate()


    # ---------------------------------------------------------------------- #
    # for validateRemove()                                                   #
    # ---------------------------------------------------------------------- #

    ########################
    # validateNameRemove() #
    ########################
    def validateNameRemove(self):
        if not self.xmstdout:
            self.event("no xm output is available (perhaps assume_xend is "
                       "true)")
            return None

        # hacky, come back to this later.
        if self.xmstdout.find(self.name) != -1:
            self.event("name is present in xm list")
        else:
            self.event("--- NOTE --- name is not present in xm list, "
                       "remove will likely fail", False)

    ##########################
    # loadShutdownBehavior() #
    ##########################
    # This could move to be a create-time option as well (that is stored)
    def loadShutdownBehavior(self):
        defaultpause = 15
        defaultcheck = True

        if self.checkshutdown == None:
            try:
                self.checkshutdown = self.getValue("behavior","checkshutdown")
            except NoValue:
                self.checkshutdown = defaultcheck
                self.event("no configuration for checkshutdown, "
                           "using default: %s" % self.checkshutdown)

        if self.checkshutdownpause == None:
            try:
                self.checkshutdownpause = \
                    self.getValue("behavior","checkshutdownpause")
            except NoValue:
                self.checkshutdownpause = defaultpause
                self.event("no configuration for checkshutdownpause, "
                           "using default: %d" % self.checkshutdownpause)

        if self.checkshutdown == True or self.checkshutdown == False:
            pass
        elif string.lower(self.checkshutdown) == "true":
            self.checkshutdown = True
        else:
            self.checkshutdown = False

        self.event("using checkshutdown: %s" % self.checkshutdown)

        try:
            self.checkshutdownpause = int(self.checkshutdownpause)
        except:
            self.checkshutdownpause = defaultpause
            self.event("configured checkshutdownpause can not be "
                       "converted into an integer, reverting to "
                       "default: %d" % self.checkshutdownpause)

        self.event("using checkshutdownpause: %s" % self.checkshutdownpause)


    # end: for validateRemove()


    # ---------------------------------------------------------------------- #

    #########
    # utils #
    #########

    def checkSecurePath(self, image):
        """Until we have image signing, this just checks that all is well,
           particularly that no one is remotely specifying images or kernels
           that lie in the secure image directory"""

        if image[:7] == "file://":
            name = image[7:]
        else:
            name = image

        log.debug("  checking security for path = '%s'" % name)

        if name == "":
            self.event("fatal, image path is empty" % name, False)
            self.tripexception = True
            return False

        if ".." in name:
            self.event("fatal, '%s' contains '..', not allowed" % name, False)
            self.tripexception = True
            return False

        if not self.securelocaldir:
            try:
                self.securelocaldir = self.getValue("images","securelocaldir")
            except NoValue:
                self.event("no value given for local secure image dir", False)
                return False

        log.debug("  securelocaldir = '%s'" % self.securelocaldir)

        # by checking for / we bypass a corner case
        if name[0] == '/' and self.securelocaldir in name:
            self.event("attempt to specify absolute path that lies in "
                       "secure image directory", False)
            self.tripexception = True
            return False

        return True

        # sym/hard link checks necessary?

    def handleImage(self, image, isfile=False, trymatchingramdisk=False):
        """Right now this only handles file://
        """
        if image[:7] == "file://":
            image = image[7:]
            isfile = True

        if not isfile:
            return True

        if not os.path.exists(image):

            if trymatchingramdisk:
                msg = "initrd image being sought via kernel matching algorithm is '%s', this does not exist on the filesystem, and so not using any initrd" % image
                self.event(msg)
                self.ramdisk = None
                return True
            else:
                self.event("fatal, image '%s' does not exist on the filesystem" % image, False)
                self.tripexception = True
                return False
        else:
            x = os.access(image, os.R_OK)
            if x:
                self.event("  '%s' exists on the filesystem and is readable"
                           % image)
                if trymatchingramdisk:
                    self.event("initrd matched with kernel, will be used: '%s'" % image)
                return True

            else:
                self.event("fatal, '%s' exists on the filesystem but is "
                           "not readable" % image)
                self.tripexception = True
                return False

    # move 
    def checkIP(self, ipstring):
        try:
            [a,b,c,d] = map(string.atoi, string.splitfields(ipstring, '.'))
            x = range(0,256)
            for i in (a,b,c,d):
                if i not in x:
                    return False
            return True
        except:
            return False

    # move 
    def checkMAC(self, macstring):
        try:
            x = macstring.split(":")
            if len(x) != 6:
                return False
            for i in x:
                if len(i) != 2:
                    return False
                for j in i:
                    if j not in string.hexdigits:
                        return False
            return True
        except:
            return False

    def getValue(self, section, key):
        return config.valueResolve(self.config, self.family, section, key)

    def event(self, msg, ok=True):
        # adding debug here prevents useful line numbers from showing
        # up in the log (fix somehow?)
        log.debug(msg)
        if ok:
            self.okstr += config.format(msg)
        else:
            self.errorstr += config.format(msg)

    def xendRestartTime(self):
        try:
            secs = self.getValue("behavior", "restart_xend_secs")
        except NoValue,err:
            self.event("restart xend seconds not configured, waiting for "
                       "default of 1.0 seconds before xm list")
            return 1.0
        else:
            try:
                seconds = float(secs)
            except:
                self.event("restart xend seconds is configured, but invalid: "
                           "%s, waiting for default 1.0 seconds before xm list" 
                           % secs)
                return 1.0
            self.event("waiting for configured xend restart seconds: %s" 
                       % str(seconds))
            return seconds

    def xendNonDom0Mem(self):
        """ this should be broken out with subcommand as parameter...  
            copied from validateXen
        """

        try:
            # assuming stdout on success for xm instead of checking ret code,
            # much better to use Popen() but not available in Python < 2.4
            cmd = "%s info" % self.xmpath
            fstdin, fstdout, fstderr = os.popen3(self.xmsudo(cmd))
            fstdin.close()
            self.xminfo = fstdout.read()
            fstdout.close()
            stderr = fstderr.read()
            fstderr.close()

            # totally unecessary to check again, but here on the offchance
            # xend dies between the last restart and now (!)
            if self.xminfo == '':
                self.event("cannot contact xend", False)
                if stderr:
                    self.event("stderr: %s" % stderr, False)
                    if "root access" in stderr:
                        self.event("sudo seems to be misconfigured", False)
                        self.tripexception = True
                        return
                    if "trust you have received the usual" in stderr:
                        self.event("sudo seems to be misconfigured", False)
                        self.tripexception = True
                        return
                try:
                    restart_xend = self.getValue("behavior","restart_xend")
                except NoValue,err:
                    self.event("fatal: cannot contact xend, restart_xend not "
                               "configured, but default is false, not "
                               "restarting", False)
                    self.tripexception = True
                else:
                    if string.lower(restart_xend) == "true":
                        cmd = "%s restart" % self.xendpath
                        ret = os.system(self.xensudo(cmd))
                        if ret:
                            self.event("fatal: attempted to restart xend and "
                                       "failed",False)
                            self.tripexception = True
                        else:
                            self.event("restarted xend")
                            time.sleep(self.xendRestartTime())
                            cmd = "%s info" % self.xmpath
                            fstdin,fstdout,fstderr = \
                                  os.popen3(self.xmsudo(cmd))

                            fstdin.close()
                            self.xminfo = fstdout.read()
                            fstdout.close()
                            stderr = fstderr.read()
                            fstderr.close()

                            if self.xminfo == '':
                                self.event("fatal: restarted xend, but xm "
                                           "info fails", False)
                                if stderr:
                                    self.event("stderr: %s" % stderr, False)
                                self.tripexception = True
                            else:
                                self.event("restarted xend, running now")
                                self.event("xm info output:\n%s" % self.xminfo)
                    else:
                        self.event("fatal: cannot contact xend, restart_xend"
                                   "configured, but not to 'true', not "
                                   "restarting",False)
                        self.tripexception = True
            else:
                self.event("xend is running")
                self.event("xm info output:\n%s" % self.xminfo)
                x = self.xminfo.split('\n')
                h = None
                for i in x:
                    if i[:11] == "free_memory":
                        h = i
                        break
                if not h:
                    self.event("xm info is not reporting free_memory?",False)
                    self.tripexception = True
                    return None
                j = h.split(':')
                try:
                    g = j[1]
                    mem = int(g)
                    return mem
                except:
                    self.event("xm info is not reporting free_memory?",False)
                    self.tripexception = True
                    return None
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            self.event("Uknown problem w/ xm info or xend restart: %s: %s\n"
                       % (str(exceptname), str(sys.exc_value)), False)
            self.tripexception = True


# -------------------------------------------------------------------------- #
# functions in xen_v2                                                        #
# -------------------------------------------------------------------------- #

###############
# persistence #
###############

def remove_tracked(dryrun, createopts):
    if not createopts or not createopts.networking:
        log.warn("cannot remove dhcp/ebtables rules without create params")
        if not createopts:
            return
    if createopts.networking:
        log.debug("   found %d nics" % len(createopts.networking))
        mactrack = None
        iptrack = None
        if createopts.mactrack:
            depersistMAC(createopts)
        if createopts.iptrack:
            depersistIP(createopts)
        if createopts.mactrack:
            for nic in createopts.networking:
                log.debug("   found MAC '%s' with association '%s'" 
                          % (nic.mac, nic.association))
                if dryrun:
                    log.debug("   dryrun, not removing MAC from tracking")
                else:
                    if createopts.mactrack.retireMAC(nic.association, nic.mac):
                        log.debug("   removed MAC from tracking")
                    else:
                        log.debug("   failed to remove MAC from tracking")
            if not dryrun:
                persistMAC(createopts)
        if createopts.iptrack:
            for nic in createopts.networking:
                if not nic.ip:
                    log.debug("   no IP to remove from tracking")
                else:
                    log.debug("   found IP '%s' with association '%s'" 
                              % (nic.ip, nic.association))
                    if dryrun:
                        log.debug("   dryrun, not removing IP from tracking")
                    else:
                        if createopts.iptrack.retireIP(nic.association,nic.ip):
                            log.debug("   removed IP from tracking")
                        else:
                            log.debug("   failed to remove IP from tracking")
            if not dryrun:
                persistIP(createopts)
        for nic in createopts.networking:
            if nic.configuration_mode == ConfigurationMode.STATIC:
                # would be an odd but possible situation, calling
                # remove with different conf than add
                if not createopts.dhcpconfigpath:
                    log.error("static networking assignments require dhcp-config")
                    break
                if not createopts.sudopath:
                    log.error("dhcp-config requires sudo")
                    break

                cmd = "%s %s rem %s %s " % (createopts.sudopath, createopts.dhcpconfigpath, nic.vifname, nic.ip)

                try:
                    ret,output = getstatusoutput(cmd)
                except:
                    log.exception("problem removing dhcp/ebtables entries")

                if ret:
                    errmsg = "problem running command: '%s' ::: return code" % cmd
                    errmsg += ": %d ::: output:\n%s" % (ret, output)
                    log.error(errmsg)
                else:
                    log.debug("altered DHCP/ebtables successfully: %s" % cmd)

def validpath(path):
    if not path:
        return False
    if path.find("..") != -1:
        return False
    return True

def persist(conf):
    path = conf.persistencedir + "/" + conf.name
    if not validpath(path):
        raise Exception("path is invalid: %s" % path)

    # cannot persist a file object:
    conf.logfilehandler = None

    try:
        f = None
        try:
            f = open(path, 'w')
            pickle.dump(conf, f)
        except:
            log.exception("problem with persisting call parameters:")
            return False
    finally:
        if f:
            f.close()
    log.debug("persisted conf object to '%s'" % path)
    return True

def depersist(path):
    log.debug("getting object from filesystem: %s" % path)
    if not validpath(path):
        raise Exception("path is invalid: %s" % path)
    try:
        f = None
        try:
            f = open(path, 'r')
            x = pickle.load(f)
            #log.debug(x)
            return x
        except:
            log.exception("problem with de-persisting call parameters:")
            return None
    finally:
        if f:
            f.close()

def persistMAC(conf):
    #log.debug("persisting mactrack object:")
    #log.debug(mactrack)

    # for now, no workspace can be named MACTRACK...
    # todo: create directories for different kinds of persistence objects
    path = conf.persistencedir + "/" + "MACTRACK"
    if not validpath(path):
        raise Exception("path is invalid: %s" % path)
    try:
        f = None
        try:
            f = open(path, 'w')
            pickle.dump(conf.mactrack, f)
        except:
            log.exception("problem with persisting MAC tracking to path='%s':"
                          % path)
            return False
    finally:
        if f:
            f.close()
    log.debug("wrote MAC tracking object to '%s'" % path)
    return True

def depersistMAC(conf):
    #log.debug("getting MAC tracking object from filesystem:")
    path = conf.persistencedir + "/" + "MACTRACK"
    if not validpath(path):
        raise Exception("path is invalid: %s" % path)
    try:
        f = None
        try:
            f = open(path, 'r')
            conf.mactrack = pickle.load(f)
            #log.debug(x)
            log.debug("successfully loaded MAC tracking from %s" % path)
            return True
        except:
            log.debug("problem with de-persisting MAC tracking, creating "
                      "new object")
            conf.mactrack = mac_track()
            if not persistMAC(conf):
                log.exception("fatal, problem creating new MAC tracking obj")
                conf.mactrack = None
                return None
            else:
                return True
    finally:
        if f:
            f.close()

def persistIP(conf):
    #log.debug("persisting iptrack object")
    #log.debug(iptrack)

    # for now, no workspace can be named IPTRACK...
    # todo: create directories for different kinds of persistence objects
    path = conf.persistencedir + "/" + "IPTRACK"
    if not validpath(path):
        raise Exception("path is invalid: %s" % path)
    try:
        f = None
        try:
            f = open(path, 'w')
            pickle.dump(conf.iptrack, f)
        except:
            log.exception("problem with persisting IP tracking to path='%s':" 
                          % path)
            return False
    finally:
        if f:
            f.close()
    log.debug("wrote IP tracking object to '%s'" % path)
    return True

def depersistIP(conf):
    #log.debug("getting IP tracking object from filesystem:")
    path = conf.persistencedir + "/" + "IPTRACK"
    if not validpath(path):
        raise Exception("path is invalid: %s" % path)
    try:
        f = None
        try:
            f = open(path, 'r')
            conf.iptrack = pickle.load(f)
            #log.debug(x)
            log.debug("successfully loaded IP tracking from %s" % path)
            return True
        except:
            log.debug("problem with de-persisting IP tracking, creating new "
                      "object")
            conf.iptrack = ip_track()
            if not persistIP(conf):
                log.exception("fatal, problem creating new IP tracking object")
                conf.iptrack = None
                return None
            else:
                return True
    finally:
        if f:
            f.close()

###########
# globals #
###########

def instance(parameters):
    global _instance
    try:
        _instance
    except:
        _instance = xen_v2_manager(parameters)
    return _instance

def parameters(conffile, action):
    global _parameters
    try:
        _parameters
    except:
        _parameters = xen_v2_manager_config(conffile, action)
    return _parameters

