#!/usr/bin/env python

# Copyright 1999-2008 University of Chicago

# Project home: http://www.nimbusproject.org

# A code folding editor is recommended, folds hand-marked with {{{ and }}}
# Alternatively, see INDEX below for section line numbers.

# ############################################################
# I. Globals
# #########################################################{{{

VERSION = "2.5"

# Apache License 2.0:
LICENSE = """

Copyright 1999-2008 University of Chicago

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License.

"""

# result of "generate-index.py < workspacepilot.py"
INDEX = """
      I. Globals                                (lines 10-69)
     II. Embedded, default configuration file   (lines 71-188)
    III. Imports                                (lines 190-216)
     IV. Exceptions                             (lines 218-344)
      V. Logging                                (lines 346-565)
     VI. Signal handlers                        (lines 567-669)
    VII. Timer                                  (lines 671-696)
   VIII. Path/system utilities                  (lines 698-1057)
     IX. Action                                 (lines 1059-1110)
      X. ReserveSlot(Action)                    (lines 1112-1715)
     XI. KillNine(ReserveSlot)                  (lines 1717-1795)
    XII. ListenerThread(Thread)                 (lines 1797-1902)
   XIII. StateChangeListener                    (lines 1904-2130)
    XIV. XenActions(StateChangeListener)        (lines 2132-2846)
     XV. FakeXenActions(XenActions)             (lines 2848-2962)
    XVI. XenKillNine(XenActions)                (lines 2964-3095)
   XVII. VWSNotifications(StateChangeListener)  (lines 3097-3712)
  XVIII. Configuration objects                  (lines 3714-3944)
    XIX. Convert configurations                 (lines 3946-4206)
     XX. External configuration                 (lines 4208-4278)
    XXI. Commandline arguments                  (lines 4280-4495)
   XXII. Standalone entry and exit              (lines 4497-4690)
"""

RESTART_XEND_SECONDS_DEFAULT = 2.0

# evaluate prefix, for log messages
EP = "evaluate:"

# Other sections also set up globals: 
#    default config (II), logging (V), signals (VI), timer (VII)

# }}} END: I. Globals

# ############################################################
# II. Embedded, default configuration file
# #########################################################{{{

DEFAULTCONFIG = """

# This is the default configuration file for the program.

# It can be changed inline or copied out into a file whose path can
# be passed into the program via command-line.  If that is done, the
# program will NOT fall back to this default configuration if there
# is an error or misconfiguration with the supplied config file.

[http]

# Shared secret needs to match container configuration (users.properties)
# connection uses digest access authentication so this is not sent in the clear
secret: pw_here

# Account value should not need to be changed, it's not a system account name.
# However, if this is commented out, HTTP based notifications will be disabled
# even if the container is asking for them.  (But it's best to just control
# notification behavior from the service configuration)
account: pilotaccount

[logging]

# if logfiledir is enabled it must be an absolute path.  If it is missing
# there will be no file based logging.  
# Note that in many circumstances the pilot job's stdout/stderr
# pipe will be cut off by the LRM, leaving file based logging as the only
# window for troubleshooting and audit.
logfiledir = /tmp/

# If logfileprefixis is "pilot-", example files: "pilot-$ID.txt" where ID is
# the slot ID (UUID) and "pilot-otherlogs.txt" when no slot ID is available.
# May be missing.
logfileprefix = workspace-pilot-

[xen]

# Guides whether you need sudo or not for xm commands.  You may be using
# unix sockets with the xen-api for example (which is a good configuration
# if the permissions are correct on /var/run/xend/xen-api.sock ).
# Anything that is not 'yes' is taken as a no.
xmsudo: yes

# Should be an absolute path if using sudo, so that sudo rule contains an 
# absolute path.
xm: /usr/sbin/xm

# Minimum MB to ever let dom0 memory be decreased to (safety check, required)
# xend-config.sxp file has a safety check as well
minmem: 256

# The dom0 MB to set after a kill-nine operation.  This should match the 
# node's boot parameter (e.g. grub line of dom0_mem=2007M).  See documentation
# for how to arrive at this number.
dom0_mem: 2007

# This is optional.  If configured and xend is down, this will be used
# to try and restart xend via 'xend restart'.  If unconfigured, xend being
# down is just an unrecoverable error.
# Adds on the order of a ~100ms if all is well.
# NOTE: if you are using virtual interfaces (e.g. "eth0:0") in dom0, a
#       xend restart will leave your machine without networking. In the
#       future we may add the ability to automatically take those down
#       and bring them back afterwards.  For now consider aliasing xend
#       to a wrapper script that does this for you.
# NOTE: requires extra sudo policy line.  This should be an absolute path
#       so that sudo rule contains absolute path.
#xend: /usr/sbin/xend

# Also optional, used if xend is configured.  Seconds to wait for
# xend to boot after we found it missing and restarted it.
# If unconfigured, default is 2.0 seconds
#restart_xend_secs: 0.3

[systempaths]

# This is only necessary if using SSH as a backup notification mechanism
# (relative or absolute is fine).  If this is commented out, SSH notifications
# will be disabled even if the container is asking for them.  (But it's
# best to just control notification behavior from the service configuration)
ssh: ssh

# If you are using xm via sudo or using xend, this is needed. It should be an
# absolute path.   See 'xmsudo' configuration.
sudo: /usr/bin/sudo

[other]

# This allows you to force what this program thinks the hostname is
# in case the gethostname system call returns the incorrect name for the
# purposes of notifications because of some DNS or multi-homing issue.
# Not set by default: uncomment and insert the proper hostname.
#forcehostname:

# When the program receives a signal to end before it expected to, it
# will sleep a short time, giving a chance for plugins to take action 
# to make way for the slot to be freed.  
# For example, the workspace service is notified of the situation and 
# will run shutdown-trash to make way.  This setting is the ratio of 
# the grace period to wait until going ahead with unreserving the slot.
# After this wait the included Xen plugin will kill anything in its way 
# to make sure the slot is relinquished. 
# Required.  
# Must be 0 or float between 0 and 1 (1 does not make any sense).
earlywaitratio = 0.5

# This allows you to force the path of the credential used for SSH
# in case the account's default credential is not the needed one.
# Not set by default: uncomment and insert the absolute path.
#sshcredential:

"""

# }}} END: II. Embedded, default configuration file

# ############################################################
# III. Imports
# #########################################################{{{

import ConfigParser
import grp
import logging
import optparse
import os
import pwd
import signal
import socket
import stat
import string
import sys
import time
import urllib2

try:
    from threading import Thread
except ImportError:
    from dummy_threading import Thread

# not using subprocess package to maintain at least python 2.3 compat.
from popen2 import Popen3

# }}} END: III. Imports

# ############################################################
# IV. Exceptions
# #########################################################{{{

class InvalidInput(Exception):
    
    """Exception for illegal commandline syntax/combinations."""
    
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg

class InvalidConfig(Exception):
    
    """Exception for misconfigurations."""
    
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg

class IncompatibleEnvironment(Exception):
    
    """Exception for when something has determined a problem with the
    deployment environment."""
    
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg

class UnexpectedError(Exception):
    
    """Exception for when a function/listener cannot proceed."""
    
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg

class ProgrammingError(Exception):
    
    """Not listed in docstrings, should never be seen except during
    development."""
    
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg
        
class AKilledVM:
    
    """Container for reporting killed VMs via KilledVMs exception."""
    
    def __init__(self, vmid, name, mem):
        """Populate the object with kill information.
        
        Required parameter:
            
        * vmid -- VMM's VM ID number (int)
        
        * name -- VMM's VM name (str)
        
        * mem -- memory VM was consuming (int)
        
        Available properties:
        
        * vmid -- VMM's VM ID number
        
        * name -- VMM's VM name
        
        * mem -- memory VM was consuming
        
        """
        
        if not isinstance(vmid, int):
            raise ProgrammingError("vmid not an int")
        if not isinstance(name, str):
            raise ProgrammingError("name not a string")
        if not isinstance(mem, int):
            raise ProgrammingError("mem not an int")
        
        self.vmid = vmid
        self.name = name
        self.mem = mem
        
    def __str__(self):
        return "vmid=%d,name='%s',mem=%d" % (self.vmid, self.name, self.mem)
        
class KilledVMs(Exception):
    
    """Exception thrown by listener when it has destroyed a running VM."""
    
    def __init__(self, listofVMs):
        """Populate the exception with list of killed VMs.
        
        Required parameter:
            
        * listofVMs -- a list of AKilledVM instances with length > 0
        
        Available property:
        
        * vms -- the list of AKilledVM instances
        
        """
        
        errmsg = "listofVMs must be list with length > 0"
        
        if listofVMs == None:
            raise ProgrammingError(errmsg)
            
        if not isinstance(listofVMs, list):
            raise ProgrammingError(errmsg)
            
        if len(listofVMs) < 1:
            raise ProgrammingError(errmsg)
            
        self.vms = listofVMs
        
    def __str__(self):
        ret = ""
        for n,vm in enumerate(self.vms):
            ret += "| #%d: %s " % (n+1,vm)
        return ret

# }}} END: IV. Exceptions

# ############################################################
# V. Logging
# #########################################################{{{

class logWrap:
    def __init__(self, logger):
        """This class is used to silence logging issues (broken pipe when
        detached, file permission change, etc) from impeding operations.

        Required parameters:

        * logger -- the real logger

        """
        self.logger = logger
        
    def wrap(func, *args, **kw):
        try:
            result = func(*args, **kw)
        except:
            return None
        return result
        
    def debug(*args, **kw):
        return self.wrap(self.logger.debug, *args, **kw)
        
    def error(*args, **kw):
        return self.wrap(self.logger.error, *args, **kw)
        
    def info(*args, **kw):
        return self.wrap(self.logger.info, *args, **kw)
        
    def exception(*args, **kw):
        return self.wrap(self.logger.exception, *args, **kw)
        
    def critical(*args, **kw):
        return self.wrap(self.logger.critical, *args, **kw)
        
    def addHandler(*args, **kw):
        return self.wrap(self.logger.addHandler, *args, **kw)

def getlog(override=None):
    """Allow developer to replace logging mechanism, e.g. if this
    module is incorporated into another program as an API.

    Keyword arguments:

    * override -- Custom logger (default None, uses global variable)

    """
    global _log
    if override:
        _log = override
    try:
        _log
    except:
        _log = logging.getLogger("workspace-pilot")
        _log.setLevel(logging.DEBUG)
    return _log

def configureLogging(level, 
                     formatstring=None, 
                     logger=None, 
                     trace=False,
                     slotid=None,
                     stdout=False,
                     logfiledir=None,
                     logfileprefix=None):
    """Configure the logging format and mechanism.  Sets global 'log' variable.
    
    Required parameter:
        
    * level -- log level

    Keyword arguments:

    * formatstring -- Custom logging format (default None, uses time+level+msg)

    * logger -- Custom logger (default None)
    
    * trace -- trace (default False)
    
    * slotid -- identifier (default None)
    
    * stdout -- also log to stdout (use with caution) (default False)
    
    * logfiledir -- directory for log files (default None)
    
    * logfileprefix -- prefix for log files, may remain None (default None)

    """

    global log
    
    logger = getlog(override=logger)
    
    if not formatstring:
        if trace:
            formatstring = "%(asctime)s %(levelname)s @%(lineno)d: %(message)s"
        else:
            formatstring = "%(asctime)s %(levelname)s: %(message)s"
    formatter = logging.Formatter(formatstring)
    
    tracemessage = ""
    
    if logfiledir:
        if not os.path.isabs(logfiledir):
            msg = "logfiledir is not an absolute path: '%s'" % logfiledir
            raise InvalidConfig(msg)
        if not os.path.exists(logfiledir):
            msg = "logfiledir does not exist: '%s'" % logfiledir
            raise InvalidConfig(msg)
        if not os.path.isdir(logfiledir):
            msg = "logfiledir is not a directory: '%s'" % logfiledir
            raise InvalidConfig(msg)
            
        logfilepath = ""
        if logfileprefix:
            logfilepath += str(logfileprefix)
            
        if slotid:
            logfilepath += str(slotid)
        else:
            logfilepath += str("otherlogs")
            
        logfilepath += ".txt"
        
        logfilepath = os.path.join(logfiledir, logfilepath)
            
        f = None
        try:
            f = file(logfilepath, 'a')
            f.write("\n## auto-generated @ %s\n\n" % time.ctime())
        finally:
            if f:
                f.close()
                
        logfilehandler = logging.FileHandler(logfilepath)
        logfilehandler.setLevel(level)
        logfilehandler.setFormatter(formatter)
        logger.addHandler(logfilehandler)
        
        tracemessage += "[file logging enabled @ '%s'] " % logfilepath

    if stdout:
        ch = logging.StreamHandler()
        ch.setLevel(level)
        ch.setFormatter(formatter)
        logger.addHandler(ch)
        
        tracemessage += "[stdout logging enabled]"

    # set global variable
    log = logger
    
    if trace and tracemessage:
        log.debug(tracemessage)

TIMESTAMP_PATH=None

def persistent_timestamp(eventname, workspid=0):
    """Record an event timestamp"""
    
    if not TIMESTAMP_PATH:
        return
    
    # workspid will always be 0 or >0 from pilot, never -1
    
    global _timestamp_lines
    try:
        _timestamp_lines
    except:
        _timestamp_lines = []
    
    # needs milliseconds too
    curtime = time.time()
    timestr = time.strftime("%Y-%m-%d-%H-%M-%S", time.gmtime(curtime))
    ms = int((curtime - int(curtime)) * 1000)
    timestr = timestr + "-" + str(ms)
    
    seppi = "___"
    line = seppi + str(eventname) + seppi + timestr + seppi + str(workspid) + seppi
    
    try:
        log.debug(line)
    except:
        pass
        
    _timestamp_lines.append(line)
    _timestamp_lines.append("\n")
    
    return None

def write_persistent_timestamps():
    """Actually write the file out"""
    
    if not TIMESTAMP_PATH:
        return
    
    global _timestamp_lines
    try:
        _timestamp_lines
    except:
        msg = "persistent_timestamp() never called?"
        try:
            log.critical(msg)
        except:
            print >> sys.stderr, msg
        return None
        
    f = None
    try:
        f = open(TIMESTAMP_PATH, 'a')
        f.writelines(_timestamp_lines)
    finally:
        if f:
            f.close()
    _timestamp_lines = []

# }}} END: V. Logging

# ############################################################
# VI. Signal handlers
# #########################################################{{{

def getaction():
    """Used for signal handling"""
    try:
        _action
    except:
        return None
    return _action

def setaction(action):
    """Used for signal handling"""
    global _action
    _action = action

def signal_handler(signame):
    """Triage a signal

    Return (current action, elapsed secs) or **os._exit** (immediate stop).

    """

    elapsed = elapsedsecs()
    action = getaction()

    if elapsed != None:
        msg = "%s, elapsed seconds: %.3f" % (signame, elapsed)
    else:
        msg = "%s, elapsed seconds not recorded" % signame

    if not log:
        msg += ".  No logger, assuming main was never traversed"
        if action:
             msg += " (but there was an action configured??)"
             print >> sys.stderr, msg
             os._exit(6)
        print >> sys.stderr, msg
        os._exit(5)

    log.error(msg)

    if not action:
        log.error("no current action, assuming main was never traversed")
        os._exit(5)

    if not elapsed:
        log.error("no elapsed seconds, assuming main was never traversed")
        os._exit(5)

    return (action, elapsed)

def sigint_handler(signum, frame):
    """Handle SIGINT"""
    
    (action, elapsed) = signal_handler("SIGINT")
    err = ""
    try:
        action.handle_sigint(elapsed)
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__
        except AttributeError:
            exceptname = exception_type
        err = "Problem: %s: %s" % (str(exceptname), str(sys.exc_value))

    write_persistent_timestamps()
        
    if err:
        log.error(err)
        os._exit(6)
    else:
        os._exit(5)

def sigterm_handler(signum, frame):
    """Handle SIGTERM"""
    
    (action, elapsed) = signal_handler("SIGTERM")
    
    persistent_timestamp("PILOT18")
    err = ""
    try:
        action.handle_sigterm(elapsed)
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__
        except AttributeError:
            exceptname = exception_type
        err = "Problem: %s: %s" % (str(exceptname), str(sys.exc_value))

    persistent_timestamp("PILOT90")
    write_persistent_timestamps()
        
    if err:
        log.error(err)
        os._exit(6)
    else:
        os._exit(5)

# }}} END: VI. Signal handlers

# ############################################################
# VII. Timer
# #########################################################{{{

def starttimer():
    """Record current time"""
    global _t0
    _t0 = time.time()
    return None

def elapsedsecs():
    """Return elapsed seconds (float) since starttimer() was called."""
    try:
        _t0
    except:
        msg = "starttimer() never called"
        if log:
            log.critical(msg)
        else:
            print >> sys.stderr, msg
        return None

    t1 = time.time()
    return t1 - _t0

# }}} END: VII. Timer

# ############################################################
# VIII. Path/system utilities
# #########################################################{{{

def checkabsexists(path, name):
    """Checks if path is absolute and exists.
    
    Raises IncompatibleEnvironment if there is a problem.
    
    Returns nothing.
    
    """
    
    if not os.path.exists(path):
        msg = "%s path '%s' does not exist on filesystem" % (name, path)
        log.error(msg)
        raise IncompatibleEnvironment(msg)
        
    if path[0] != '/':
        msg = "%s path '%s' should be absolute path" % (name, path)
        log.error(msg)
        raise IncompatibleEnvironment(msg)
        
def bashEscape(cmd):
    """returns \ escapes for some bash special characters
    
    Required parameter:
    
    * cmd - command string
    
    Return escaped string.
    
    """
    
    if not cmd:
        return cmd
    escs = "\\'`|;()?#$^&*="
    for e in escs:
        idx = 0
        ret = 0
        while ret != -1:
            ret = cmd.find(e, idx)
            if ret >= 0:
                cmd = "%s\%s" % (cmd[:ret],cmd[ret:])
                idx = ret + 2
    return cmd
        
def modeStr(mode):
    """Returns string form of given mode."""
    string=""
    mode=stat.S_IMODE(mode)
    for i in ("USR", "GRP", "OTH"):
        for perm in "R", "W", "X":
            if mode & getattr(stat, "S_I"+ perm + i):
                string = string + perm.lower()
            else:
                string = string + "-"
    return string
        
def checkrootpermissions(path, trace=False):
    """Check if root is the only account with ownership and write permissions
    up to the / directory.
    
    Required parameter:
    
    * path -- Path in question. Assumed to be absolute
    
    Keyword parameter:
    
    * trace -- trace boolean, default False
    
    Raises IncompatibleEnvironment if there is a problem.
    
    """
       
    uidx = stat.ST_UID
    gidx = stat.ST_GID
    midx = stat.ST_MODE
    
    log.debug("checking permissions: '%s'" % path)
    
    # not guaranteed to be 'root'
    pwentry = pwd.getpwuid(0)
    uid0str = pwentry[0]
        
    done = False
    first = True
    chownid = 0
    chownuserstr = uid0str
    while not done:
        
        if not first:
            (path, old) = os.path.split(path)
        else:
            first = False
    
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
            raise IncompatibleEnvironment(errmsg)
    
        if path == '/':
            done = True
        
        if trace:
            permdetails = "%d:%d %s" % (en[uidx], en[gidx], modeStr(en[midx]))
        
        if en[uidx] != chownid:
            pwentry = pwd.getpwuid(en[uidx])
            currchownuserstr = pwentry[0]
            
            msg = "ownership of '%s' is %d " % (path, en[uidx])
            msg += "(%s) but should be %d " % (currchownuserstr, chownid)
            msg += "(%s)" % (chownuserstr)
            raise IncompatibleEnvironment(msg)
            
        else:
            en = os.stat(path)
            mode = stat.S_IMODE(en[midx])
            pwentry = pwd.getpwuid(en[uidx])
            currnam = pwentry[0]
            grentry = grp.getgrgid(en[gidx])
            currgrp = grentry[0]
            worldrx = False
        
            # not checking for the sticky bit, sane /tmp directories
            # would not pass these simple tests (admin knows best)
            
            if mode & stat.S_IWGRP or mode & stat.S_IWOTH:
                msg = "'%s' has wrong permissions, " % path
                msg += "path and all parent directories should only be "
                msg += "owned and writeable by %s.  Permissions: " % uid0str
                msg += "%s:%s %s" % (currnam, currgrp, modeStr(en[midx]))
                raise IncompatibleEnvironment(msg)
                
            elif mode & stat.S_IROTH and mode & stat.S_IXOTH:
                worldrx = True
                
            if not worldrx:
                msg = "OK:               '%s'" % path
            else:
                msg = "OK: (world R+X)   '%s'" % path
            if trace:
                msg += " (%s)" % permdetails
            log.info(msg)
            
class SimpleRunThread(Thread):
    """Run a command with timeout options, delay, stdin, etc."""
    
    def __init__ (self, cmd, killsig=-1, killtime=0, stdin=None, delay=None):
        """Populate the thread.
        
        Required parameters:
        
        * cmd -- command to run
        
        Keyword parameters:
        
        * killsig -- signum to kill with, default is unset 
        (needed if you set a killtime)
        
        * killtime -- secs (float or int) to wait before kill, default is 
        unset (if set, needs killsig parameter)
        
        * stdin -- optional stdin to push, default is unset
        
        * delay -- secs (float or int) to wait before invoking cmd
        
        Properties available:
        
        * stdout -- stdout data or None
        
        * stderr -- stderr data or None
        
        * killed -- boolean, set True if cmd was killed
        
        * exception -- if kill won't work
        
        """
        
        Thread.__init__(self)
        self.cmd = cmd
        self.stdin = stdin
        self.killsig = killsig
        self.killtime = float(killtime)
        self.delay = delay
        self.exception = None
        self.exit = None
        self.stdout = None
        self.stderr = None
        self.killed = False
        
    def run(self):
        if self.delay:
            log.debug("delaying for %.3f secs: '%s'" % (self.delay, self.cmd))
            time.sleep(self.delay)
        log.debug("program starting '%s'" % self.cmd)
        p = Popen3(self.cmd, True)
        if self.stdin:
            if p.poll() == -1:
                p.tochild.write(self.stdin)
                p.tochild.flush()
                p.tochild.close()
                #log.debug("wrote '%s' to child" % self.stdin)
            else:
                log.error("child exited before stdin was written to")
        done = False
        while not done and self.killtime > 0:
            time.sleep(0.2)
            if p.poll() != -1:
                done = True
            self.killtime -= 0.2
            
        if not done and self.killsig != -1:
            try:
                os.kill(p.pid, self.killsig)
                self.killed = True
            except OSError, e:
                log.exception("problem killing")
                self.exception = e
                return
                
        self.exit = p.wait()
        self.stdout = p.fromchild.read()
        self.stderr = p.childerr.read()
        p.fromchild.close()
        p.childerr.close()
        log.debug("program ended: '%s'" % self.cmd)
        
def runsudotest(cmd, allowbadexit=False):
    """Run a test sudo command.
    
    Required parameter:
    
    * cmd -- command to run, string
    
    Keyword parameters:
    
    * allowbadexit -- don't throw exception if there was only a bad exit code
    (default False)
    
    Return stdout if successful.
    
    Raises IncompatibleEnvironment if there is a problem.
    
    """
    
    thr = SimpleRunThread(cmd, killsig=signal.SIGKILL, killtime=3)
    thr.start()
    thr.join()
    
    configsudo = "\n\n  **SUDO PROBLEM** Guessing you need to manually run "
    configsudo += "sudo once in a terminal (try '%s'), many systems " % cmd
    configsudo += "require each account to accept 'the rules' one time.\n\n"
    
    if thr.stderr or thr.exit:
        msg = "Problem running '%s'." % cmd
        msg += "exit code = %d, stderr = %s" % (thr.exit, thr.stderr)
        if thr.stderr.rfind("usual lecture") > 0:
            msg += configsudo
        if allowbadexit:
            log.debug("OK error: %s" % msg)
        else:
            raise IncompatibleEnvironment(msg)
        
    # sudo child won't take signals
    if thr.exception:
        raise IncompatibleEnvironment(configsudo)
        
    return thr.stdout
        
def runexe(cmd, killtime=2.0):
    """Run a system program.
    
    Required parameter:
    
    * cmd -- command to run, string
    
    Keyword parameter:
    
    * killtime -- how many seconds to wait before SIGKILL (int or float)
    Default is 2.0 seconds.
    
    Return (exitcode, stdout, stderr)
    
    * exitcode -- integer exit code
    
    * stdout -- stdout or None
    
    * stderr -- stderr or None
    
    Raises IncompatibleEnvironment for serious issue (but not on non-zero exit)
    
    """
    
    thr = SimpleRunThread(cmd, killsig=signal.SIGKILL, killtime=killtime)
    thr.start()
    thr.join()
    
    # sudo child won't take signals
    if thr.exception:
        raise IncompatibleEnvironment(str(thr.exception))
        
    return (thr.exit, thr.stdout, thr.stderr)

def getstdout(cmd, strip=False):
    """Retrieve stdout simply.
    
    Required parameter:
    
    * cmd -- single string command to run.
    
    Keyword parameters:
    
    * strip -- remove last newline if it exists (boolean, default is False)
    
    Raises IncompatibleEnvironment if there is a problem (defined by presence
    of stderr...).
    
    Return stdout.
    
    """
    
    try:
        fstdin, fstdout, fstderr = os.popen3(cmd)
        fstdin.close()
        stdout = fstdout.read()
        fstdout.close()
        stderr = fstderr.read()
        fstderr.close()
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        name = str(exceptname)
        err = str(sys.exc_value)
        errmsg = "Problem running '%s': %s: %s\n" % (cmd, name, err)
        raise IncompatibleEnvironment(errmsg)
    if stderr:
        raise IncompatibleEnvironment(stderr)
    else:
        if stdout:
            if strip and stdout[-1] == '\n':
                stdout = stdout[:-1]
            return stdout
        else:
            raise IncompatibleEnvironment("no output?")
            
# }}} END: VIII. Path/system utilities

# ############################################################
# IX. Action
# #########################################################{{{

class Action:

    """Parent class of every action."""

    def run(self):
        """Start.

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """

        pass

    def handle_sigterm(self, elapsed):
        """Received SIGTERM.

        Required parameter:

        * elapsed -- seconds after main() began that signal was sent (float)

        Return nothing.
        
        Raise UnexpectedError if there was a problem.

        """

        pass

    def handle_sigint(self, elapsed):
        """Received SIGINT.

        Required parameter:

        * elapsed -- seconds after main() began that signal was sent (float)

        Return nothing.
        
        Raise UnexpectedError if there was a problem.

        """

        pass

# }}} END: IX. Action

# ############################################################
# X. ReserveSlot(Action)
# #########################################################{{{

class ReserveSlot(Action):

    """Class implementing the reserve slot action."""

    STATE_BEGIN = 0
    STATE_EVALUATING = 10
    STATE_ERROR_EVALUATING = 11

    STATE_RESERVING = 20
    STATE_ERROR_RESERVING = 21

    STATE_RESERVED = 30
    STATE_ERROR_RESERVED = 31

    STATE_UNRESERVING = 40
    STATE_ERROR_UNRESERVING = 41

    STATE_UNRESERVED = 50
    STATE_ERROR_UNRESERVED = 51

    STATE_EARLY_UNRESERVING = 60
    STATE_ERROR_EARLY_UNRESERVING = 61

    STATE_EARLY_UNRESERVED = 70
    STATE_ERROR_EARLY_UNRESERVED = 71
    
    # for log messages:
    statestrings = {
            STATE_BEGIN:'begin',
            STATE_EVALUATING:'evaluating',
            STATE_ERROR_EVALUATING:'error-evaluating',
            
            STATE_RESERVING:'reserving',
            STATE_ERROR_RESERVING:'error-reserving',
            
            STATE_RESERVED:'reserved',
            STATE_ERROR_RESERVED:'error-reserved',
            
            STATE_UNRESERVING:'unreserving',
            STATE_ERROR_UNRESERVING:'error-unreserving',
            
            STATE_UNRESERVED:'unreserved',
            STATE_ERROR_UNRESERVED:'error-unreserved',
            
            STATE_EARLY_UNRESERVING:'early-unreserving',
            STATE_ERROR_EARLY_UNRESERVING:'error-early-unreserving',
            
            STATE_EARLY_UNRESERVED:'early-unreserved',
            STATE_ERROR_EARLY_UNRESERVED:'error-early-unreserved' }

    def __init__(self, common, slotconf, listeners):
        """Instantiate object with configurations necessary to operate and
        initialize any listener objects.

        Required parameters:

        * common -- CommonConf instance

        * slotconf -- ReserveSlotConf instance

        * listeners -- list of StateChangeListener instances

        Raise InvalidConfig if problem with the supplied configurations.

        """

        self.common = common
        self.conf = slotconf

        self.real_sleep_duration = 0

        self.listeners = []

        if listeners:
            for listener in listeners:
                listener.initialize()
                self.listeners.append(listener)

        self.allerrors = ""
        self.state_when_interrupted = None

        self.state = self.STATE_BEGIN

        # a handler is set to None when the program was already exiting
        self.oninterrupt = {
                self.STATE_BEGIN:self.run_errorReserving,
                self.STATE_EVALUATING:self.run_errorReserving,
                self.STATE_ERROR_EVALUATING:None,

                self.STATE_RESERVING:self.run_errorReserving,
                self.STATE_ERROR_RESERVING:None,

                self.STATE_RESERVED:self.run_unreserving,
                self.STATE_ERROR_RESERVED:None,

                self.STATE_UNRESERVING:self.run_errorUnreserving,
                self.STATE_ERROR_UNRESERVING:None,

                self.STATE_UNRESERVED:None,
                self.STATE_ERROR_UNRESERVED:None,

                self.STATE_EARLY_UNRESERVING:self.run_errorEarlyUnreserving,
                self.STATE_ERROR_EARLY_UNRESERVING:None,

                self.STATE_EARLY_UNRESERVED:None,
                self.STATE_ERROR_EARLY_UNRESERVED:None }
                
        # all methods in oninterrupt dict values should be a key here
        self.impliedstate = {
                self.run_errorReserving:self.STATE_ERROR_RESERVING,
                self.run_unreserving:self.STATE_UNRESERVING,
                self.run_errorUnreserving:self.STATE_ERROR_UNRESERVING,
                self.run_errorEarlyUnreserving:self.STATE_EARLY_UNRESERVING }

    def run(self):
        """Start.

        Overrides Action.run()

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """
        if self.common.trace:
            log.debug("ReserveSlot.run() called")

        if self.common.evaluate:
            self.state = self.STATE_EVALUATING
            log.debug("new state: %s" % self.statestrings[self.state])
            self.allerrors += self.run_evaluate()
            if self.allerrors:
                raise UnexpectedError(self.allerrors)

        self.state = self.STATE_RESERVING
        log.debug("new state: %s" % self.statestrings[self.state])
        self.allerrors += self.run_reserving()
        if self.allerrors:
            self.allerrors += self.run_errorReserving(errstr=self.allerrors)
            raise UnexpectedError(self.allerrors)

        self.state = self.STATE_RESERVED
        log.debug("new state: %s" % self.statestrings[self.state])
        self.allerrors += self.run_reserved()
        if self.allerrors:
            raise UnexpectedError(self.allerrors)

        # subtract the time we've used so far (margin of error is the time
        # taken to start the Python interpreter).
        t1 = elapsedsecs()
        if t1 != None:
            real_sleep_duration = self.conf.duration - t1
        else:
            raise ProgrammingError("elapsedsecs() returned None")
            
        # corner case can happen if windows are very small and e.g.
        # duration so far was exacerbated by a xend restart
        if real_sleep_duration < 0:
            real_sleep_duration = 0

        if real_sleep_duration:
            log.info("sleeping for %0.3f seconds" % real_sleep_duration)
            # implementing this is a listener doesn't work well with signals
            time.sleep(real_sleep_duration)
        else:
            log.info("not sleeping")

        self.state = self.STATE_UNRESERVING
        log.debug("new state: %s" % self.statestrings[self.state])
        self.allerrors += self.run_unreserving()
        if self.allerrors:
            self.allerrors += self.run_errorUnreserving(errstr=self.allerrors)
            raise UnexpectedError(self.allerrors)

        self.state = self.STATE_UNRESERVED
        log.debug("new state: %s" % self.statestrings[self.state])
        self.allerrors += self.run_unreserved()
        if self.allerrors:
            raise UnexpectedError(self.allerrors)

    def run_evaluate(self):
        """Move to the special evaluate state.

        Return empty string or all relevant error messages.

        """

        return self.runHandler("evaluate", self.STATE_ERROR_EVALUATING)

    def run_reserving(self):
        """Move to the reserving state.

        Return empty string or all relevant error messages.

        """

        return self.runHandler("reserving", self.STATE_ERROR_RESERVING)

    def run_errorReserving(self, errstr=None):
        """Deal with a reserving listener problem
        
        Keyword parameter:
        
        * errstr -- error messages received so far, if applicable

        Return empty string or all relevant error messages.

        """

        return self.runHandler("errorReserving", None, errstr=errstr)

    def run_reserved(self):
        """Move to the reserved state.

        Return empty string or all relevant error messages.

        """

        return self.runHandler("reserved", self.STATE_ERROR_RESERVED)

    def run_unreserving(self):
        """Move to the unreserving state.

        Return empty string or all relevant error messages.

        """

        return self.runHandler("unreserving", self.STATE_ERROR_UNRESERVING)

    def run_errorUnreserving(self, errstr=None):
        """Deal with an unreserving listener problem
        
        Keyword parameter:
        
        * errstr -- error messages received so far, if applicable

        Return empty string or all relevant error messages.

        """

        return self.runHandler("errorUnreserving", None, errstr=errstr)

    def run_unreserved(self):
        """Move to the unreserved state.

        Return empty string or all relevant error messages.

        """

        return self.runHandler("unreserved", self.STATE_ERROR_UNRESERVED)

    def run_earlyUnreserving(self):
        """Move to the early unreserving state.  Doze.  
        Then move to unreserving.

        Return empty string or all relevant error messages.

        """
        
        allerrs = ""
        
        grace = self.conf.graceperiod
        ratio = self.conf.earlywaitratio
        doze = grace * ratio
        
        msg = "earlyUnreserving: allowed to wait for %0.3f seconds" % doze
        msg += " (grace period %d * %0.5f early wait ratio)" % (grace, ratio)
        log.info(msg)
        
        # if handlers take a long time, for example the notifications are
        # blocking for some reason, this WILL impede us later, a kill9 will
        # be sent and cleanup will not be accomplished (I've seen this happen).
        # Thus, the time it takes to run this handler must be a) limited and
        # b) deducted from doze time.
        
        # handler time
        htime = float(doze * 0.90)
        
        msg = "Advising earlyUnreserving handlers to timeout after 90% of "
        msg += "%0.3f seconds which is %0.3f seconds." % (doze, htime)
        log.info(msg)
        
        stopwatch_start = time.time()
        
        try:
            errstate = self.STATE_ERROR_EARLY_UNRESERVING
            errs = self.runHandler("earlyUnreserving", errstate, timeout=htime)
            if errs:
                log.error("Problems from earlyUnreserving handlers: %s" % errs)
                log.error("Continuing with unreserve anyhow.")
                allerrs += "**ERRORS FROM EARLY_UNRESERVING: %s" % errs
            elapsedtime = float(time.time() - stopwatch_start)
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__
            except AttributeError:
                exceptname = exception_type
            allerrs += "Unexpected problem running earlyUnreserving: "
            allerrs += "%s: %s" % (str(exceptname), str(sys.exc_value))
            elapsedtime = doze + 10000 # force no doze
        
        if elapsedtime >= doze: 
            tstr = "elapsedtime %0.3f  >=  doze %0.3f" % (elapsedtime, doze)
            msg = "earlyUnreserving exception or timeout failed us: %s" % tstr
            log.critical(msg)
            doze = 0
        else:
            doze = float(doze - elapsedtime)
        
        if doze > 0:
            log.debug("earlyUnreserving sleeping for ~ %0.3f seconds." % doze)
            
            # time.sleep consistently returns earlier than expected, probably
            # has something to do with being in a signal handler?  Docs only
            # talk about receiving new signals during a sleep, not sleeping
            # in a signal handler.
            
            #time.sleep(doze)
            
            target_time = time.time() + doze
            while True:
                time.sleep(0.1)
                now = time.time()
                if now >= target_time:
                    break
            
            log.info("earlyUnreserving is finished waiting, forcing unreserve")
        else:
            log.info("earlyUnreserving not sleeping")
        
        errstate = self.STATE_ERROR_UNRESERVING
        errs = self.runHandler("unreserving", errstate)
        if errs:
            log.error("Problems from unreserving handlers: %s" % errs)
            allerrs += "\n**ERRORS FROM UNRESERVING: %s" % errs
            
        if allerrs:
            return allerrs

    def run_errorEarlyUnreserving(self, errstr=None):
        """Deal with an early unreserving listener problem
        
        Keyword parameter:
        
        * errstr -- error messages received so far, if applicable

        Return empty string or all relevant error messages.

        """

        return self.runHandler("errorEarlyUnreserving", None, errstr=errstr)
        
    def run_killedVMsNotification(self, killedVMs):
        """Inform listeners there were killed VMs.
        
        Required parameter:
        
        * killedVMs -- list of KilledVMs instances
        
        Return empty string or all relevant error messages.
        
        Never call anything that would raise a KilledVMs exception in this
        handler, there would be a small chance of infinite recursion.
        
        """
        
        if not isinstance(killedVMs, list):
            raise ProgrammingError("killedVMs is not a list")
            
        if len(killedVMs) == 0:
            raise ProgrammingError("killedVMs is an empty list")
            
        for killed in killedVMs:
            if not isinstance(killed, KilledVMs):
                msg = "something in killedVMs is not a KilledVMs instance"
                raise ProgrammingError(msg)
        
        return self.runHandler("killedVMsNotification", None, obj=killedVMs)

    def runHandler(self, handler, errorstate,
                   errstr=None, obj=None, timeout=None):
        """Invoke given handler.

        Required parameters:

        * handler -- name of the handler

        * errorstate -- if set, state to move self.state to if an error occurs
        
        Keyword parameter:
        
        * errstr -- error messages received so far, if applicable
        
        * obj -- an object parameter for the handler
        
        * timeout -- seconds to wait before killing, float.  
        Currently this must be supported by handler itself because the hang is
        usually going to be at the system I/O level, this is not a python
        execution problem.  Value of 0 is construed equal to None (i.e., not set).  (current known uses map down to the runexe function's "killtime"
        parameter -- the timeout here is not hyper correct to the point of
        accounting for the overhead of getting to that call)

        Return empty string or all relevant error messages.
        
        If handler produces a KilledVMs exception, call the appropriate
        handler on all listeners and include errlog of this in return value.

        """
        
        if timeout == 0:
            timeout = None # make sure
        
        if timeout:
            try:
                timeout = float(timeout)
            except:
                raise UnexpectedError("timeout/float problem")
        
        trace = self.common.trace
        cb = handler
        errors = ""
        self.currentthreads = []
        killedVMexceptions = []

        try:
            for lner in self.listeners:
                if errstr and obj:
                    err = errstr
                    thr = ListenerThread(lner, cb, trace, 
                                         errstr=err, obj=obj, timeout=timeout)
                elif obj:
                    thr = ListenerThread(lner, cb, trace, 
                                         obj=obj, timeout=timeout)
                elif errstr:
                    thr = ListenerThread(lner, cb, trace, 
                                         errstr=errstr, timeout=timeout)
                else:
                    thr = ListenerThread(lner, cb, trace, timeout=timeout)
                thr.start()
                self.currentthreads.append(thr)
            for thr in self.currentthreads:
                thr.join()
                if thr.exception:
                    if isinstance(thr.exception, KilledVMs):
                        killedVMexceptions.append(thr.exception)
                if not thr.success:
                    if errorstate != None:
                        self.state = errorstate
                        log.debug("new state: %s" 
                                  % self.statestrings[self.state])
                    if thr.errmsg:
                        errors += thr.errmsg
                    else:
                        errors += "undocumented err from %s" % listener
                    errors += "\n"
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__
            except AttributeError:
                exceptname = exception_type
            errors += "Unexpected problem running %s: " % cb
            errors += "%s: %s" % (str(exceptname), str(sys.exc_value))

        self.currentthreads = []
        
        # never call anything that would raise a KilledVMs exception
        # in this handler, there would be a small chance of infinite
        # recursion
        if len(killedVMexceptions) > 0:
            problem = self.run_killedVMsNotification(killedVMexceptions)
            if problem:
                errors += problem
        
        return errors

    def handle_sigterm(self, elapsed):
        """Received SIGTERM.
        
        Overrides Action.handle_sigterm()

        Required parameter:

        * elapsed -- seconds after main() began that signal was sent (float)

        Return nothing.
        
        Raise UnexpectedError if there was a problem.

        """

        self.handle_interrupt(elapsed)

    def handle_sigint(self, elapsed):
        """Received SIGINT.
        
        Overrides Action.handle_sigint()

        Required parameter:

        * elapsed -- seconds after main() began that signal was sent (float)

        Return nothing.
        
        Raise UnexpectedError if there was a problem.

        """

        self.handle_interrupt(elapsed)

    def handle_interrupt(self, elapsed):
        """Handle interrupt.

        Required parameter:

        * elapsed -- seconds after main() began that signal was sent (float)

        Return nothing.
        
        Raise UnexpectedError if there was a problem.

        """
        self.state_when_interrupted = self.state
        curstatestr = self.statestrings[self.state]
        log.error("current state = %s" % curstatestr)
        
        if self.state == self.STATE_RESERVED:
            if elapsed >= self.conf.horizon:
                log.info("elapsed (%.3f) >= horizon (%.3f) ==> not early" 
                          % (elapsed, self.conf.horizon))
                # falls through to unreserving via oninterrupt dict
            else:
                log.info("elapsed (%.3f) < horizon (%.3f) ==> early" 
                          % (elapsed, self.conf.horizon))
                self.state = self.STATE_EARLY_UNRESERVING
                log.debug("new state: %s" % self.statestrings[self.state])
                
                allerrs = ""
                
                errs = self.run_earlyUnreserving()
                if errs:
                    allerrs += errs
                    errs = self.run_errorEarlyUnreserving(errstr=errs)
                    if errs:
                        allerrs += errs
                self.state = self.STATE_EARLY_UNRESERVED
                log.debug("new state: %s" % self.statestrings[self.state])
                errs = self.run_unreserved()
                if errs:
                    allerrs += errs
                if allerrs:
                    raise UnexpectedError(allerrs)
                    
                return
                
        method = self.oninterrupt[self.state]
        if not method:
            if self.common.trace:
                log.debug("nothing needs to be done for this state")
            return
                
        name = method.__name__
        log.debug("interrupt handler for '%s': %s" % (curstatestr, name))
        self.interrupted_elapsed = elapsed

        errors = ""
        
        try:
            self.state = self.impliedstate[method]
            log.debug("new state: %s" % self.statestrings[self.state])
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__
            except AttributeError:
                exceptname = exception_type
            errors += "Problem setting new state from method '%s': " % method
            errors += "%s: %s" % (str(exceptname), str(sys.exc_value))
            # continue though
        
        try:
            method()
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__
            except AttributeError:
                exceptname = exception_type
            errors += "Problem running %s: " % method
            errors += "%s: %s" % (str(exceptname), str(sys.exc_value))
            
        if errors:
            raise UnexpectedError(errors)
        
        # not worrying about post-method() state change

# }}} END: X. ReserveSlot(Action)

# ############################################################
# XI. KillNine(ReserveSlot)
# #########################################################{{{

class KillNine(ReserveSlot):

    """Class implementing the kill all action."""
    
    def run(self):
        """Remove all guest VMs and bring dom0 memory up to the configured
        maximum.
        
        Overrides ReserveSlot.run()

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """

        if self.common.trace:
            log.debug("Killnine.run() called")

        trace = self.common.trace
        errors = ""
        self.currentthreads = []
        killedVMexceptions = []
        
        if self.common.evaluate:
            errors += self.run_evaluate()
            if allerrors:
                raise UnexpectedError(errors)
                
        errors += self.run_unreserving()
        if errors:
            # a 'hidden' option is to have a notification listener, mainly
            # for test harness
            errors += self.run_errorUnreserving(errstr=errors)
            log.critical(errors)
        
    def handle_sigterm(self, elapsed):
        """Received SIGTERM.
        
        Overrides ReserveSlot.handle_sigterm()

        Required parameter:

        * elapsed -- seconds after main() began that signal was sent (float)

        Return nothing.
        
        Raise UnexpectedError if there was a problem.

        """

        # TODO: print out any killed so far?
        pass

    def handle_sigint(self, elapsed):
        """Received SIGINT.
        
        Overrides ReserveSlot.handle_sigint()

        Required parameter:

        * elapsed -- seconds after main() began that signal was sent (float)

        Return nothing.
        
        Raise UnexpectedError if there was a problem.

        """

        # TODO: print out any killed so far?
        pass
    
# }}} END: XI. KillNine(ReserveSlot)

# ############################################################
# XII. ListenerThread(Thread)
# #########################################################{{{

class ListenerThread(Thread):

    """Class for running listener handlers."""

    def __init__ (self, listener, method_name, trace,
                  errstr=None, obj=None, timeout=None):
        """Populate the thread with the listener and the method to be called

        Required parameters:

        * listener -- instance of StateChangeListener

        * method_name -- name of the handler method to be invoked

        * trace -- if trace is set
        
        Keyword parameter:
        
        * errstr -- error messages received so far, if applicable
        
        * obj -- some object parameter
        
        * timeout -- advise a handler to timeout
        
        Available properties:
        
        * success -- boolean (True if method threw exception)
        
        * errmsg -- error message from exception
        
        * exception -- the exception

        Raise ProgrammingError if a parameter is None or if listener does
        not contain the method to be called.

        """

        Thread.__init__(self)

        if not listener:
            raise ProgrammingError("no listener")
        if not method_name:
            raise ProgrammingError("no method_name")

        method = getattr(listener, method_name, None)
        if not method:
            err = "%s not implemented by %s" % (method_name, listener)
            raise ProgrammingError(err)

        if not callable(method):
            raise ProgrammingError("method %s not callable" % method_name)

        self.listener = listener
        self.method = method
        self.method_name = method_name
        self.trace = trace
        self.errstr = errstr
        self.obj = obj
        
        self.timeout = timeout

        # for return
        self.success = False
        self.errmsg = None
        self.exception = None

    def run(self):
        """Run the handler and record any errors for action."""

        if self.trace:
            log.debug("(calling '%s' %s)" 
                      % (self.method_name, self.listener))

        # jeesh, maybe just move to **kwargs in the handler defs at this point
        try:
            if self.obj and self.errstr:
                self.method(errstr=self.errstr, obj=self.obj,
                            timeout=self.timeout)
            elif self.obj:
                self.method(obj=self.obj, timeout=self.timeout)
            elif self.errstr:
                self.method(errstr=self.errstr, timeout=self.timeout)
            else:
                self.method(timeout=self.timeout)
            self.success = True
            if self.trace:
                log.debug("(called OK: '%s' %s)"
                          % (self.method_name, self.listener))
        except Exception, e:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__
            except AttributeError:
                exceptname = exception_type
            exceptmsg = "%s: %s" % (str(exceptname), str(sys.exc_value))
            log.error("Problem calling '%s' handler on listener %s || %s"
                      % (self.method_name, self.listener, exceptmsg))
            self.success = False
            self.errmsg = exceptmsg
            self.exception = e

# }}} END: XII. ListenerThread(Thread)

# ############################################################
# XIII. StateChangeListener
# #########################################################{{{

class StateChangeListener:

    """Parent class for classes implementing handlers for when state 
    changes have occurred.  Actions will invoke these handlers (in a
    dedicated, simultaneously executed thread for each listener) after
    a state has been reached."""

    def __init__(self, conf, common):
        """Set the configurations.

        Required parameters:

        * conf -- Instance-specific configuration object.

        * common -- Instance of CommonConf.

        Raise InvalidConfig if either parameter is None.

        """

        self.conf = conf
        self.common = common
        self.initialized = False

        if not self.conf:
            raise InvalidConfig("No configuration object")
        if not self.common:
            raise InvalidConfig("No CommonConf object")

        if self.common.trace:
            name = self.conf.__class__.__name__
            log.debug("StateChangeListener created with %s instance." % name)

    def initialize(self):
        """Perform initialization work.

        Raise InvalidConfig if there is a problem with the supplied
        configuration object.

        Return nothing.

        """

        if self.common.trace:
            log.debug("StateChangeListener.initialize() called")
        self.initialized = True

    def evaluate(self, timeout=None):
        """Optionally run extra tests for when program is in evaluate mode.

        Raise IncompatibleEnvironment if there is a problem with an
        interaction with remote systems or the local one.

        Return nothing.

        """

        pass

    # Reserving:

    def reserving(self, timeout=None):
        """Reserving.
        
        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.
        (causes errorReserving)

        Raise IncompatibleEnvironment if it is impossible to proceed.
        (causes errorReserving)
        
        """

        pass

    def errorReserving(self, errstr=None, timeout=None):
        """Action or a listener had an error during reserving.

        Keyword parameter:
        
        * errstr -- error messages received so far, if applicable
        
        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """

        pass

    def reserved(self, timeout=None):
        """Reserved successfully.

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """

        pass

    # Unreserving:

    def unreserving(self, timeout=None):
        """Unreserving.

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.
        (causes errorUnreserving)

        Raise IncompatibleEnvironment if it is impossible to proceed.
        (causes errorUnreserving)
        
        Raise KilledVMs if there was a problem that was "taken care of".

        """

        pass

    def errorUnreserving(self, errstr=None, timeout=None):
        """Action or a listener had an error during unreserving.

        Keyword parameter:
        
        * errstr -- error messages received so far, if applicable
        
        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """

        pass

    def unreserved(self, timeout=None):
        """Unreserved successfully.

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """

        pass

    # Early unreserving:

    def earlyUnreserving(self, timeout=None):
        """Early unreserving.

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.
        (causes errorEarlyUnreserving)

        Raise IncompatibleEnvironment if it is impossible to proceed.
        (causes errorEarlyUnreserving)

        """

        pass

    def errorEarlyUnreserving(self, errstr=None, timeout=None):
        """Action or a listener had an error during early unreserving.
        
        Keyword parameter:
        
        * errstr -- error messages received so far, if applicable

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """

        pass

    def earlyUnreserved(self, timeout=None):
        """Unreserved early successfully.

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """

        pass
        
    def killedVMsNotification(self, obj=None, errstr=None, timeout=None):
        """VMs were killed.
        
        Keyword parameters:
        
        * obj -- object with information (in this impl this will in
        practice always be a list of KilledVMs exceptions).
        
        * errstr -- error messages received so far, if applicable

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """

        pass

# }}} END: XIII. StateChangeListener

# ############################################################
# XIV. XenActions(StateChangeListener)
# #########################################################{{{

class XenActions(StateChangeListener):

    """StateChangeListener that performs Xen actions on the system.

    Overrides StateChangeListener methods:

    * initialize()
    
    * evaluate()

    * reserving() -- Adjust dom0 memory to make room for this request.

    * unreserving() -- Adjust dom0 memory back to state before program ran.

    """
    
    ##############################
    # Overriden handler methods: #
    ##############################

    def initialize(self):
        """Perform initialization work.

        Overrides StateChangeListener.initialize()

        Raise InvalidConfig if there is a problem with the supplied
        configuration object.

        Return nothing.

        Expects conf to be a XenConf instance.

        """

        if self.common.trace:
            log.debug("XenActions.initialize()")
            
        if self.conf.xendpath:
            # run even in evaluate mode, tests in evaluate() will run xm etc.
            self.testXend()

        self.initialized = True
        
        # For log message tailoring only -- not for functionality difference.
        # For that, use descendant class XenKillNine(XenActions).
        self.killninemode = False
        
    def evaluate(self, timeout=None):
        """Optionally run extra tests for when program is in evaluate mode.
        
        Test the Xen commands.

        Raise IncompatibleEnvironment if there is a problem with the
        configurations/system.

        Return nothing.

        """
        
        if not self.initialized:
            raise ProgrammingError("not initialized")
        
        xm = self.conf.xmpath
        xmsudo = self.conf.xmsudo
        sudo = self.conf.sudopath
        xend = self.conf.xendpath
        
        log.info("%s xm configuration: %s" % (EP, xm))
        log.info("%s xmsudo configuration: %s" % (EP, xmsudo))
        log.info("%s sudo configuration: %s" % (EP, sudo))
        if not self.killninemode:
            log.info("%s memory configuration: %s" % (EP, self.conf.memory))
            log.info("%s minmem configuration: %s" % (EP, self.conf.minmem))
        else:
            if self.conf.memory == "BESTEFFORT":
                log.info("%s memory target: BESTEFFORT" % EP)
            else:
                log.info("%s We are in kill-nine mode.  Memory target is "
                         "dom0_mem = %d" % (EP, self.conf.memory))
        
        if xmsudo:
            log.info("%s using xm via sudo" % EP)
            checkabsexists(xm, "xm")
            checkrootpermissions(xm, trace=self.common.trace)
            
        if xend:
            log.info("%s xend is configured: %s" % (EP, xend))
            checkabsexists(xend, "xend")
            checkrootpermissions(xend, trace=self.common.trace)
            
        if xmsudo or xend:
            checkabsexists(sudo, "sudo")
            checkrootpermissions(sudo, trace=self.common.trace)
            sudoverstr = getstdout("%s -V" % sudo, strip=True)
            log.info("%s sudo version = %s" % (EP, sudoverstr))
                
        if not xmsudo:
            stdout = getstdout("%s info" % xm)
            if stdout:
                log.info("%s xm info = \n%s\n" % (EP, stdout))
            else:
                log.info("%s xm info, no stdout?" % EP)
                
        else:
            account = getstdout("whoami", strip=True)
            log.info("%s current account = %s" % (EP, account))
            
            rule = "%s ALL=(root) NOPASSWD: %s" % (account, xm)
            log.info("%s candidate xm sudo rule:\n\n%s\n" % (EP, rule))
            
            cmd = "%s %s info" % (sudo, xm)
            log.info("%s seeing if '%s' works" % (EP, cmd))
            stdout = runsudotest(cmd)
            if stdout:
                log.info("%s xm info = \n%s\n" % (EP, stdout))
            else:
                log.info("%s xm info, no stdout?" % EP)
        
        if xend:
            rule = "%s ALL=(root) NOPASSWD: %s" % (account, xend)
            log.info("%s candidate xend sudo rule:\n\n%s\n" % (EP, rule))
            cmd = "%s %s status" % (sudo, xend)
            log.info("%s seeing if '%s' works, test not entirely "
                     "accurate" % (EP, cmd))
            runsudotest(cmd, allowbadexit=True)
            # counting on no exception
            log.info("%s xend path and sudo rule likely OK." % EP)

    def reserving(self, timeout=None):
        """Reserving.  Adjust dom0 memory to make room for this request.

        Overrides StateChangeListener.reserving()

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.
        (causes errorReserving)

        Raise IncompatibleEnvironment if it is impossible to proceed.
        (causes errorReserving)

        """
        
        if not self.initialized:
            raise ProgrammingError("not initialized")
            
            
        memory = self.conf.memory
        if self.common.trace:
            log.debug("XenActions.reserving(), reserving %d MB" % memory)
            
        persistent_timestamp("PILOT2")
        curmem = self.currentAllocation_MB()
        persistent_timestamp("PILOT2B")
        
        log.debug("current memory MB = %d" % curmem)
        targetmem = curmem - self.conf.memory
        msg = "target memory MB = %d " % targetmem
        msg += "(%d - %d)" % (curmem, self.conf.memory)
        log.debug(msg)
        
        if targetmem < self.conf.minmem:
            msg = "target memory (%dM) is less than the minimum " % targetmem
            msg += "allowed memory (%dM)" % self.conf.minmem
            raise IncompatibleEnvironment(msg)
            
        # assumes no VMs are started in the meantime
        persistent_timestamp("PILOT2C")
        self.memset(targetmem)
        persistent_timestamp("PILOT3")
        
        # assumes lowering always works (see unreserving where we can't assume)

    def unreserving(self, timeout=None):
        """Unreserving.  Adjust dom0 memory back to state before program ran.

        Overrides StateChangeListener.unreserving()

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.
        (causes errorUnreserving)

        Raise IncompatibleEnvironment if it is impossible to proceed.
        (causes errorUnreserving)
        
        Raise KilledVMs if there was a problem that was "taken care of".
        
        If raising KilledVMs is necessary and if it impossible to proceed
        later, log that to critical and still raise KilledVMs.

        """
        
        if not self.initialized:
            raise ProgrammingError("not initialized")

        memory = self.conf.memory
        if self.common.trace:
            log.debug("XenActions.unreserving(), unreserving %d MB" % memory)
            
        persistent_timestamp("PILOT20")
        curmem = self.currentAllocation_MB()
        persistent_timestamp("PILOT21")
        
        if not self.common.evaluate:
            log.debug("current memory MB = %d" % curmem)
            targetmem = curmem + self.conf.memory
            msg = "target memory MB = %d " % targetmem
            msg += "(%d + %d = %d)" % (curmem, self.conf.memory, targetmem)
            log.debug(msg)
        else:
            msg = "%s current memory is actually %d but for " % (EP, curmem)
            msg += "evaluate's sake pretending current memory is current "
            msg += "memory minus the memory allocation "
            msg += "(remember no memory actually got adjusted previously "
            msg += "because this is evaluate mode)." 
            log.debug(msg)
            targetmem = curmem
            fakecurmem = curmem - self.conf.memory
            log.debug("%s fake current memory MB = %d" % (EP, fakecurmem))
            msg = "target memory MB = %d " % targetmem
            msg += "(%d + %d = %d)" % (fakecurmem, self.conf.memory, targetmem)
            log.debug(msg)
        
        # Since mem-set provides no helpful return code, using unreliable
        # guesswork to see if kills are necessary (xen-api may help)
        
        persistent_timestamp("PILOT22")
        freemem = self.currentFree_MB()
        persistent_timestamp("PILOT23")
        
        raiseme = None
        
        needed = self.conf.memory
        if freemem < needed:
            
            msg = "The current free memory not being used by dom0 or "
            msg += "guests is %d MB.  But we need %d MB " % (freemem, needed)
            msg += " free right now for dom0 to get back the memory taken "
            msg += "from it. *** Attempting to kill all guest VMs ***"
            log.error(msg)
            
            # In this first version we kill all guest VMs in this error
            # situation instead of selectively killing what would be 
            # necessary to free enough memory.
            
            # The possibly right thing to do in the future would be to
            # have some sort of regular expression parameter provided
            # at slot reservation that we could use on the VM names to
            # recognize which are going to be associated with this slot
            # and implement that pattern in the naming scheme when VMs
            # are started.
            
            killedVMs = self.killAll()
            if killedVMs:
                raiseme = KilledVMs(killedVMs)
            else:
                msg = "No killed VMs: this is unexpected because the amount "
                msg += "of memory reported to be free for guests "
                msg += "(%d MB) is not enough free memory to ensure " % freemem
                msg += "the slot's memory will be returned to dom0 "
                msg += "(%d free MB needed).  The assumption is that " % needed
                msg += "guest VMs are using this memory but we could "
                msg += "not kill any."
                log.critical(msg)
                raise UnexpectedError(msg)
        
        # assumes no VMs are started in the meantime
        try:
            persistent_timestamp("PILOT24")
            self.memset(targetmem)
            persistent_timestamp("PILOT25")
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            n = str(exceptname)
            err = str(sys.exc_value)
            errmsg = "Problem setting memory to %d: %s: %s" % (targetmem,n,err)
            if raiseme:
                log.critical(errmsg)
                raise raiseme
            else:
                raise UnexpectedError(errmsg)
        
        if raiseme:
            raise raiseme

    ####################
    # Utility methods: #
    ####################
    
    def memset(self, MB):
        """Adjust dom0 memory to specified amount.
        
        Required parameter:
        
        * MB -- the MB target
        
        Raise UnexpectedError if it is impossible to proceed
        
        """
        cmd = "%s mem-set 0 %d" % (self.conf.xmpath, MB)
        if self.conf.xmsudo:
            cmd = "%s %s" % (self.conf.sudopath, cmd)
        
        if self.common.evaluate:
            log.info("%s would have run '%s'" % (EP, cmd))
        else:
            (exit, stdout, stderr) = runexe(cmd)
            msg = "'%s': exit=%d, stdout='%s', " % (cmd, exit, stdout)
            msg += "stderr='%s'" % (stderr)
            log.debug(msg)
            if exit != 0:
                # mem-set's exit of 0 only actually means the memory target
                # on the domain was adjusted successfully, not that it was
                # reached
                raise UnexpectedError(msg)
            
    def currentFree_MB(self):
        """Return free memory for guests in MB (int).
        
        Raise UnexpectedError if it is impossible to proceed.
        
        Inspect xm info (waiting on xen-api).
        
        Return memory is whatever int is reported.
        
        """
        
        cmd = "%s info" % self.conf.xmpath
        if self.conf.xmsudo:
            cmd = "%s %s" % (self.conf.sudopath, cmd)
        (exit, stdout, stderr) = runexe(cmd)
        if exit:
            msg = "xm info failed: exit = "
            msg += "%d, stdout = %s, stderr = %s" % (exit, stdout, stderr)
            raise UnexpectedError(msg)
            
        if not stdout:
            msg = "xm info has no stdout: exit = "
            msg += "%d, stderr = %s" % (exit, stderr)
            raise UnexpectedError(msg)
            
        mem = 0
        lines = stdout.split("\n")
        done = False
        for line in lines:
            if done:
                break
            if line.rfind("free_memory") >= 0:
                parts = line.split()
                # yeeha
                for part in parts:
                    try:
                        mem = int(part)
                        done = True
                        break
                    except:
                        pass
        return mem
    
    def currentAllocation_MB(self):
        """Return current memory allocation of system in MB (int).
        
        Raise UnexpectedError if it is impossible to proceed.
        
        """
        
        return self.currentAllocation_kB() / 1024
        
    def currentAllocation_kB(self):
        """Return current memory allocation of system in kB (int).
        
        Raise UnexpectedError if it is impossible to proceed.
        
        Inspect /proc/xen/balloon (waiting on xen-api).
        
        """
        
        path = '/proc/xen/balloon'
        f = None
        result = None
        try:
            try:
                f = open(path)
                result = f.read()
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                name = str(exceptname)
                err = str(sys.exc_value)
                errmsg = "Problem opening '%s': %s: %s" % (path, name, err)
                raise UnexpectedError(errmsg)
        finally:
            if f:
                f.close()
                
        if not result:
            raise UnexpectedError("didn't read anything from %s" % path)
            
        mem = 0
        lines = result.split("\n")
        done = False
        for line in lines:
            if done:
                break
            if line.rfind("urrent allocation") >= 0:
                parts = line.split()
                # yeeha
                for part in parts:
                    try:
                        mem = int(part)
                        done = True
                        break
                    except:
                        pass
        if mem > 1:
            return mem
        else:
            raise UnexpectedError("couldn't find allocation from %s" % path)
        
    def testXend(self, logerror=False):
        """Test if xend is running and restart it if not.  If user is using
        interface aliases and these are not torn down and brought back up
        with a xend restart: WARNING.
        
        Keyword parameter:
        
        * logerror - log everything to error level (default False) for extreme
        error situations
        
        Raise UnexpectedError if it is impossible to proceed.
        
        Return xm list stdout (if it was successful)
        
        Do not call if conf.xendpath not configured.
        
        """
        
        if logerror:
            log.error("running testXend()")
        elif self.common.trace:
            log.debug("XenActions.testXend()")
        
        xmsudo = self.conf.xmsudo
        sudo = self.conf.sudopath
        xm = self.conf.xmpath
        xend = self.conf.xendpath
        
        if not xend:
            raise ProgrammingError("do not call if xendpath not configured")
        
        if xmsudo:
            listcmd = "%s %s list" % (sudo, xm)
        else:
            listcmd = "%s list" % xm
            
        try:
            (exit, stdout, stderr) = runexe(listcmd)
            result = "'%s': exit=%d, stdout='%s', " % (listcmd, exit, stdout)
            result += "stderr='%s'" % (stderr)
            if exit != 0:
                msg = "cannot contact xend: %s" % result
                log.error(msg)
            else:
                msg = "xend is alive, result: %s" % result
                if logerror:
                    log.error(msg)
                elif self.common.trace:
                    log.debug(msg)
                log.info("xend is alive")
                return stdout
                
            restartcmd = "%s %s restart" % (sudo, xend)
                
            (exit, stdout, stderr) = runexe(restartcmd, killtime=5.0)
            result = "'%s': exit=%d, stdout='%s'," % (restartcmd, exit, stdout)
            result += " stderr='%s'" % (stderr)
            if exit != 0:
                msg = "fatal: attempted to restart xend and failed, "
                msg += "result: %s" % result
                if logerror:
                    log.error(msg)
                raise UnexpectedError(msg)
            else:
                secs = self.conf.restart_xend_secs
                msg = "restarted xend: waiting %.3f seconds before " % secs
                msg += "next test"
                if logerror:
                    log.error(msg)
                else:
                    log.info(msg)
                time.sleep(secs)
                
                (exit, stdout, stderr) = runexe(listcmd)
                msg = "'%s': exit=%d, stdout='%s', " % (listcmd, exit, stdout)
                msg += "stderr='%s'" % (stderr)
                if logerror:
                    log.error(msg)
                else:
                    log.debug(msg)

                if exit != 0:
                    msg = "fatal: restarted xend but xm list fails again"
                    if stderr:
                        msg += ", stderr: %s" % stderr
                    if logerror:
                        log.error(msg)
                    raise UnexpectedError(msg)
                else:
                    msg = "restarted xend, running now, xm list "
                    msg += "output:\n%s" % stdout
                    if logerror:
                        log.error(msg)
                    else:
                        log.info(msg)
                    return stdout
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            msg = "unknown problem testing xm/xend: "
            msg += "%s: %s" % (str(exceptname), str(sys.exc_value))
            raise UnexpectedError(msg)
            
    def killAll(self):
        """Destroy all guest VMs.
        
        Return list of AKilledVM instances (list can be empty)
        
        Log everything to error (or critical) level.
        
        Currently suffers from check then act race.
        
        In the future this could iterate, checking back on the system to
        make sure it worked and if more VMs were started in the meantime.
        
        (fortunately when using VWS, in the situation where that is the only
        thing starting VMs it would have already gotten an error about this
        slot and not try to start more VMs)
        
        """
        
        log.error("Killing any existing VMs.")
        
        killedVMs = []
        allVMs = []
        currvms = self.currentVMs(logerror=True)
        for vm in currvms:
            # using AKilledVM class for all since it is convenient
            allVMs.append(AKilledVM(vm[0],vm[1],vm[2]))
        
        for vm in allVMs:
            if vm.vmid < 0:
                log.critical("problem parsing, ouch: %s" % vm)
            elif vm.vmid != 0:
                try:
                    self.killOne(vm.vmid)
                    killedVMs.append(vm)
                except:
                    exception_type = sys.exc_type
                    try:
                        excname = exception_type.__name__ 
                    except AttributeError:
                        excname = exception_type
                    msg = "problem killing VM: %s" % vm
                    msg += " | %s: %s" % (str(excname), str(sys.exc_value))
                    log.critical(msg)
        
        return killedVMs
        
    def currentVMs(self, logerror=False):
        """Retrieve current VM list.
        
        Keyword parameter:
        
        * logerror - log everything to error level (default False) for extreme
        error situations
        
        Return list of (vmid, name, memory) tuples (can be length zero).
        types: (int, string, int)
        
        Raise UnexpectedError if it is impossible to accomplish.
        
        """
        
        log.error("running currentVMs()")
        
        xmlist = None
        if self.conf.xendpath:
            xmlist = self.testXend(logerror=logerror)
        else:
            xmsudo = self.conf.xmsudo
            sudo = self.conf.sudopath
            xm = self.conf.xmpath
            
            if xmsudo:
                listcmd = "%s %s list" % (sudo, xm)
            else:
                listcmd = "%s list" % xm
                
            try:
                (exit, stdout, stderr) = runexe(listcmd)
                result = "exit=%d, stdout='%s', " % (exit, stdout)
                result += "stderr='%s'" % (stderr)
                if exit != 0:
                    log.error("PROBLEM: %s" % result)
                else:
                    xmlist = stdout
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                msg = "unknown problem running xm list: "
                msg += "%s: %s" % (str(exceptname), str(sys.exc_value))
                raise UnexpectedError(msg)
                
        if not xmlist:
            raise UnexpectedError("no xm list output")
            
        lines = xmlist.split('\n')
        if len(lines) < 2:
            # ouch  (xen-api ...)
            raise UnexpectedError("less than two lines in xm list output")
        
        namecol = -1
        idcol = -1
        memcol = -1
        cols = lines[0].split()
        for position,col in enumerate(cols):
            if col.lower().rfind("name") >= 0:
                namecol = position
            if col.lower().rfind("id") >= 0:
                idcol = position
            if col.lower().rfind("mem") >= 0:
                memcol = position
        
        for position in (namecol, idcol, memcol):
            if position == -1:
                # ouch
                raise UnexpectedError("information missing in xm list output")
        
        vmlist = []
        for line in lines[1:]:
            if not line:
                continue
            if logerror:
                log.error("parsing line '%s'" % line)
            elif self.common.trace:
                log.debug("parsing line '%s'" % line)
            cols = line.split()
            vmid = cols[idcol]
            try:
                vmid = int(vmid)
            except:
                log.error("vmid '%s' not an integer..." % vmid)
                vmid = -1
            name = cols[namecol]
            mem = cols[memcol]
            try:
                mem = int(mem)
            except:
                log.error("memory '%s' not an integer..." % mem)
                mem = -1
            vmlist.append((vmid, name, mem))
            
        return vmlist
        
    def killOne(self, vmid):
        """Destroy one guest VM.
        
        Required parameter:
        
        * vmid -- the vmid to kill
        
        Return nothing.
        
        Raise UnexpectedError if it is impossible to accomplish.
        
        Log everything to error level.
        
        """
        
        log.error("Killing vmid=%d" % vmid)
        
        cmd = "%s destroy %d" % (self.conf.xmpath, vmid)
        if self.conf.xmsudo:
            cmd = "%s %s" % (self.conf.sudopath, cmd)
        
        if self.common.evaluate:
            log.error("%s would have run '%s'" % (EP, cmd))
        else:
            log.error("running '%s'" % cmd)
            (exit, stdout, stderr) = runexe(cmd)
            msg = "'%s': exit=%d, stdout='%s', " % (cmd, exit, stdout)
            msg += "stderr='%s'" % (stderr)
            log.error(msg)
            if exit != 0:
                raise UnexpectedError(msg)
                
# }}} END: XIV. XenActions(StateChangeListener)
                
# ############################################################
# XV. FakeXenActions(XenActions)
# #########################################################{{{

class FakeXenActions(XenActions):

    """XenActions descendant for developers: can be adjusted to pretend it's a
    working and/or failing XenActions instance.  Wire in as a plugin manually. 
    Used for testing service notifications under error scenarios.

    Overrides XenActions methods:

    * initialize() -- don't run testXend.  Configure fake behaviors here.
        
    * evaluate() -- do nothing
    
    * reserving() -- either do nothing or throw an exception
    
    * unreserving() -- either do nothing or throw an exception

    """
    
    def initialize(self):
        if self.common.trace:
            log.debug("FakeXenActions.initialize()")
        
        # pick one of the errors in each faildict, or zero to pass
        
        self.reserving_failwith = 0
        self.reserving_faildict = {
              0:"just return ('success')",
              1:"fail with normal exception",
              2:"fail with normal exception with multiline msg" }
              
        self.unreserving_failwith = 0
        self.unreserving_faildict = {
              0:"just return ('success')",
              1:"fail with normal exception",
              2:"fail with normal exception with multiline msg",
              3:"fail with 1 killed VM VWS would know about",
              4:"fail with 1 killed VM VWS wouldn't know about",
              5:"fail with 3 killed VMs VWS would know about",
              6:"fail with 3 killed VMs VWS wouldn't know about",
              7:"fail with 1 killed VM VWS would know about + 1 it wouldn't" }
              
        self.initialized = True

    def evaluate(self, timeout=None):
        pass
    
    def reserving(self, timeout=None):
        if not self.initialized:
            raise ProgrammingError("not initialized")

        msg = self.reserving_faildict[self.reserving_failwith]
        log.debug("FakeXenActions.reserving() action: %s" % msg)
        
        if self.reserving_failwith == 0:
            return
        elif self.reserving_failwith == 1:
            raise UnexpectedError("unexpected error...")
        elif self.reserving_failwith == 2:
            raise UnexpectedError("unexpected error...\nxxx\nyyy\n")

    def unreserving(self, timeout=None):
        if not self.initialized:
            raise ProgrammingError("not initialized")
            
        msg = self.unreserving_faildict[self.unreserving_failwith]
        log.debug("FakeXenActions.unreserving() action: %s" % msg)
            
        if self.unreserving_failwith == 0:
            return
        method_name = "unreserving%d" % self.unreserving_failwith
        method = getattr(self, method_name, None)
        if not method:
            err = "%s not implemented by %s" % (method_name, self)
            raise ProgrammingError(err)
        method()

    def unreserving1(self):
        raise UnexpectedError("unexpected error...")

    def unreserving2(self):
        raise UnexpectedError("unexpected error...\nxxx\nyyy\n")

    def unreserving3(self):
        killedVMs = [AKilledVM(1,"workspace-1",512)]
        raise KilledVMs(killedVMs)

    def unreserving4(self):
        killedVMs = [AKilledVM(1,"bogusname",512)]
        raise KilledVMs(killedVMs)
        
    def unreserving5(self):
        killedVMs = []
        killedVMs.append(AKilledVM(2,"workspace-1",512))
        killedVMs.append(AKilledVM(18,"workspace-2",512))
        killedVMs.append(AKilledVM(3,"workspace-3",512))
        raise KilledVMs(killedVMs)

    def unreserving6(self):
        killedVMs = []
        killedVMs.append(AKilledVM(14,"bogusname-1",512))
        killedVMs.append(AKilledVM(2,"bogusname-2",512))
        killedVMs.append(AKilledVM(32,"bogusname-3",512))
        raise KilledVMs(killedVMs)

    def unreserving7(self):
        killedVMs = []
        killedVMs.append(AKilledVM(1,"workspace-1",512))
        killedVMs.append(AKilledVM(2,"bogusname",512))
        raise KilledVMs(killedVMs)

# }}} END: XV. FakeXenActions(XenActions)

# ############################################################
# XVI. XenKillNine(XenActions)
# #########################################################{{{

class XenKillNine(XenActions):

    """XenActions descendant that performs Xen kill-nine actions on the system.
    
    This is more implementation than type inheritance, will likely refactor
    XenActions and XenKillNine to use delegation in the future.

    Overrides XenActions methods:

    * initialize()
    
    * unreserving()
    
    """
    
    def initialize(self):
        """Perform initialization work.

        Overrides XenActions.initialize()

        Raise InvalidConfig if there is a problem with the supplied
        configuration object.

        Return nothing.

        Expects conf to be a XenConf instance.

        """

        if self.common.trace:
            log.debug("XenKillNine.initialize()")
            
        if self.conf.xendpath:
            # run even in evaluate mode, tests in evaluate() will run xm etc.
            self.testXend()

        self.initialized = True
        self.killninemode = True
        
    def unreserving(self, timeout=None):
        """Unreserving.  Adjust dom0 memory to its max (if max is configured)
        or alternatively give it as much memory as the VMs that will be killed
        used (if they exist) plus any free memory reported before the kills.

        Overrides XenActions.unreserving()

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.
        (causes errorUnreserving)

        Raise IncompatibleEnvironment if it is impossible to proceed.
        (causes errorUnreserving)
        
        Raise KilledVMs if there was a problem that was "taken care of".
        
        If raising KilledVMs is necessary and if it impossible to proceed
        later, log that to critical and still raise KilledVMs.
        
        Because this is run in times of a problem, most everything gets logged
        to INFO level.

        """
        
        if not self.initialized:
            raise ProgrammingError("not initialized")

        memory = self.conf.memory
        if memory == XenActionsConf.BESTEFFORT:
            log.info("XenKillNine unreserving, releasing as much as we can")
        else:
            log.info("XenKillNine unreserving, releasing %d MB" % memory)
            
        curmem = self.currentAllocation_MB()
        
        log.info("current memory MB = %d" % curmem)
        if memory != XenActionsConf.BESTEFFORT:
            log.info("target memory MB = %d " % memory)
            targetmem = memory
        else:
            log.info("target memory is best effort")
            freemem = self.currentFree_MB()
            log.info("best effort mode, current free memory MB = %d" % freemem)
            targetmem = freemem + curmem
        
        raiseme = None
        killedVMs = self.killAll()
        if killedVMs:
            raiseme = KilledVMs(killedVMs)
        
        if memory == XenActionsConf.BESTEFFORT:
            targetmem = freemem + curmem
            # see what was freed up, if anything
            if raiseme:
                for vm in raiseme.vms:
                    targetmem += vm.mem
        else:
            targetmem = memory
        
        curmem = self.currentAllocation_MB()
        if curmem != targetmem:
            # for test harness mostly
            if not raiseme:
                fakevm = AKilledVM(-34, "memorydifference", targetmem-curmem)
                raiseme = KilledVMs([fakevm])
        
        # assumes no VMs are started in the meantime
        try:
            self.memset(targetmem)
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            n = str(exceptname)
            err = str(sys.exc_value)
            errmsg = "Problem setting memory to %d: %s: %s" % (targetmem,n,err)
            if raiseme:
                log.critical(errmsg)
                raise raiseme
            else:
                raise UnexpectedError(errmsg)
        
        if raiseme:
            raise raiseme
    
# }}} END: XVI. XenKillNine(XenActions)

# ############################################################
# XVII. VWSNotifications(StateChangeListener)
# #########################################################{{{

class VWSNotifications(StateChangeListener):

    """StateChangeListener that makes notifications to the
    Virtual Workspace Service.
    
    Overrides StateChangeListener methods:

    * initialize() -- verifies contact string and sets up self.http_contact_url
    and/or self.notify_cmd
    
    * evaluate() -- logs known information and makes two test notifications
    
    * reserved() -- notifies

    * errorReserving() -- notifies (with error string if available)

    * unreserving() -- notifies
    
    * errorUnreserving() -- notifies (with error string if available)
    
    * earlyUnreserving() -- notifies
    
    * errorEarlyUnreserving() -- notifies (with error string if available)
    
    * killedVMsNotification() -- notifies with list of killed VM names
    
    Class variables house string constants VWS sees.
    
    """
    
    # strings VWS notification consumer sees
    PILOT_PREFIX = "pilot-"
    TEST = "test"
    RESERVED = "reserved"
    UNRESERVING = "unreserving"
    EARLY_UNRESERVING = "earlyunreserving"
    KILLED = "killed"
    
    ##############################
    # Overriden handler methods: #
    ##############################

    def initialize(self):
        """Perform initialization work.

        Overrides StateChangeListener.initialize()

        Raise InvalidConfig if there is a problem with the supplied
        configuration object.

        Return nothing.

        Expects conf to be a VWSNotificationsConf instance.

        """

        if self.common.trace:
            log.debug("VWSNotifications.initialize()")
        
        log.debug("slotid: %s" % self.conf.slotid)
        
        if not self.conf.contactstring:
            raise ProgrammingError("contact string is required")
        
        contact = self.conf.contactstring
        
        parts = contact.split("+++")
        if len(parts) > 1:
            log.debug("multiple contact targets: %s" % contact)
        else:
            log.debug("one contact target: %s" % contact)
            
        httpcontact = None
        sshcontact = None
        for part in parts:
            if part[:7] == "http://":
                httpcontact = part
            else:
                sshcontact = part
        
        if httpcontact and not self.conf.http_enabled:
            msg = "received HTTP contact but HTTP notifications are disabled"
            log.info(msg)
            
        self.http_contact_url = None
        if httpcontact and self.conf.http_enabled:
            log.debug("HTTP contact URL: %s" % httpcontact)
            if not self.conf.http_account:
                raise ProgrammingError("HTTP secret is not present")
            if self.common.trace:
                log.debug("HTTP account: %s" % self.conf.http_account)
                log.debug("HTTP secret is present")
                
            self.http_contact_url = httpcontact
            
        if sshcontact and not self.conf.ssh_enabled:
            msg = "received SSH contact (or something else) but SSH "
            msg += "notifications are disabled"
            log.info(msg)
            
        self.notify_cmd = None
        if sshcontact and self.conf.ssh_enabled:
            
            if sshcontact.rfind(":") < 0:
                raise InvalidConfig("ssh contact string is invalid (no ':')")
                
            if sshcontact.rfind("/") < 0:
                raise InvalidConfig("ssh contact string is invalid (no '/')")
            
            i1 = string.find(sshcontact, ':')
            userhost = sshcontact[:i1]
            i2 = string.find(sshcontact, '/')
            port = sshcontact[i1+1:i2]
            path = sshcontact[i2:]
            
            if len(path) <= 1:
                msg = "path '%s' in ssh contact string is a single '/'"  % path
                msg += " not an absolute path to the notifications exe"
                raise InvalidConfig(msg)
            
            try:
                testint = int(port)
            except:
                msg = "port '%s' in contact string is not an integer" % port
                raise InvalidConfig(msg)
            
            sshargs = ""
            if self.conf.key_path:
                # if key_path config exists, there is no fallback to default
                # key: see if file exists or fail - see permissions note in
                # evaluate()
                checkabsexists(self.conf.key_path, "ssh credential")
                sshargs = "-i %s " % self.conf.key_path
            sshargs += "-p " + port + ' ' + userhost
            
            if self.common.trace:
                log.debug("ssh path: %s" % self.conf.ssh_path)
                log.debug("ssh host or user+host: %s" % userhost)
                log.debug("ssh port: %s" % port)
                log.debug("ssh notif path: %s" % path)
                log.debug("ssh args: %s" % sshargs)
            
            self.notify_cmd = self.conf.ssh_path + ' '
            self.notify_cmd += sshargs + ' ' + path
        
            log.debug("SSH notifications cmd: %s" % self.notify_cmd)
            
        self.initialized = True
        
    def evaluate(self, timeout=None):
        """Optionally run extra tests for when program is in evaluate mode.
        
        Test the notification mechanism.

        Raise IncompatibleEnvironment if there is a problem with the
        configurations/system.

        Return nothing.

        """
        
        if not self.initialized:
            raise ProgrammingError("not initialized")
        
        slotid = self.conf.slotid
        log.info("%s slotid: %s" % (EP, slotid))
        
        contact = self.conf.contactstring
        log.info("%s contact string configuration: %s" % (EP, contact))
        
        hostname = self.conf.hostname
        log.info("%s hostname configuration: %s" % (EP, hostname))
        
        
        log.info("%s http account configuration: %s" % (EP, ssh))
        if not self.conf.http_enabled:
            log.info("%s HTTP notifications disabled" % EP)
        else:
            log.info("%s HTTP contact URL: %s" % (EP, self.http_contact_url))
            
        ssh = self.conf.ssh_path
        keypath = self.conf.key_path
        
        log.info("%s ssh path configuration: %s" % (EP, ssh))
        if not self.conf.ssh_enabled:
            log.info("%s SSH notifications disabled" % EP)
        else:
            if keypath:
                log.info("%s ssh key override: %s" % (EP, keypath))
            else:
                log.info("%s ssh key override not configured" % EP)

            log.info("%s parsed notify command: %s" % (EP, self.notify_cmd))
        
        # SSH exe may simply be on PATH, cannot validate anything w/o testing
        
        # sshcontact string and keypath are evaluated every call in initialize
        
        # Permission problem with key will make ssh fail when run. With 
        # good reason, though SSH does not check up on the enclosing
        # directories.
        # For this iteration we don't do that either, only checking for 
        # 600 via SSH test command so nothing to do right now.
        
        
        # make a test call, VWS service will ignore this notification
        self.notifyService(self.TEST, 0, testing=True, timeout=timeout)
        
        errmsg = "test error message line 1\nline 2\n"
        self.notifyService(self.TEST, 1, extrastr=errmsg, testing=True, timeout=timeout)
        
    def reserved(self, timeout=None):
        """Reserved successfully.

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """

        if self.common.trace:
            log.debug("VWSNotifications.reserved()")

        if not self.initialized:
            raise ProgrammingError("not initialized")
            
        # timestamp: YYYY-MM-DD-HH-MM-SS  (always UTC/GMT)
        # (UTC/GMT differences are insignificant for our purposes)
        message = time.strftime("%Y-%m-%d-%H-%M-%S", time.gmtime())
        
        self.notifyService(self.RESERVED, 0, extrastr=message, timeout=timeout)
        
    def errorReserving(self, errstr=None, timeout=None):
        """Action or a listener had an error during reserving.  Alert VWS.

        Keyword parameter:
        
        * errstr -- error messages received so far, if applicable
        
        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """

        if self.common.trace:
            log.debug("VWSNotifications.errorReserving(), errstr: %s" % errstr)
        
        if not self.initialized:
            raise ProgrammingError("not initialized")
            
        if errstr:
            errstr = "Pilot had a problem reserving: %s" % errstr
            
        self.notifyService(self.RESERVED, 1, extrastr=errstr, timeout=timeout)
            
    def unreserving(self, timeout=None):
        """Unreserving.  Alert VWS.

        Overrides StateChangeListener.unreserving()

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.
        (causes errorUnreserving)

        Raise IncompatibleEnvironment if it is impossible to proceed.
        (causes errorUnreserving)

        """

        if self.common.trace:
            log.debug("VWSNotifications.unreserving()")

        if not self.initialized:
            raise ProgrammingError("not initialized")
            
        self.notifyService(self.UNRESERVING, 0, timeout=timeout)
        
    def errorUnreserving(self, errstr=None, timeout=None):
        """Action or a listener had an error during unreserving.

        Keyword parameter:
        
        * errstr -- error messages received so far, if applicable
        
        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """
        
        if self.common.trace:
            msg = "VWSNotifications.errorUnreserving(), errstr = %s" % errstr
            log.debug(msg)

        if not self.initialized:
            raise ProgrammingError("not initialized")

        if errstr:
            errstr = "Pilot had a problem unreserving: %s" % errstr
            
        self.notifyService(self.UNRESERVING, 1, extrastr=errstr, timeout=timeout)

    def earlyUnreserving(self, timeout=None):
        """Early unreserving.  Alert VWS.

        Overrides StateChangeListener.earlyUnreserving()

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.
        (causes errorEarlyUnreserving)

        Raise IncompatibleEnvironment if it is impossible to proceed.
        (causes errorEarlyUnreserving)

        """

        if self.common.trace:
            log.debug("VWSNotifications.earlyUnreserving()")

        if not self.initialized:
            raise ProgrammingError("not initialized")
            
        # timestamp: YYYY-MM-DD-HH-MM-SS  (always UTC/GMT)
        # (UTC/GMT differences are insignificant for our purposes)
        message = time.strftime("%Y-%m-%d-%H-%M-%S", time.gmtime())
        
        self.notifyService(self.EARLY_UNRESERVING, 0, extrastr=message, timeout=timeout)
        
    def errorEarlyUnreserving(self, errstr=None, timeout=None):
        """Action or a listener had an error during early unreserving.
        
        Keyword parameter:
        
        * errstr -- error messages received so far, if applicable

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """
        
        if self.common.trace:
            msg = "VWSNotifications.errorEarlyUnreserving(), "
            msg += "errstr = %s" % errstr
            log.debug(msg)

        if not self.initialized:
            raise ProgrammingError("not initialized")

        if errstr:
            errstr = "Pilot had a problem earlyUnreserving: %s" % errstr
            
        self.notifyService(self.EARLY_UNRESERVING, 1, extrastr=errstr, timeout=timeout)
            
    def killedVMsNotification(self, obj=None, errstr=None, timeout=None):
        """VMs were killed.
        
        Keyword parameters:
        
        * obj -- object with information (a list of KilledVMs exceptions).
        
        * errstr -- error messages received so far, if applicable

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """

        if self.common.trace:
            msg = "VWSNotifications.killedVMsNotification() obj=%s, " % obj
            msg += "errstr = %s" % errstr
            log.debug(msg)
        
        if not isinstance(obj, list):
            raise ProgrammingError("given object is not a list")
            
        killedVMs = obj
            
        if len(killedVMs) == 0:
            raise ProgrammingError("killedVMs is an empty list")
            
        # flatten
        allkilled = []
        count = 0
        msg = ""
        sendstr = ""
        for killed in killedVMs:
            count += 1
            if not isinstance(killed, KilledVMs):
                msg = "something in killedVMs is not a KilledVMs instance: "
                msg += "%s" % killed
                raise ProgrammingError(msg)
            else:
                for vm in killed.vms:
                    msg += "| #%d: %s " % (count,vm)
                    allkilled.append(vm)
                    sendstr += "%s," % vm.name
        # remove hanging comma for good measure
        sendstr = sendstr[:-1]
                    
        msg2 = "Found %d killed VMs to notify VWS about: " % len(allkilled)
        log.info("%s%s" % (msg2,msg))
        
        if errstr:
            msg = "not sending extra error information to VWS: %s" % errstr
            log.error(msg)
        
        self.notifyService(self.KILLED, 0, extrastr=sendstr, timeout=timeout)
        
    ####################
    # Utility methods: #
    ####################
    
    def notifyService(self, state, exitcode, 
                      extrastr=None, testing=False, timeout=None):
        """Send a notification.
        
        sendid = self.conf.slotid + "+++" + self.conf.hostname
        
        If using HTTP POST (URL @ self.http_contact_url):
            $sendid 
            $state 
            $exitcode 
            [$extrastr]
        
        If using SSH:
            Command syntax (exe parameters to self.notify_cmd):
                write $sendid $state $exitcode [$extrastr]
        
        Required parameters:
        
        * state -- event/state causing the notification, not necessarily the
        name of the states in StateChangeListener.
        VWS considers e.g. "reserved" with 0 exit code to mean "reserved" and
        "reserved" with non-0 exit code to mean "errorReserving".
        
        * exitcode -- integer exit/result code, VWS doesn't do anything besides
        distinguish between 0 and non-0
        
        Keyword parameters:
        
        * extrastr -- a message to pass to VWS, could be information or errors
        
        * testing -- boolean to trigger the method to send the notification
        even if the program is in evaluate mode and just log what "would have
        happened".  This is here just for test notifications during an evaluate
        run.
        
        * timeout -- if not None, float to trigger killtime
        
        Raise UnexpectedError if it is impossible to proceed.
        
        """
        
        if not self.notify_cmd and not self.http_contact_url:
            raise ProgrammingError("notify target missing, not initialized")
            
        if not timeout:
            # even if timeout is not requested, have to stop it at some point,
            # 20 seconds is an inordinate amt of time
            timeout=20.0
        
        sendstr = ""
        if exitcode:
            sendstr = "PILOT ERROR "
            if extrastr:
                # remove newlines, replace with another token
                lines = extrastr.splitlines()
                a = lambda x: x + " ]eol[ "
                sendstr += ''.join(map(a, lines))
                sendstr = "'" + sendstr + "'"
            else:
                sendstr += "No error output is available?"
                
        if state == self.RESERVED and exitcode == 0:
            if not extrastr:
                raise ProgrammingError("reserved requires timestamp")
            # assuming no newlines
            sendstr = extrastr
            
        sendid = self.conf.slotid + "+++" + self.conf.hostname
            
        if state == self.KILLED:
            if not extrastr:
                raise ProgrammingError("killed requires kill list")
            # assuming no newlines
            sendstr = extrastr
            
        if state == self.EARLY_UNRESERVING and exitcode == 0:
            if not extrastr:
                raise ProgrammingError("earlyunreserving requires timestamp")
            # assuming no newlines
            sendstr = extrastr

        if sendstr:
            if self.common.trace:
                log.debug("sendstr = %s" % sendstr)
            sendstr = bashEscape(sendstr)
            if self.common.trace:
                log.debug("escaped sendstr = %s" % sendstr)

        state = self.PILOT_PREFIX + state
        
        # three results from notifyWith___ methods: True/False/Exception
        
        httpOK = False
        sshOK = False
        try:
            httpOK = self.notifyWithHTTP(sendid, state, exitcode, sendstr, timeout=timeout)
        except:
            log.exception("Problem running HTTP notfication")
        
        if not httpOK:
            try:
                sshOK = self.notifyWithCMD(sendid, state, exitcode, sendstr, timeout=timeout)
            except:
                log.exception("Problem running SSH notfication")
                
        if not httpOK and not sshOK:
            raise UnexpectedError("No notifications succeeded.")
            
    def notifyWithHTTP(self, sendid, state, exitcode, sendstr, timeout=None):
        if not self.http_contact_url:
            log.debug("HTTP handler not running")
            return False
            
        needtimestamp = False
        if state == self.PILOT_PREFIX + self.RESERVED:
            needtimestamp = exitcode == 0
            
        writelines = ""
        writelines += str(sendid)
        writelines += "\n"
        writelines += str(state)
        writelines += "\n"
        writelines += str(exitcode)
        writelines += "\n"
        if sendstr:
            writelines += str(sendstr)
            writelines += "\n"
            
        passmgr = urllib2.HTTPPasswordMgrWithDefaultRealm()
        
        url = self.http_contact_url
        account = self.conf.http_account
        pw = self.conf.http_secret
        passmgr.add_password(None, url, account, pw)
        
        authhandler = urllib2.HTTPDigestAuthHandler(passmgr)
        
        opener = urllib2.build_opener(authhandler)
        urllib2.install_opener(opener)
        
        if needtimestamp:
            persistent_timestamp("PILOT4")
        pagehandle = urllib2.urlopen(url, writelines)
        if needtimestamp:
            persistent_timestamp("PILOT5")
            write_persistent_timestamps()
        
        lines =  pagehandle.readlines()
        log.debug("HTTP notification result: %s" % lines)
        return True
        
    def notifyWithCMD(self, sendid, state, exitcode, sendstr, timeout=None):
        if not self.notify_cmd:
            log.debug("no configured notification cmd")
            return False
            
        exeargs = ['write', sendid, state, str(exitcode)]
        if sendstr:
            exeargs.append(sendstr)

        argumentstr = ' '.join(exeargs)
        
        cmd =  self.notify_cmd + ' ' + argumentstr
              
        if self.common.evaluate and not testing:
            log.info("%s would have run VWS notification: '%s'" % (EP, cmd))
            return
            
        log.debug("notification command: '%s'" % cmd)
            
        (exit, stdout, stderr) = runexe(cmd, killtime=timeout)
        msg = "'%s': exit=%d, stdout='%s', " % (cmd, exit, stdout)
        msg += "stderr='%s'" % (stderr)
        
        if exit != 0:
            msg = "Problem w/ notif cmd: %s" % msg
            log.error(msg)
            raise UnexpectedError(msg)
        else:
            if self.common.trace:
                log.debug(msg)
            log.info("VWS command based (SSH) notification successful")
            return True
            
# }}} END: XVII. VWSNotifications(StateChangeListener)

# ############################################################
# XVIII. Configuration objects
# #########################################################{{{

# These are objects that are populated from a combination of
# commandline and config file settings.  Their fields are set
# to control the behavior of the action and listener objects.

class CommonConf:

    """Class for configurations common to all actions and objects."""

    def __init__(self, trace, evaluate):
        """Set the configurations.

        Required parameters:

        * trace -- Make extra log statements to DEBUG level (boolean).

        * evaluate -- "Dryrun" mode (boolean).

        """

        self.trace = trace
        self.evaluate = evaluate

class ReserveSlotConf:

    """Class for reserve-slot configurations."""

    def __init__(self, duration, graceperiod, earlywaitratio):
        """Set the configurations.

        Required parameters:

        * duration -- The expected wall time of the slot.

        * graceperiod -- The grace period between SIGTERM and SIGKILL.
        
        * earlywaitratio -- Ratio of grace period to wait on earlyUnreserve
        before causing a (forced, usually) unreserve.  Gives listeners a
        chance to respond.  For example VWS will call shutdown-trash in the
        meantime.  Must be 0 or float between 0 and 1 (but not 1 itself).

        Raise InvalidConfig if there is a problem with parameters.

        """

        if duration == None:
            raise InvalidConfig("duration is required")
        if graceperiod == None:
            raise InvalidConfig("graceperiod is required")

        try:
            duration = int(duration)
        except:
            raise InvalidConfig("duration (s) is required to be an integer")

        try:
            graceperiod = int(graceperiod)
        except:
            raise InvalidConfig("graceperiod (s) is required to be an integer")

        if duration < 1:
            raise InvalidConfig("duration must be > 0 seconds")

        if graceperiod < 0:
            raise InvalidConfig("graceperiod must be >= 0 seconds")
            
        self.duration = duration
        self.graceperiod = graceperiod
        self.horizon = duration
            
        errmsg = "earlywaitratio is required to be a number: zero or between "
        errmsg += "zero and one (but not one itself)."
        try:
            earlywaitratio = float(earlywaitratio)
        except:
            raise InvalidConfig(errmsg)
        if earlywaitratio < 0 or earlywaitratio >= 1.0:
            raise InvalidConfig(errmsg)
        self.earlywaitratio = earlywaitratio

        log.debug("reserve-slot duration: %ds" % self.duration)
        log.debug("reserve-slot grace period: %ds" % self.graceperiod)
        log.debug("reserve-slot earlywaitratio: %.5f" % self.earlywaitratio)

class XenActionsConf:

    """Class for XenActions configurations."""
    
    BESTEFFORT = "BESTEFFORT"

    def __init__(self, xmpath, xendpath, xmsudo, sudopath, memory, minmem, xend_secs):
        """Set the configurations.

        Required parameters:

        * xmpath -- Path to xm.
        
        * xendpath -- Path to xend.  If None, xend restart option is disabled.

        * xmsudo -- Use sudo with xm commands
        
        * sudopath -- Path to sudo.

        * memory -- The amount of memory to make free.  If set to "BESTEFFORT"
        a kill-nine operation will restore as much as it finds in the guests
        that are killed.
        
        * minmem -- The minimum memory to allow dom0 to reach (ignored by
        the kill-nine operation).
        
        * xend_secs -- If xendpath is configured, amount of time to
        wait after a restart before checking if it booted.
        
        Raise InvalidConfig if there is a problem with parameters.

        """

        self.xmpath = xmpath
        self.xmsudo = xmsudo
        self.sudopath = sudopath
        self.xendpath = xendpath

        if memory == None:
            raise InvalidConfig("memory is required")
            
        if memory != self.BESTEFFORT:
            try:
                memory = int(memory)
            except:
                raise InvalidConfig("memory (MB) is required to be an integer")
    
            if memory < 1:
                raise InvalidConfig("memory must be > 0 MB")
    
            log.debug("memory: %dM" % memory)
        self.memory = memory
    
        try:
            minmem = int(minmem)
        except:
            raise InvalidConfig("minmem not an integer")
            
        if minmem < 0:
            raise InvalidConfig("minmem is negative")
            
        if memory != self.BESTEFFORT:
            log.debug("minmem: %dM" % minmem)
        self.minmem = minmem
        
        try:
            restart_xend_secs = float(xend_secs)
        except:
            raise InvalidConfig("restart_xend_secs required to be a number")
        log.debug("restart_xend_secs: %.3f" % restart_xend_secs)
        self.restart_xend_secs = restart_xend_secs

class VWSNotificationsConf:

    """Class for VWSNotifications configurations."""

    def __init__(self, slotid, contactstring, hostname,
                 http_enabled, ssh_enabled, 
                 http_account=None, http_secret=None, 
                 ssh_path=None, key_path=None):
        """Set the configurations.

        Required parameters:
        
        * slotid -- The given slot ID, all notifications include this
        (string, usually UUID).

        * contactstring -- Supplied contact string for notifications.
        
        * hostname -- The hostname to use as part of reserved notification.

        * http_enabled -- True if HTTP POST notifications are possible
        
        * ssh_enabled -- True if SSH notifications are possible

        Keyword parameters:
            
        * http_account -- account for HTTP POST
            
        * http_secret -- shared secret for HTTP POST
            
        * ssh_path -- Path to SSH.

        * key_path -- Path to SSH key.  If None (the default value), account's
        default will be used.

        Raise InvalidConfig if there is a problem with parameters.

        """
        
        if not http_enabled and not ssh_enabled:
            raise InvalidConfig("no notification mechanism is enabled")

        if not id:
            raise InvalidConfig("no slotid")
        if not contactstring:
            raise InvalidConfig("no contactstring")
        if not hostname:
            raise InvalidConfig("no hostname")
            
        if http_enabled:
            if not http_account:
                raise InvalidConfig("http_enabled but no http_account")
            if not http_secret:
                raise InvalidConfig("http_enabled but no http_secret")
                
        if ssh_enabled:
            if not ssh_path:
                raise InvalidConfig("ssh_enabled but no ssh_path")
            # OK if key_path does not exist

        # contactstring is parsed/validated in VWSNotifications.initialize()
        self.slotid = slotid
        self.contactstring = contactstring
        self.hostname = hostname
        
        self.http_enabled = http_enabled
        self.ssh_enabled = ssh_enabled
        self.http_account = http_account
        self.http_secret = http_secret
        self.ssh_path = ssh_path
        self.key_path = key_path

# }}} END: XVIII. Configuration objects

# ############################################################
# XIX. Convert configurations
# #########################################################{{{

def getCommonConf(opts, config):
    """Return populated CommonConf object or raise InvalidConfig.

    Required parameters:

    * opts -- parsed optparse opts

    * config -- parsed ConfigParser

    """

    if not opts:
        raise ProgrammingError("opts is None")
    if not config:
        raise ProgrammingError("config is None")

    return CommonConf(opts.trace, opts.evaluate)

def getReserveSlotConf(opts, config):
    """Return populated ReserveSlotConf object or raise InvalidConfig

    Required parameters:

    * opts -- parsed optparse opts

    * config -- parsed ConfigParser
    
    Raise InvalidConfig if there is a problem.

    """

    if not opts:
        raise ProgrammingError("opts is None")
    if not config:
        raise ProgrammingError("config is None")
        
    try:
        earlywaitratio = config.get("other", "earlywaitratio")
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        msg = "%s: %s" % (str(exceptname), str(sys.exc_value))
        raise InvalidConfig(msg)

    return ReserveSlotConf(opts.duration, opts.graceperiod, earlywaitratio)

def getXenActionsConf(opts, config):
    """Return populated XenActionsConf object or raise InvalidConfig

    Required parameters:

    * opts -- parsed optparse opts

    * config -- parsed ConfigParser
    
    Raise InvalidConfig if there is a problem.

    """

    if not opts:
        raise ProgrammingError("opts is None")
    if not config:
        raise ProgrammingError("config is None")
    
    xmsudo = False
    try:
        xmsudo_val = config.get("xen", "xmsudo")
        if xmsudo_val:
            if xmsudo_val.lower() == 'yes':
                xmsudo = True
    except Exception, e:
        msg = "Problem finding xmsudo config, ASSUMING 'no do not use sudo "
        msg += "with xm commands': '%s'" % e
        log.info(msg)

    try:
        xm = config.get("xen", "xm")
        minmem = config.get("xen", "minmem")
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        msg = "%s: %s" % (str(exceptname), str(sys.exc_value))
        raise InvalidConfig(msg)
        
    # optional
    xend = None
    try:
        xend = config.get("xen", "xend")
    except Exception, e:
        log.debug("problem finding xend path: %s" % e)
        
    sudo = None
    # TODO
    #if xmsudo or xend:
    try:
        sudo = config.get("systempaths", "sudo")
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        emsg = "%s: %s" % (str(exceptname), str(sys.exc_value))
        
        msg = "One or both of the 'xmsudo' or 'xend' configurations are "
        msg += "enabled but there was a problem finding the sudo "
        msg += "path: '%s'" % emsg
        raise InvalidConfig(msg)
    
    xend_secs = -1
    if xend:
        try:
            xend_secs = config.get("xen", "restart_xend_secs")
        except Exception, e:
            defaultsec = RESTART_XEND_SECONDS_DEFAULT
            log.debug("problem finding restart_xend_secs (defaulting it "
                      "to %.3f seconds): %s" % (defaultsec, e))
            xend_secs = defaultsec
            
        try:
            xend_secs = float(xend_secs)
        except:
            msg = "restart_xend_secs ('%s') is not a number" % xend_secs
            raise InvalidConfig(msg)
            
    if not opts.killnine:
        return XenActionsConf(xm, xend, xmsudo, sudo, opts.memory, minmem, xend_secs)
    else:
        alt = "going to kill all guest VMs (if they exist) and give dom0 "
        alt += "their memory (which may or may not be the maximum available) "
        alt += "as well as any free memory reported to exist before the kills."
        dom0mem = None
        minmem = 0 # ignored by killnine
        try:
            dom0mem = config.get("xen", "dom0_mem")
        except:
            if opts.trace:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                msg = "%s: %s" % (str(exceptname), str(sys.exc_value))
                log.debug(msg)
            log.info("could not find dom0_mem configuration, %s" % alt)
            dom0mem = XenActionsConf.BESTEFFORT
        else:
            try:
                dom0mem = int(dom0mem)
            except:
                msg = "dom0_mem configuration '%s' is not an integer" % dom0mem
                log.info(msg + ", %s" % alt)
                dom0mem = XenActionsConf.BESTEFFORT
                
        return XenActionsConf(xm, xend, xmsudo, sudo, dom0mem, minmem, xend_secs)

def getVWSNotificationsConf(opts, config):
    """Return populated VWSNotificationsConf object or raise InvalidConfig

    Required parameters:

    * opts -- parsed optparse opts

    * config -- parsed ConfigParser
    
    Raise InvalidConfig if there is a problem.

    """

    if not opts:
        raise ProgrammingError("opts is None")
    if not config:
        raise ProgrammingError("config is None")

    if not opts.contact:
        raise InvalidConfig("contact string is None")

    contact = opts.contact
    
    if not opts.slotid:
        raise InvalidConfig("slotid is None")
        
    sid = opts.slotid

    forcehostname = None
    
    try:
        forcehostname = config.get("other", "forcehostname")
    except:
        pass
    
    if forcehostname:
        hoststr = forcehostname
        log.debug("using hostname '%s' (retrieved from config)" % hoststr)
    else:
        try:
            hoststr = socket.gethostname()
            hoststr = hoststr.split('.')[0] # simulate hostname -s
        except Exception, e:
            raise InvalidConfig(e.msg)
        log.debug("using hostname '%s' (retrieved from syscall)" % hoststr)

    # Mechanisms:

    http_enabled = False
    http_account = None
    http_secret = None
                
    try:
        http_account = config.get("http", "account")
        http_enabled = True
    except Exception, e:
        log.debug("No account in [http] section, HTTP notifications disabled")
        pass
    
    if http_enabled:
        log.debug("HTTP notifications are possible")
        try:
            http_secret = config.get("http", "secret")
            log.debug("Found HTTP secret")
        except:
            msg = "HTTP notifications enabled but no [http]-->secret config"
            raise InvalidConfig(msg)

    ssh_enabled = False
    ssh_path = None
    key_path = None
    
    try:
        ssh_path = config.get("systempaths", "ssh")
        ssh_enabled = True
    except Exception, e:
        log.debug("No sshpath, ssh notifications disabled")
        pass
    
    if ssh_enabled:
        log.debug("SSH notifications are possible")
        try:
            key_path = config.get("other", "sshcredential")
            log.debug("using non-default ssh credential: %s" % sshcred)
        except:
            pass # OK, will use system's default

    return VWSNotificationsConf(sid, contact, hoststr, 
                                http_enabled, ssh_enabled,
                                http_account=http_account,
                                http_secret=http_secret,
                                ssh_path=ssh_path,
                                key_path=key_path)

# }}} END: XIX. Convert configurations

# ############################################################
# XX. External configuration
# #########################################################{{{

class FakeFile:

    """Class for feeding a string to an API needing a file-like object.

    Only the readline() method is implemented.

    """

    def __init__(self, string):

        """Instantiate class with string.

        Required arguments:

        * string -- String to treat as file-like object, can contain newlines

        """

        if string is None:
            raise InvalidConfig("config file (string) can not be None")

        self.lines = string.splitlines(True)
        self.gen = self.genline()

    def readline(self):
        """Return the next line in the file until EOF (then return None)."""
        try:
            ret = self.gen.next()
        except StopIteration:
            return None
        return ret

    def genline(self):
        """Return the next line in the list until the end (StopIteration)."""
        for line in self.lines:
            yield line

def getconfig(string=None, filepath=None):
    """Return SafeConfigParser instantiated from supplied config source.

    Keyword arguments:

    * string -- String containing the text of the config file (with newlines)

    * filepath -- Path to a config file

    One (and not both) of these keyword arguments must be supplied.

    Raise InvalidConfig if there is a problem.

    """

    if not string and not filepath:
        raise InvalidConfig("neither string nor filepath was supplied "
                            "to getconfig()")
    if string and filepath:
        raise InvalidConfig("both string and filepath were supplied "
                            "to getconfig()")

    config = ConfigParser.SafeConfigParser()
    if string:
        config.readfp(FakeFile(string))
    if filepath:
        config.read(filepath)
    return config

# }}} END: XX. External configuration

# ############################################################
# XXI. Commandline arguments
# #########################################################{{{

class ARGS:

    """Class for command-line argument constants"""

    RES_SLOT_LONG="--reserveslot"
    RES_SLOT="-r"
    RES_SLOT_HELP="Reserve a slot (see documentation)."
    
    KILL9_LONG="--killnine"
    KILL9="-k"
    KILL9_HELP="Immediately destroy all guest VMs and back out their \
slots.  The target dom0 memory for this should be set in the configuration \
file (by default, the configuration file is embedded at top of this program). \
If not, dom0 is given the aggregate memory of the currently running VMs (the \
VMs that are getting killed) plus its current memory and any memory reported \
free before the kills.  See documentation for how to find dom0's max memory."
    
    MEMORY_LONG="--memory"
    MEMORY="-m"
    MEMORY_HELP="The number of MB to reserve."
    
    DURATION_LONG="--duration"
    DURATION="-d"
    DURATION_HELP="The number of seconds to stay alive."
    
    GRACE_LONG="--graceperiod"
    GRACE="-g"
    GRACE_HELP="The number of (expected) seconds this program has left to \
operate after receiving a SIGTERM before it receives SIGKILL."
    
    SLOTID_LONG="--slotid"
    SLOTID="-i"
    SLOTID_HELP="The slot ID, typically a UUID."
    
    CONTACT_LONG="--contact"
    CONTACT="-c"
    CONTACT_HELP="The contact string for notification-based plugins.  \
If left out, no notifications will be sent.  Format is plugin specific \
(see API or documentation)."
    
    CONFIGPATH_LONG="--configpath"
    CONFIGPATH="-p"
    CONFIGPATH_HELP="Path to configuration file that overrides the defaults. \
If there is a problem with the supplied config file, the program will NOT \
fall back to the defaults."
    
    STDOUTLOG_LONG="--stdoutlog"
    STDOUTLOG="-s"
    STDOUTLOG_HELP="log messages to stdout (use with caution under LRM)"
    
    EVALUATE_LONG="--evaluate"
    EVALUATE="-e"
    EVALUATE_HELP="Triggers the program to continue as normal but not \
actually effect any change, instead reporting on what would have happened.  \
It is allowed to run external, non-invasive testing commands."
    
    TRACE_LONG="--trace"
    TRACE="-t"
    TRACE_HELP="log all debug messages"
    
    VERBOSE_LONG="--verbose"
    VERBOSE="-v"
    VERBOSE_HELP="log debug messages"
    
    QUIET_LONG="--quiet"
    QUIET="-q"
    QUIET_HELP="don't log any messages (unless error occurs)."

def parsersetup():
    """Return configured command-line parser."""

    ver="Workspace Pilot %s - http://www.nimbusproject.org" % VERSION
    usage="see help (-h)."
    parser = optparse.OptionParser(version=ver, usage=usage)

    # ----

    group = optparse.OptionGroup(parser,  "Output options", "-------------")

    group.add_option(ARGS.QUIET, ARGS.QUIET_LONG,
                      action="store_true", dest="quiet", default=False,
                      help=ARGS.QUIET_HELP)

    group.add_option(ARGS.VERBOSE, ARGS.VERBOSE_LONG,
                      action="store_true", dest="verbose", default=False, 
                      help=ARGS.VERBOSE_HELP)

    group.add_option(ARGS.TRACE, ARGS.TRACE_LONG,
                      action="store_true", dest="trace", default=False,
                      help=ARGS.TRACE_HELP)
    
    group.add_option(ARGS.STDOUTLOG, ARGS.STDOUTLOG_LONG,
                      action="store_true", dest="stdoutlog", default=False,
                      help=ARGS.STDOUTLOG_HELP)

    parser.add_option_group(group)

    # ----

    group = optparse.OptionGroup(parser,
                                 "Required action, one is required",
                                 "-------------------------------")

    group.add_option(ARGS.RES_SLOT, ARGS.RES_SLOT_LONG,
                     action="store_true", dest="reserveslot", default=False,
                     help=ARGS.RES_SLOT_HELP)

    group.add_option(ARGS.KILL9, ARGS.KILL9_LONG,
                     action="store_true", dest="killnine", default=False,
                     help=ARGS.KILL9_HELP)

    parser.add_option_group(group)

    # ----

    group = optparse.OptionGroup(parser,
                           "Required arguments for %s" % ARGS.RES_SLOT_LONG,
                           "---------------------------------")

    group.add_option(ARGS.MEMORY, ARGS.MEMORY_LONG,
                     dest="memory", metavar="MB",
                     help=ARGS.MEMORY_HELP)

    group.add_option(ARGS.DURATION, ARGS.DURATION_LONG,
                     dest="duration", metavar="SECONDS",
                     help=ARGS.DURATION_HELP)

    group.add_option(ARGS.GRACE, ARGS.GRACE_LONG,
                     dest="graceperiod", metavar="SECONDS",
                     help=ARGS.GRACE_HELP)
                          
    group.add_option(ARGS.SLOTID, ARGS.SLOTID_LONG,
                     dest="slotid", metavar="UUID",
                     help=ARGS.SLOTID_HELP)

    parser.add_option_group(group)

    # ----

    group = optparse.OptionGroup(parser,
                           "Optional arguments for %s" % ARGS.RES_SLOT_LONG,
                           "------------------------------------------------")

    group.add_option(ARGS.CONTACT, ARGS.CONTACT_LONG,
                     dest="contact", metavar="CONTACTSTRING",
                     help=ARGS.CONTACT_HELP)

    parser.add_option_group(group)

    # ----

    group = optparse.OptionGroup(parser,
                           "Optional arguments for %s or %s" %
                                         (ARGS.RES_SLOT_LONG, ARGS.KILL9_LONG),
                           "------------------------------------------------")
                            
    group.add_option(ARGS.CONFIGPATH, ARGS.CONFIGPATH_LONG,
                    dest="configpath", metavar="PATH",
                    help=ARGS.CONFIGPATH_HELP)

    group.add_option(ARGS.EVALUATE, ARGS.EVALUATE_LONG,
                    action="store_true", dest="evaluate", default=False,
                    help=ARGS.EVALUATE_HELP)

    parser.add_option_group(group)
    
    return parser

def validateargs(opts):
    """Validate command-line argument combination.

    Required arguments:

    * opts -- Parsed optparse opts object

    Raise InvalidInput if there is a problem.

    """

    actions = [opts.reserveslot, opts.killnine]

    count = 0
    for action in actions:
        if action:
            count += 1

    seeh = "see help (-h)"

    if not count:
        raise InvalidInput("You must supply an action, %s." % seeh)

    if count != 1:
        raise InvalidInput("You may only supply one action, %s." % seeh)

    # kill-nine has no required cmdline parameters
        
    if opts.reserveslot:
        thisaction = ARGS.RES_SLOT_LONG
        if not opts.memory:
            raise InvalidInput("%s requires %s, %s." %
                                       (thisaction, ARGS.MEMORY_LONG, seeh))
        if not opts.duration:
            raise InvalidInput("%s requires %s, %s." %
                                       (thisaction, ARGS.DURATION_LONG, seeh))
        if not opts.graceperiod:
            raise InvalidInput("%s requires %s, %s." %
                                       (thisaction, ARGS.GRACE_LONG, seeh))
        if not opts.slotid:
            raise InvalidInput("%s requires %s, %s." %
                                       (thisaction, ARGS.SLOTID_LONG, seeh))

# }}} END: XXI. Commandline arguments

# ############################################################
# XXII. Standalone entry and exit
# #########################################################{{{

def main(argv=None):
    """Consume inputs, configure logging, and launch the action.

    Keyword arguments:

    * argv -- Executable's arguments (default None)

    Return exit code:

    * 0 -- Success

    * 1 -- Input problem

    * 2 -- Configuration problem

    * 3 -- Problem with the local or remote system environment.

    * 4 -- Failure to carry out action.

    * 42 -- Programming error, please report if this is a non-modified release
    
    
    Note that two other codes may also be used for interpreter exit if running
    in standalone mode (i.e., if __name__ == "__main__").
        
    * 5 -- Signal caught and handled successfully

    * 6 -- Signal caught and handled unsuccessfully

    """

    if os.name != 'posix':
        print >>sys.stderr, "Only runs on POSIX systems."
        return 3

    starttimer()

    parser = parsersetup()

    if argv:
        (opts, args) = parser.parse_args(argv[1:])
    else:
        (opts, args) = parser.parse_args()

    global log
    log = None
    
    try:
        validateargs(opts)

        if opts.configpath:
            config = getconfig(filepath=opts.configpath)
        else:
            config = getconfig(string=DEFAULTCONFIG)

        logfiledir = None
        try:
            logfiledir = config.get("logging", "logfiledir")
        except:
            pass
            
        logfileprefix = None
        if logfiledir:
            try:
                logfileprefix = config.get("logging", "logfileprefix")
            except:
                pass
                
        if opts.verbose or opts.trace:
            configureLogging(logging.DEBUG, 
                             trace=opts.trace,
                             slotid=opts.slotid,
                             stdout=opts.stdoutlog,
                             logfiledir=logfiledir,
                             logfileprefix=logfileprefix)
        elif opts.quiet:
            configureLogging(logging.ERROR,
                             slotid=opts.slotid,
                             stdout=opts.stdoutlog,
                             logfiledir=logfiledir,
                             logfileprefix=logfileprefix)
        else:
            configureLogging(logging.INFO,
                             slotid=opts.slotid,
                             stdout=opts.stdoutlog,
                             logfiledir=logfiledir,
                             logfileprefix=logfileprefix)

        if opts.evaluate:
            log.info("EVALUATE MODE ENABLED")
            
        if opts.reserveslot:
            log.debug("action %s" % ARGS.RES_SLOT_LONG)
        elif opts.killnine:
            log.debug("action %s" % ARGS.KILL9_LONG)

        commonconf = getCommonConf(opts, config)

        # Plugin wiring is programmatic in this version.
        # Here is the place to wire new ones for now.

        # Everything is separated entirely by action, for the future when we
        # will likely have others with different needs.
        if opts.reserveslot:
            slotconf = getReserveSlotConf(opts, config)
            listeners = []

            xenconf = getXenActionsConf(opts, config)
            xen = XenActions(xenconf, commonconf)
            
            # For testing only:
            #xen = FakeXenActions(xenconf, commonconf)
            
            listeners.append(xen)

            if opts.contact:
                notifyconf = getVWSNotificationsConf(opts, config)
                notify = VWSNotifications(notifyconf, commonconf)
                listeners.append(notify)

            action = ReserveSlot(commonconf, slotconf, listeners)
            setaction(action)
            action.run()

        elif opts.killnine:
            xenconf = getXenActionsConf(opts, config)
            xen = XenKillNine(xenconf, commonconf)
            listeners = [xen]
            # "hidden" option, used in the test harness for example
            if opts.contact:
                notifyconf = getVWSNotificationsConf(opts, config)
                notify = VWSNotifications(notifyconf, commonconf)
                listeners.append(notify)
                log.debug("kill9 notifications enabled")
            action = KillNine(commonconf, None, listeners)
            setaction(action)
            action.run()

    except InvalidInput, e:
        msg = "Problem with input: %s" % e.msg
        if log:
            log.critical(msg)
        else:
            print >>sys.stderr, msg
        return 1

    except InvalidConfig, e:
        msg = "Problem with configuration: %s" % e.msg
        if log:
            log.critical(msg)
        else:
            print >>sys.stderr, msg
        return 2

    except IncompatibleEnvironment, e:
        msg = "Cannot validate environment: %s" % e.msg
        if log:
            log.critical(msg)
        else:
            print >>sys.stderr, msg
        return 3

    except UnexpectedError, e:
        msg = "Problem executing: %s" % e.msg
        if log:
            log.critical(msg)
        else:
            print >>sys.stderr, msg
        return 4

    except ProgrammingError,e:
        msg = "Programming error.\n"
        msg += "   If this is a non-modified release, please report this.\n"
        msg += "   If possible, please include trace logs.\n"
        msg += "   MESSAGE: %s" % e.msg
        if log:
            log.critical(msg)
        else:
            print >>sys.stderr, msg
        return 42

if __name__ == "__main__":
    signal.signal(signal.SIGINT, sigint_handler)
    signal.signal(signal.SIGTERM, sigterm_handler)
    persistent_timestamp("PILOT1")
    exitcode = main()
    write_persistent_timestamps()
    sys.exit(exitcode)

# }}} END: XXII. Standalone entry and exit


