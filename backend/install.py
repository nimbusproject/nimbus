#!/usr/bin/env python

# Copyright 1999-2006 University of Chicago
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy
# of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

# For more information see: http://workspace.globus.org

import ConfigParser
import fileinput
import grp
import logging
import optparse
import os
import pwd
import shutil
import stat
import sys
from zipfile import PyZipFile

PERM_NOTE="""
sudo is used for altering/securing the system's networking setup for a VM

And for Xen3, it is used for xm and xend.

Corresponding entries must be in the sudoers file, e.g:

  globus ALL=(root) NOPASSWD: /opt/workspace/bin/dhcp-config.sh
  globus ALL=(root) NOPASSWD: /usr/sbin/xm
  globus ALL=(root) NOPASSWD: /usr/sbin/xend

... where globus is the account that workspace-control is run under

For information about sudo, see:
    http://www.gratisoft.us/sudo/
And:
    http://xkcd.com/c149.html

Since we call it via sudo, the default dhcpconfig value
"/opt/workspace/bin/dhcp-config.sh" implies that /opt, /opt/workspace/, 
and /opt/workspace/bin are not writeable by anyone but root. 

"/opt/workspace/bin/dhcp-config.sh" itself should be owned by
root and chmod 700

These are the recommended permissions for the default 
/opt/workspace configurations distributed in the program's
example configuration file:

drwxr-xr-x   root    root     /opt
drwxr-xr-x   root    root     /opt/workspace

/opt/workspace contents:

drwxr-x---   root    globus   bin
drwxr-x---   root    globus   images
drwx------   globus  globus   logs
drwx------   globus  globus   persistence
drwx------   globus  globus   secureimages
-rw-r-----   root    globus   worksp.conf

This installer can accomplish all of this for you.
"""


ZIPHEADER="""#!/bin/sh
exec python -c '
import sys
assert sys.version >= "2.3", sys.argv[1] + ": Python 2.3 or greater required"
sys.path.insert(0, sys.argv[1])
del sys.argv[0]
import worksp
sys.exit(worksp.main())
' "$0" "$@"
exit 1
"""

log = logging.getLogger("worksp")
ch = logging.StreamHandler()
ch.setLevel(logging.DEBUG)

formatter = logging.Formatter("%(levelname)s: %(message)s")

# for timestamp and line no
#formatter = logging.Formatter("%(asctime)s (%(lineno)d) %(levelname)s - %(message)s")

ch.setFormatter(formatter)
log.addHandler(ch)

class StopInstallation(Exception):
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg

class StopInstallationError(Exception):
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg
        
class NoValue(Exception):
    pass
        
def confValResolve(section, key, config, family):
    """Resolves values from overriding sections of configuration file"""

    result = None

    # First try family
    if family:
        try:
            result = config.get(section+"-"+family,key)
            return result
        except:
            pass
    # If not in family specific section, try regular section
    try:
        result = config.get(section,key)
        return result
    except:
        raise NoValue()

def curry(func, *args, **kwds):
    def callit(*moreargs, **morekwds):
        kw = kwds.copy()
        kw.update(morekwds)
        return func(*(moreargs+args), **kw)
    return callit

def initGetVal(conf):
    return curry(confValResolve, conf, conf.family)
    
def userBooleanQ(querystring, default="Y"):
    resp = None
    if default == "Y":
        tag = " [Y/n]: "
    else:
        tag = " [y/N]: "
    while (resp == None):
        resp = raw_input(querystring + tag)
        if resp == None:
            continue
        if resp == "":
            if default == "Y":
                return True
            else:
                return False

        resp = resp.strip().lower()
        if (resp[0] == 'y'):
            return True
        elif (resp[0] == 'n'):
            return False
        else:
            print "(invalid response)"
            resp = None

def userStringQ(querystring):
    return raw_input(querystring)
    
def userDirChangeQ(path, mode, userstr, groupstr, default="Y"):
    q = "\nAlter '%s' to %s?" % (path, modeStr(mode))
    return userBooleanQ(q, default)
    
def modeStr(mode):
    string=""
    mode=stat.S_IMODE(mode)
    for i in ("USR", "GRP", "OTH"):
        for perm in "R", "W", "X":
            if mode & getattr(stat, "S_I"+ perm + i):
                string = string + perm.lower()
            else:
                string = string + "-"
    return string
    
def mode700(mode):
    mode = stat.S_IMODE(mode)
    if not mode & stat.S_IRWXU:
        return False
    for i in ("GRP", "OTH"):
        for perm in "R", "W", "X":
            if mode & getattr(stat, "S_I"+ perm + i):
                return False
    return True
    
def mode750(mode):
    mode = stat.S_IMODE(mode)
    for perm in (stat.S_IRWXU, stat.S_IRGRP, stat.S_IXGRP):
        if not mode & perm:
            return False
    for perm in (stat.S_IWGRP, stat.S_IROTH, stat.S_IWOTH, stat.S_IXOTH):
        if mode & perm:
            return False
    return True
    
def checkUpDir(path, opts, firstdirid=0):
    """checks if root is the only one with write permissions up to
       the / directory.  assumes all paths are absolute.  bottom
       directory in path can be owned by an alternate uid if needed,
       will try to change the bottom dirs to restricted permissions"""
       
    uidx = stat.ST_UID
    gidx = stat.ST_GID
    midx = stat.ST_MODE
    
    if firstdirid:
        log.debug("checking '%s', last child dir should be non-root" % path)
    else:
        log.debug("checking '%s'" % path)
    
    # not guaranteed to be 'root'
    pwentry = pwd.getpwuid(0)
    uid0str = pwentry[0]
        
    done = False
    first = True
    bottomdir = True
    while not done:
        
        if not first:
            (path, old) = os.path.split(path)
        else:
            first = False
    
        try:
            en = os.stat(path)
            skip_non_existant = False
        except:
            if opts.onlyverify:
                log.info("Would verify permissions of '%s' but it doesn't exist" % path)
                skip_non_existant = True
            else:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                name = str(exceptname)
                err = str(sys.exc_value)
                errmsg = "Problem stat-ing '%s': %s: %s\n" % (path, name, err)
                log.error(errmsg)
                raise StopInstallationError(errmsg)
    
        if path == '/':
            done = True
        
        if skip_non_existant:
            # if we're in onlyverify try to get as much information as possible
            if bottomdir:
                bottomdir = False
            continue

        log.debug("Perms on '%s': %d:%d %s"
                  % (path, en[uidx], en[gidx], modeStr(en[midx])))
                  
        if bottomdir:
            chownid = 0
            chownuserstr = uid0str
            chowngroup = opts.groupid
            chowngroupstr = opts.group
            
            if firstdirid != 0:
                chownid = opts.accountid
                chownuserstr = opts.account
                
            if en[uidx] != chownid or en[gidx] != chowngroup:
                
                pwentry = pwd.getpwuid(en[uidx])
                currchownuserstr = pwentry[0]
                grpentry = grp.getgrgid(en[gidx])
                currchowngrpstr = grpentry[0]
                
                msg = "directory ownership of '%s' " % path
                msg += "is %d:%d " % (en[uidx], en[gidx])
                msg += "(%s:%s) " % (currchownuserstr, currchowngrpstr)
                msg += " but should be %d:%d " % (chownid, chowngroup)
                msg += "(%s:%s)" % (chownuserstr, chowngroupstr)
                
                log.debug(msg)
            
                change = False
                
                changestr = "owner     '%s' changed to %s:%s" \
                            % (path, chownuserstr, opts.group)
                
                if opts.install:
                    msg += "\nDo you want to change the ownership?"
                    print ""
                    if not userBooleanQ(msg):
                        raise StopInstallation("")
                    else:
                        change = True
                        
                elif opts.onlyverify:
                    log.info("Would have changed " + changestr)
                    
                elif opts.noninteractive or opts.exe:
                    change = True
                    
                try:
                    if change:
                        os.chown(path, chownid, chowngroup)
                    
                        # could have been a race
                        en = os.stat(path)
                        if en[uidx] != chownid or en[gidx] != chowngroup:
                            raise StopInstallationError("failed to chown"
                                              "directory '%s' to %d:%d" 
                                              % (path, chownid, chowngroup))
                        else:
                            log.info("changed " + changestr)
                    
                except:
                    exception_type = sys.exc_type
                    try:
                        exceptname = exception_type.__name__ 
                    except AttributeError:
                        exceptname = exception_type
                    name = str(exceptname)
                    err = str(sys.exc_value)
                    errmsg = "Problem chown-ing '%s': %s: %s\n" % (path, name, err)
                    log.error(errmsg)
                    raise StopInstallationError(errmsg)
            else:
                log.debug("OK: '%s' owned by %s:%s" 
                         % (path, chownuserstr, chowngroupstr))
                
        targetmode = None
        targetstr = None

        en = os.stat(path)
        mode = stat.S_IMODE(en[midx])
        pwentry = pwd.getpwuid(en[uidx])
        currnam = pwentry[0]
        grentry = grp.getgrgid(en[gidx])
        currgrp = grentry[0]
        worldrx = False
        
        if bottomdir and firstdirid != 0:
            if not mode700(en[midx]):
                targetmode = stat.S_IRWXU
                log.debug("directory '%s' could be %s but it is %s"
                          % (path, modeStr(targetmode), modeStr(en[midx])))
        elif bottomdir:
            if not mode750(en[midx]):
                targetmode = stat.S_IRWXU | stat.S_IRGRP | stat.S_IXGRP
                log.debug("directory '%s' should be %s but it is %s"
                          % (path, modeStr(targetmode), modeStr(en[midx])))
        else:
            # if not bottomdir we should only check if root is owner and
            # only writer and if privileged group can R/X. these dirs could
            # be anywhere.
            # if the user changes the configurations from the defaults of
            # /opt/workspace/*, so should not automate changes
            #
            # not checking for the sticky bit, sane /tmp directories
            # would not pass these simple tests (admin knows best)
            
            if en[uidx] != 0 or mode & stat.S_IWGRP or mode & stat.S_IWOTH:
                raise StopInstallation("'%s' has wrong permissions, all parent"
                " directories of the workspace control directories should "
                "only be owned and writeable by %s.  Permissions: %s:%s %s" 
                % (path, uid0str, currnam, currgrp, modeStr(en[midx])))
                
            elif mode & stat.S_IROTH and mode & stat.S_IXOTH:
                worldrx = True
                
            elif en[gidx] == opts.groupid and not mode & stat.S_IRGRP and not mode & stat.S_IXGRP:
                raise StopInstallation("'%s' has wrong permissions, if the "
                "parent directories of workspace control directories are "
                "owned by the given privileged group, they should be read and "
                "executable by the group if not world R+X or there is no "
                "way to access them" % path)
                
            elif not mode & stat.S_IRGRP or not mode & stat.S_IXGRP:
                raise StopInstallation("'%s' has wrong permissions, the "
                "parent directories of workspace control directories "
                "should be R+X by at least some group or world R+X "
                "or there is no way to access them.  Permissions: "
                "%s:%s %s" % (path, uid0str, currgrp, modeStr(en[midx])))
                
            elif en[gidx] != opts.groupid:
                log.warn("------------ WARNING: ---------------")
                log.warn("directory could be OK: '%s' is not world or group "
                "writeable (good) but it is not owned by given, "
                "privileged group. Confirm if OK.  Permissions: " 
                "%s:%s %s" % (path, uid0str, currgrp, modeStr(en[midx])))
                
        change = False
        
        if targetmode != None and opts.install:
            print ""
            if not userDirChangeQ(path, targetmode, opts.account, opts.group):
                raise StopInstallation("'%s' has too open permissions" % path)
            else:
                log.debug("user responds yes, changing '%s' permission from "
                         "%s to %s" 
                         % (path, modeStr(en[midx]), modeStr(targetmode)))
                change = True
                
        if targetmode != None and opts.onlyverify:
            log.info("Would have changed '%s' permissions from %s to %s" 
                    % (path, modeStr(en[midx]), modeStr(targetmode)))
            
        if targetmode != None and opts.noninteractive:
            log.debug("changing '%s' permission from %s to %s" 
                     % (path, modeStr(en[midx]), modeStr(targetmode)))
            change = True
            
        if targetmode != None and opts.exe:
            log.debug("changing '%s' permission from %s to %s" 
                     % (path, modeStr(en[midx]), modeStr(targetmode)))
            change = True
            
        if change:
            try:
                os.chmod(path, targetmode)
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                name = str(exceptname)
                err = str(sys.exc_value)
                errmsg = "Problem chmod-ing '%s': %s: %s\n" % (path, name, err)
                log.error(errmsg)
                raise StopInstallationError(errmsg)
                
            en2 = os.stat(path)
            errmsg = "Failed to modify '%s' to %s" % (path, modeStr(targetmode))
            if bottomdir and firstdirid != 0:
                if not mode700(en2[midx]):
                    raise StopInstallationError(errmsg)
            elif bottomdir:
                if not mode750(en2[midx]):
                    raise StopInstallationError(errmsg)
            
            log.info("modified perms    '%s' from %s to %s" 
                    % (path, modeStr(en[midx]), modeStr(targetmode)))
        
        elif targetmode == None:
            if not worldrx:
                log.info("OK:               '%s'" % path)
            else:
                log.info("OK: (world R+X)   '%s'" % path)
                
            log.debug("OK: owned by %s:%s, %s:  '%s'" % (currnam, currgrp, modeStr(en[midx]),path))

        if bottomdir:
            bottomdir = False

def getConf(confsection, confname, opts, conf, isdir=True):
    try:
        path = conf.getVal(confsection, confname)
        log.debug("found %s.%s configuration: '%s'" 
                    % (confsection, confname, path))
    except NoValue:
        raise StopInstallationError("conf file '%s' contains no %s.%s value" % (opts.conffile_abspath, confsection, confname))

    if not os.path.isabs(path):
        raise StopInstallationError("%s.%s value '%s' is not an absolute path" % (confsection, confname, path))
        
    path = os.path.realpath(path)
    
    original = None
    afile = None
    if not isdir:
        original = path
        (path, afile) = os.path.split(path)

    if os.path.exists(path):
        if isdir:
            log.debug("%s: '%s' exists" % (confname, path))
        else:
            log.debug("%s: '%s' exists (parent directory of file '%s')" % (confname, path, afile))
    else:
        change = False
        if opts.install:
            if not userBooleanQ("%s '%s' doesn't exist, create it and "
                            "any missing parents?" % (confname, path)):
                raise StopInstallation("%s configuration missing: '%s'" 
                                        % (confname, path))
            else:
                change = True
                
        elif opts.onlyverify:
            log.info("Would have created %s directory '%s' and missing parents" % (confname, path))
                
        elif opts.noninteractive or opts.exe:
            change = True
                
        if change:
            os.makedirs(path, 0755)
            log.info("created directory '%s' (%s)" % (path, confname))

    if isdir:
        if not opts.onlyverify and not os.path.isdir(path):
            raise StopInstallationError("%s value '%s' is not a directory" 
                                            % (confname,path))
    if isdir:
        return path
    else:
        return original


# ----------------------------- TASKS ----------------------------- #

class task:
    def run(self, opts, conf):
        pass

# -----------    0: greeting    ----------- #

class greeting(task):
    def run(self, opts, conf):
        print "\nThis is the Workspace Service TP2.2 backend installer."
        print "For more information, see http://workspace.globus.org/vm/\n"
        if opts.noninteractive:
            print """NON-INTERACTIVE MODE:

You chose non-interactive mode which is best used with a configuration
file that is known to be working (for batch install mode).  This mode will
not verify anything interactively, it will make whatever changes that are
necessary (but will still fail in cases where the issue is not sane to
automate).

"""

        elif opts.onlyverify:
            print """ONLY-VERIFY MODE:

You chose the only-verify mode which prints what the non-interactive mode
would have done -- it will not actually change anything.
"""
        elif opts.install:
            print """
You've chosen the blocking installation method that will ask you questions
if necessary.  

You can choose the capitalized default answer by pressing enter.

Example question:
"""
            return userBooleanQ("    Do you want to continue?")
        return True
        
# -----------     1: getconf    ----------- #

class getconf(task):
    def run(self, opts, conf):
        if not os.path.exists(opts.conffile):
            raise StopInstallationError(
                "'%s' does not exist on the filesystem" % opts.conffile)

        opts.conffile_abspath = os.path.abspath(opts.conffile)
        
        (path, afile) = os.path.split(opts.conffile_abspath)
        checkUpDir(path, opts)
        
        targetmode = stat.S_IRUSR | stat.S_IWUSR | stat.S_IRGRP
        
        if opts.onlyverify:
            log.info("Would have checked/changed permissions on '%s'" % opts.conffile_abspath)
        else:
            try:
                os.chown(opts.conffile_abspath, 0, opts.groupid)
                os.chmod(opts.conffile_abspath, targetmode)
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                name = str(exceptname)
                err = str(sys.exc_value)
                errmsg = "Problem changing permissions of '%s': %s: %s\n" \
                         % (opts.conffile_abspath, name, err)
                log.error(errmsg)
                raise StopInstallationError(errmsg)
            
        en = os.stat(opts.conffile_abspath)
            
        pwentry = pwd.getpwuid(en[stat.ST_UID])
        userstr = pwentry[0]
        grpentry = grp.getgrgid(en[stat.ST_GID])
        grpstr = grpentry[0]
            
        log.info("Permissions of '%s': %s:%s %s"
              % (opts.conffile_abspath, userstr, grpstr, modeStr(en[stat.ST_MODE])))
            
        log.info("Loading %s" % opts.conffile_abspath)

        conf = ConfigParser.ConfigParser()
        conf.read(opts.conffile)

        # Family?
        try:
            conf.family = conf.get("general","family")
            log.debug("found family = %s" % conf.family)
        except:
            conf.family = None
            log.debug("no value given for conf family")

        conf.getVal = initGetVal(conf)
        return conf

# -----------     2: logdir      ----------- #

class logdir(task):
    def run(self, opts, conf):
        logdirpath = getConf("general", "logfiledir", opts, conf)
        checkUpDir(logdirpath, opts, opts.accountid)
        opts.logdirpath = logdirpath
        
# -----------  3: mountdirpath  ----------- #

class mountdir(task):
    def run(self, opts, conf):
        try:
            mountdirpath = getConf("systempaths", "mountdir", opts, conf)
        except StopInstallationError:
            # this is OK now
            opts.mountdirpath = None
            log.debug("no mountdirpath configuration (ok as of tp1.2.2)")
        else:
            checkUpDir(mountdirpath, opts, opts.accountid)
            opts.mountdirpath = mountdirpath
        
# ----------- 4: persistencedir ----------- #

class persistencedir(task):
    def run(self, opts, conf):
        persistdirpath = getConf("persistence", "persistencedir", opts, conf)
        checkUpDir(persistdirpath, opts, opts.accountid)
        opts.persistdirpath = persistdirpath
        
# ----------- 5: securelocaldir ----------- #

class securelocaldir(task):
    def run(self, opts, conf):
        securelocaldirpath = getConf("images", "securelocaldir", opts, conf)
        checkUpDir(securelocaldirpath, opts, opts.accountid)
        opts.securelocaldirpath = securelocaldirpath
        
# -----------    6: tmpdir      ----------- #

class tmpdir(task):
    def run(self, opts, conf):
        try:
            tmpdirpath = getConf("systempaths", "tmpdir", opts, conf)
        except StopInstallationError:
            # this is OK now
            opts.tmpdirpath = None
            log.debug("no tmpdirpath configuration (ok as of tp1.2.2)")
        else:
            checkUpDir(tmpdirpath, opts, opts.accountid)
            opts.tmpdirpath = tmpdirpath
        
# -----------    7: localdir    ----------- #

class localdir(task):
    """root should own this directory (and its contents...) so there is
       no chance that read-only kernels or filesystems are over-written"""
    def run(self, opts, conf):
        localdirpath = getConf("images", "localdir", opts, conf)
        checkUpDir(localdirpath, opts)
        opts.localdirpath = localdirpath
        
# -----------    8: mounttool   ----------- #

class mounttool(task):
    def run(self, opts, conf):
        try:
            mounttoolpath = getConf("systempaths", "mounttool", opts, conf, False)
        except StopInstallationError:
            # this is OK now
            opts.mounttoolpath = None
            log.debug("no mounttool configuration (ok as of tp1.2.2)")
            return
            
        log.debug("found systempaths.mounttool configuration: '%s'" 
                  % mounttoolpath)
                  
        (path, afile) = os.path.split(mounttoolpath)
        checkUpDir(path, opts)
        
        opts.mounttoolpath = mounttoolpath
        
        if not opts.mountdirpath or not opts.tmpdirpath:
            raise StopInstallationError("systempaths.mounttoolpath "
                        "configuration requires both systempaths.mountdir and "
                        "systempaths.tmpdir configurations")
                  
        if opts.onlyverify:
            log.info("Would have installed %s" % mounttoolpath)
            log.info("Would have replaced  %s IMAGE_DIR setting with %s "
                     % (mounttoolpath, opts.securelocaldirpath))
            log.info("Would have replaced  %s MOUNTPOINT_DIR setting with %s "
                     % (mounttoolpath, opts.mountdirpath))
            log.info("Would have replaced  %s FILE_DIR setting with %s "
                     % (mounttoolpath, opts.tmpdirpath))
            return
            
        binpath = "mount-alter.sh"
        
        if not os.path.exists(binpath):
            raise StopInstallationError("%s doesn't exist?  you are probably "
            "running this installer from a different directory than the "
            "workspace-control source directory" % binpath)
        
        try:
            shutil.copyfile(binpath, mounttoolpath)
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            name = str(exceptname)
            err = str(sys.exc_value)
            errmsg = "Problem creating '%s': %s: %s\n" \
                     % (mounttoolpath, name, err)
            log.error(errmsg)
            raise StopInstallationError(errmsg)
        
        try:
            os.chown(mounttoolpath, 0, os.getgid())
            os.chmod(mounttoolpath, stat.S_IRWXU)
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            name = str(exceptname)
            err = str(sys.exc_value)
            errmsg = "Problem changing permissions '%s': %s: %s\n" \
                     % (mounttoolpath, name, err)
            log.error(errmsg)
            raise StopInstallationError(errmsg)
    
        log.info("Created '%s' and changed its permissions" % mounttoolpath)
        
        while opts.securelocaldirpath[-1] == '/':
            opts.securelocaldirpath = opts.securelocaldirpath[:-1]
        while opts.mountdirpath[-1] == '/':
            opts.mountdirpath = opts.mountdirpath[:-1]
        while opts.tmpdirpath[-1] == '/':
            opts.tmpdirpath = opts.tmpdirpath[:-1]
        
        foundsecdir = False
        foundmntdir = False
        foundtmpdir = False
        # replace authorized directories to match the given conf file settings
        try:
            for line in fileinput.input(mounttoolpath, inplace=1, bufsize=2000):
                if "IMAGE_DIR=" in line:
                    print "IMAGE_DIR='%s/'" % opts.securelocaldirpath
                    foundsecdir = True
                elif "MOUNTPOINT_DIR=" in line:
                    print "MOUNTPOINT_DIR='%s/'" % opts.mountdirpath
                    foundmntdir = True
                elif "FILE_DIR=" in line:
                    print "FILE_DIR='%s/'" % opts.tmpdirpath     
                    foundtmpdir = True
                else:
                    print line,
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            name = str(exceptname)
            err = str(sys.exc_value)
            errmsg = "Problem changing '%s': %s: %s\n" \
                     % (binpath, name, err)
            log.error(errmsg)
            raise StopInstallationError(errmsg)
            
        if foundsecdir:
            log.warn("Set mount-alter.sh IMAGE_DIR check to '%s'" % opts.securelocaldirpath)
        else:
            log.warn("Did not set mount-alter.sh IMAGE_DIR")
            
        if foundmntdir:
            log.warn("Set mount-alter.sh MOUNTPOINT_DIR check to '%s'" % opts.mountdirpath)
        else:
            log.warn("Did not set mount-alter.sh MOUNTPOINT_DIR")
            
        if foundtmpdir:
            log.warn("Set mount-alter.sh FILE_DIR check to '%s'" % opts.tmpdirpath)
        else:
            log.warn("Did not set mount-alter.sh FILE_DIR")
            
# -----------    9: dhcpconfig   ----------- #

class dhcpconfig(task):
    def run(self, opts, conf):
        dhcpconfigpath = getConf("systempaths", "dhcpconfig", opts, conf, False)
        log.debug("found systempaths.dhcpconfig configuration: '%s'" 
                  % dhcpconfig)
                  
        (dhcpconfigdir, afile) = os.path.split(dhcpconfigpath)
        checkUpDir(dhcpconfigdir, opts)
        
        opts.dhcpconfig = dhcpconfigpath
        
        binpaths = ["dhcp-config.sh", "ebtables-config.sh", "dhcp-conf-alter.py"]
        
        for binpath in binpaths:
            if not os.path.exists(binpath):
                raise StopInstallationError("%s doesn't exist?  you are "
                "probably running this installer from a different directory "
                "than the workspace-control source directory" % binpath)
                
            # for now, assume user doesn't change script name in conf
            target = os.path.join(dhcpconfigdir, binpath)
            
            if opts.onlyverify:
                log.info("Would have created '%s' and changed its permissions" % target)
                continue
            
            try:
                shutil.copyfile(binpath, target)
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                name = str(exceptname)
                err = str(sys.exc_value)
                errmsg = "Problem creating '%s': %s: %s\n" \
                         % (target, name, err)
                log.error(errmsg)
                raise StopInstallationError(errmsg)
            
            try:
                os.chown(target, 0, os.getgid())
                os.chmod(target, stat.S_IRWXU)
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                name = str(exceptname)
                err = str(sys.exc_value)
                errmsg = "Problem changing permissions '%s': %s: %s\n" \
                         % (target, name, err)
                log.error(errmsg)
                raise StopInstallationError(errmsg)
        
            log.info("Created '%s' and changed its permissions" % target)
        
            
# -----------    10: user scripts   ----------- #

class userscripts(task):
    def run(self, opts, conf):
        blankcreatepath = getConf("systempaths", "blankcreate", opts, conf, False)
        log.debug("found systempaths.blankcreate configuration: '%s'" 
                  % blankcreatepath)
                  
        (blankcreatedir, bin) = os.path.split(blankcreatepath)
        checkUpDir(blankcreatedir, opts)
        
        opts.blankcreate = blankcreatepath
    
        if not os.path.exists(bin):
            log.debug("'%s' doesn't exist in the workspace-control source directory" % bin)
            if os.path.exists(blankcreatepath):
                return
            else:
                raise StopInstallationError("'%s' doesn't exist on the filesystem (and '%s' wasn't in workspace-control source directory" % (blankcreatepath, bin))
            
        target = os.path.join(blankcreatedir, bin)
        
        if opts.onlyverify:
            log.info("Would have created '%s' and changed its permissions" % target)
            return

        try:
            shutil.copyfile(bin, target)
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            name = str(exceptname)
            err = str(sys.exc_value)
            errmsg = "Problem creating '%s': %s: %s\n" \
                     % (target, name, err)
            log.error(errmsg)
            raise StopInstallationError(errmsg)
        
            
        targetmode = stat.S_IRWXU | stat.S_IRGRP | stat.S_IXGRP
                
        try:
            os.chown(target, 0, opts.groupid)
            os.chmod(target, targetmode)
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            name = str(exceptname)
            err = str(sys.exc_value)
            errmsg = "Problem changing permissions '%s': %s: %s\n" \
                     % (target, name, err)
            log.error(errmsg)
            raise StopInstallationError(errmsg)
    
        log.info("Created '%s' and changed its permissions" % target)
            
# -----------    11: thisexe     ----------- #

class thisexe(task):
    def run(self, opts, conf):
        exepath = getConf("systempaths", "thisexe", opts, conf, False)
        log.debug("found systempaths.thisexe configuration: '%s'" 
                  % exepath)
                  
        (path, afile) = os.path.split(exepath)
        checkUpDir(path, opts)
                  
        opts.exepath = exepath
        
        if opts.onlyverify:
            log.info("Would have installed %s" % exepath)
            log.info("Would have replaced  %s default conf "
                     "file setting with %s" % (exepath, opts.conffile_abspath))
            return
        
        f = None
        try:
            try:
                # touch the file or replace what was there
                f = open(exepath, 'w')
                f.close()
                f = None
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                name = str(exceptname)
                err = str(sys.exc_value)
                errmsg = "Problem creating '%s': %s: %s\n" \
                         % (exepath, name, err)
                log.error(errmsg)
                raise StopInstallationError(errmsg)
        finally:
            if f:
                f.close()
                
        targetmode = stat.S_IRWXU | stat.S_IRGRP | stat.S_IXGRP
                
        try:
            os.chown(exepath, 0, opts.groupid)
            os.chmod(exepath, targetmode)
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            name = str(exceptname)
            err = str(sys.exc_value)
            errmsg = "Problem changing permissions '%s': %s: %s\n" \
                     % (exepath, name, err)
            log.error(errmsg)
            raise StopInstallationError(errmsg)
    
        log.info("Touched '%s' and changed its permissions" % exepath)
        
        binpath = "worksp.py"
        packagepath = "./workspace"
        
        if not os.path.exists(binpath):
            raise StopInstallationError("%s doesn't exist?  you are probably "
            "running this installer from a different directory than the "
            "workspace-control source directory" % binpath)
            
        if not os.path.exists(packagepath):
            raise StopInstallationError("%s doesn't exist?  you are probably "
            "running this installer from a different directory than the "
            "workspace-control source directory" % packagepath)
            
        if not os.path.isdir(packagepath):
            raise StopInstallationError("%s isn't a directory?  you are "
            "probably running this installer from a different directory than "
            "the workspace-control source directory" % packagepath)
            
        # replace default conf file with -c parameter to this installer.
        # This way the program's caller does not have to pass the conf
        # file parameter every time the program is called
        founddefconf = False
        try:
            for line in fileinput.input(binpath, inplace=1, bufsize=2000):
                if "DEFAULTCONF=" in line:
                    print 'DEFAULTCONF="%s"' % opts.conffile_abspath
                    founddefconf = True
                else:
                    print line,
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            name = str(exceptname)
            err = str(sys.exc_value)
            errmsg = "Problem changing '%s': %s: %s\n" \
                     % (binpath, name, err)
            log.error(errmsg)
            raise StopInstallationError(errmsg)
        
        if founddefconf:
            log.warn("Replaced exe's default conf file with '%s'" % opts.conffile_abspath)
        else:
            log.warn("Did not find exe's default conf file setting, so did not change")
        
        f = None
        try:
            try:
                f = open(exepath, 'w')
                f.write(ZIPHEADER)
                f.close()
                f = None
                log.debug("wrote zipheader to '%s'" % exepath)
                f = PyZipFile(exepath, 'a')
                f.writepy(binpath)
                f.writepy(packagepath)
                dbg = ""
                for info in f.infolist():
                    dbg += info.filename + "\n"
                log.debug("zip contents now:\n%s" % dbg)
                f.close()
                f = None
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                name = str(exceptname)
                err = str(sys.exc_value)
                errmsg = "Problem creating '%s': %s: %s\n" \
                         % (exepath, name, err)
                log.error(errmsg)
                raise StopInstallationError(errmsg)
        finally:
            if f:
                f.close()
                
        log.info("Compiled modules into '%s'" % exepath)
        
        
# -----------      12: sudo       ----------- #

class sudo(task):
    def run(self, opts, conf):
        sudopath = getConf("systempaths", "sudo", opts, conf, False)
        log.debug("found systempaths.sudo configuration: '%s'" 
                  % sudopath)
                  
        if opts.onlyverify:
            log.info("Would have checked out %s" % sudopath)
        
        if not os.path.isabs(sudopath):
            raise StopInstallationError("systempaths.sudo value '%s' is not an absolute path" % sudopath)
            
        opts.sudopath = os.path.realpath(sudopath)
        
            
        xmpath = getConf("xenpaths", "xm", opts, conf, False)
        log.debug("found xenpaths.xm configuration: '%s'" 
                  % sudopath)
                  
        if opts.onlyverify:
            log.info("Would have checked out %s" % xmpath)
            
        if not os.path.isabs(xmpath):
            raise StopInstallationError("xenpaths.xm value '%s' is not an absolute path" % opts.xmpath)
            
        opts.xmpath = os.path.realpath(xmpath)
            
            
        xendpath = getConf("xenpaths", "xend", opts, conf, False)
        log.debug("found xenpaths.xend configuration: '%s'" 
                  % xendpath)
                  
        if opts.onlyverify:
            log.info("Would have checked out %s" % xendpath)
            
        if not os.path.isabs(xendpath):
            raise StopInstallationError("xenpaths.xend value '%s' is not an absolute path" % opts.xendpath)
            
        opts.xendpath = os.path.realpath(xendpath)
            
        pwentry = pwd.getpwuid(0)
        uid0str = pwentry[0]
        
        
        for binpath in [opts.sudopath, opts.xmpath, opts.xendpath]:
                
            log.info("Checking          '%s'" % binpath)
            
            path = binpath
            done = False
            while not done:
                (path, old) = os.path.split(path)
            
                try:
                    en = os.stat(path)
                except:
                    exception_type = sys.exc_type
                    try:
                        exceptname = exception_type.__name__ 
                    except AttributeError:
                        exceptname = exception_type
                    name = str(exceptname)
                    err = str(sys.exc_value)
                    errmsg = "Problem stat-ing '%s': %s: %s\n" % (path, name, err)
                    log.error(errmsg)
                    raise StopInstallationError(errmsg)
            
                if path == '/':
                    done = True
            
                en = os.stat(path)
                mode = stat.S_IMODE(en[stat.ST_MODE])
                pwentry = pwd.getpwuid(en[stat.ST_UID])
                currnam = pwentry[0]
                grentry = grp.getgrgid(en[stat.ST_GID])
                currgrp = grentry[0]
                
                if en[stat.ST_UID] != 0 or mode & stat.S_IWGRP or mode & stat.S_IWOTH:
                    raise StopInstallation("'%s' has wrong permissions, all parent"
                    " directories should only be owned and writeable by %s. " "Permissions: %s:%s %s" 
                    % (path, uid0str, currnam, currgrp, modeStr(en[stat.ST_MODE])))
                else:
                    log.info("OK:               '%s'" % path)
                
                    
        sudoconfig = ""
        if opts.mounttoolpath:
            sudoconfig = opts.account + " ALL=(root) NOPASSWD: " + opts.mounttoolpath + '\n'
        sudoconfig += opts.account + " ALL=(root) NOPASSWD: " + opts.dhcpconfig + '\n'
        sudoconfig += opts.account + " ALL=(root) NOPASSWD: " + opts.xmpath + '\n'
        sudoconfig += opts.account + " ALL=(root) NOPASSWD: " + opts.xendpath + '\n'

        log.warn("\n\n *** Installation is NOT complete yet. ***\n\nFirst, "
        "make sure the authorized kernel(s) exist where listed in the conf "
        "file.\n\nThen, you must manually add these policies to your sudo "
        "config (use visudo):\n\n%s" % sudoconfig)
        
        log.warn("\n\nAlso, copy 'dhcpd.conf.example' to "
        "'/etc/dhcpd.conf' (if you need to change\nwhere this is "
        "located edit the DHCPD_CONF setting in '%s').\n\nOnce copied, "
        "you must alter the blank subnet settings in it to match the "
        "available\nsubnets for workspaces (examples included).\n" 
        % opts.dhcpconfig)
        
        fstdin, fstdout, fstderr = os.popen3("dirname .")
        fstdin.close()
        stdout = fstdout.read()
        fstdout.close()
        stderr = fstderr.read()
        fstderr.close()
        
        log.debug("stderr: " + stderr)
        
        if stderr:
            log.warn("dirname problem: %s" % stderr)
            log.warn("If dirname is not installed, hardcode paths to EBTABLES_CONFIG\nand DHCP_CONF_ALTER in '%s'" % opts.dhcpconfig)
        
        fstdin, fstdout, fstderr = os.popen3("ebtables")
        fstdin.close()
        stdout = fstdout.read()
        fstdout.close()
        stderr = fstderr.read()
        fstderr.close()
        
        log.debug("stderr: " + stderr)
        
        if stderr:
            log.warn("ebtables problem: %s" % stderr)
            log.warn("ebtables is required, see documentation")

# ----------------------------- SETUP ----------------------------- #

def setup():
    ver="TP2.2 backend: %prog\nhttp://workspace.globus.org/vm/"
    parser = optparse.OptionParser(version=ver)

    parser.add_option("-q", "--quiet",
                  action="store_true", dest="quiet", default=False,
                  help="don't print any messages (unless error occurs).")

    parser.add_option("-v", "--verbose",
                  action="store_true", dest="verbose", default=False,
                  help="print the most messages")

    parser.add_option("-p", "--permissionsnote",
                  action="store_true", dest="permissionsnote", default=False,
                  help="print the unix permissions note that can also found"
                  " in the middle of the sample worksp.conf.example")

    group = optparse.OptionGroup(parser, "Required action",
                 "One of these actions is required:")

    group.add_option("-n", "--noninteractive",
                  action="store_true", dest="noninteractive", default=False,
                  help="Don't ask the user anything (for automated install "
                       "with a well-known conf file).")

    group.add_option("-o", "--onlyverify",
                  action="store_true", dest="onlyverify", default=False,
                  help="Just run the setup tests and print what would have "
                  "happened in --noninteractive mode.  Good option to try "
                  "first since it will do nothing to the filesystem.")

    group.add_option("-i", "--install", action="store_true", dest="install",
                  default=False, help="Install the program, make needed "
                  "directories, and set them with default permissions. "
                  "Will block to ask you questions if necessary.")
                  
    group.add_option("-e", "--exe", action="store_true", dest="exe",
                  default=False, help="Just install the workspace-control "
                  "program to the path listed in the 'thisexe' configuration "
                  "of your conf file.")

    parser.add_option_group(group)

    group = optparse.OptionGroup(parser, "Required arguments",
                 "These arguments are required for --noninteractive, "
                 "--onlyverify, --install, and --exe")

    group.add_option("-c", "--conf", dest="conffile",
                  help="Configuration file to use with the control program. "
                       "This installer reads in its values, validates the "
                       "configuration, makes necessary directories, adjusts "
                       "permissions, and installs the control program.",
                  metavar="FILE")

    group.add_option("-a", "--account", dest="account", metavar="ACCOUNT",
                  help="The name of the privileged account besides root that "
                       "will run the workspace-control program.  This is "
                       "used to create and/or verify good directory "
                       "permissions.")

    group.add_option("-g", "--group", dest="group", metavar="GROUP",
                  help="The name of the unix group that the account "
                       "specified with --account shares with root. "
                       "This allows the specified non-root account, and "
                       "only that account to run read-only programs and "
                       "read read-only files.  For more permissions info, "
                       "re-run this program with the -p flag or view the "
                       "equivalent long comment in the sample worksp.conf.")
                  
    parser.add_option_group(group)

    return parser

# ----------------------------- RUN ----------------------------- #

def start(argv=None):

    parser = setup()

    if argv:
        (opts, args) = parser.parse_args(argv[1:])
    else:
        (opts, args) = parser.parse_args()

    if opts.permissionsnote:
        sys.exit(PERM_NOTE)

    if not opts.conffile:
        sys.exit("configuration file (-c) required, see help (-h)")

    actions = [opts.noninteractive, opts.onlyverify, opts.install, opts.exe]
        
    count = 0
    for action in actions:
        if action:
            count += 1

    if not count:
        sys.exit("You must supply an action, see help (-h).")

    if count > 1:
        sys.exit("You may only supply one action, see help (-h).")

    if not os.geteuid()==0:
        sys.exit("Only root should run the installer.")

    if not opts.account:
        sys.exit("lesser privileged account (-a) required, see help (-h)")

    if not opts.group:
        sys.exit("privileged group (-g) required, see help (-h)")
        
    if opts.verbose:
        log.setLevel(logging.DEBUG)
    elif opts.quiet:
        log.setLevel(logging.ERROR)
    else:
        log.setLevel(logging.INFO)
        
    try:
        pwentry = pwd.getpwnam(opts.account)
    except:
        sys.exit("Problem: user '%s' is not found on system" % opts.account)
    opts.account = pwentry[0]
    opts.accountid = pwentry[2]
    log.info("target non-root account: name '%s', id=%s" % (opts.account, opts.accountid))
    
    try:
        grpentry = grp.getgrnam(opts.group)
    except:
        sys.exit("Problem: group '%s' is not found on system" % opts.group)
    opts.group = grpentry[0]
    opts.groupid = grpentry[2]
    log.info("target privileged group: name '%s', id=%s" % (opts.group, opts.groupid))
    
    # First check if it's primary group
    if opts.groupid == pwentry[2]:
        log.info("group '%s' is primary group of account '%s'" % (opts.group, opts.account))
    elif not opts.account in grpentry[3]:
        log.warn("user '%s' may not be member of group '%s'" % (opts.account, opts.group))

    if not opts.quiet:
        if not greeting().run(opts, None):
            return 0

    try:
        conf = getconf().run(opts, None)
        if conf == None:
            raise StopInstallationError("no parsed conf object")
            
        # NOTE: to disable some of the tasks, adjust this list: 

        TASKS=[securelocaldir, logdir, mountdir, persistencedir, \
               tmpdir, localdir, mounttool, dhcpconfig, userscripts, \
               thisexe, sudo]
            
        if opts.exe:
            tasks = [thisexe]
        else:
            tasks = TASKS

        for task in tasks:
            task().run(opts, conf)

    except StopInstallation, e:
        print "\nGoodbye.  (%s)" % e.msg
        return 1

    except StopInstallationError, e:
        log.error("Exiting because of a problem: %s" % e.msg)
        return 2
        
    except Exception, e:
        # a problem we did not case for
        log.exception("Problem running:")
        return 3

def main():
    return start()

if __name__ == "__main__":
    sys.exit(main())


