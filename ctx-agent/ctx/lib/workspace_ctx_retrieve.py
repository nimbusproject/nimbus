#!/usr/bin/env python

# Copyright 1999-2009 University of Chicago

# Project home: http://workspace.globus.org

# ############################################################
# Globals
# #########################################################{{{

VERSION = "2.2"

# Apache License 2.0:
LICENSE = """

Copyright 1999-2009 University of Chicago

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

# Project home: http://workspace.globus.org

# In the future, this value may come from bootstrap or the information
# may even be notification based.
POLL_DELAY_SECONDS_DEFAULT = 3

# For parsing response
NS_CTXBROKER = "http://www.globus.org/2008/12/nimbus"
NS_CTX = NS_CTXBROKER + "/contextualization"
NS_CTXTYPES = NS_CTXBROKER + "/ctxtypes"
NS_CTXDESC = NS_CTXBROKER + "/ctxdescription"

# }}} END: Globals

# ############################################################
# Embedded, default configuration file
# #########################################################{{{

DEFAULTCONFIG = """

# This is the default configuration file for the program.

# It can be changed inline or copied out into a file whose path can
# be passed into the program via command-line (see -h).  If that is done, 
# the program will NOT fall back to this default configuration if there
# is an error or misconfiguration with the supplied config file.


[sshd]

# Absolute path only, the sshd host key
generatedkey: /etc/ssh/ssh_host_rsa_key.pub

# SSHd hosts config is adjusted directly by this program right now, adds
# equiv hostnames and pubkeys for host based authorization across the whole
# contextualization context.

hostbasedconfig: /etc/hosts.equiv
knownhostsconfig: /etc/ssh/ssh_known_hosts


[reginst]

#### Regular instantiation

# Path to metadata server URL file
path: /var/nimbus-metadata-server-url

# Comma separated names of possible identity nics (do NOT use lo, for example).
# These are REAL local interface names that may be present (each is checked for
# an IP configuration).
nicnames: eth0, eth1


[systempaths]

# These can be relative or absolute paths.
hostname: hostname
curl: curl


# ***NOTE: it is unlikely you need to alter the configurations below this line

[taskpaths]

#### Calling order (this is explained in more detail below).
#### 0-etchosts
#### 1-ipandhost
#### 2-thishost
#### 3-data
#### 4-restarts
#### 5-thishostfinalize


#### 0-etchosts
#
# every identity seen is always sent to etchosts
#    arg1: IP
#    arg2: short hostname
#    arg3: FQDN

etchosts: /opt/nimbus/ctx-scripts/0-etchosts.sh


#### 1-ipandhost
#
# Directory where the scripts live that match the required role names.
# See samples for more explanation.
#
# These role scripts receive:
#    arg1: IP
#    arg2: short hostname
#    arg3: FQDN

ipandhostdir: /opt/nimbus/ctx-scripts/1-ipandhost


#### 2-thishost
#
# "thishost" scripts are called with network information known about the
# host this program is running on.
#
# Each script receives:
#    arg1: IP
#    arg2: Short local hostname
#    arg3: FQDN
#
# The names of the scripts in this directory must correspond to the interface
# that the context broker knows about, not the local interface which may not
# match.
# 
# Particular scripts may be absent.  The entire directory configuration
# may also be absent.

thishostdir: /opt/nimbus/ctx-scripts/2-thishost


#### 3-data
#
# The opaque data directory contains scripts that match provided data names.
# If data fields exist in the context, the data is written to a file and
# that file absolute path is sent as only argument to the scripts.
# The scripts are called after 'thishost' but before 'restarts'.

datadir: /opt/nimbus/ctx-scripts/3-data


#### 4-restarts
#
# The restart directory contains scripts that match provided roles.
#
# After all role information has been added via the ipandhostdir script AND
# after the "thishost" scripts have successfully run, this program will call
# the restart script for each required role it knows about (presumably to
# restart the service now that config has changed).
#
# No arguments are sent.
#
# It is OK for the required role to not have a script in this directory.

restartdir: /opt/nimbus/ctx-scripts/4-restarts


#### 5-thishostfinalize
#
# The "thishostfinalize" scripts are called with network information known
# about the host this program is running on.  It is called AFTER the restart
# scripts are successfully called.
#
# Each script receives:
#    arg1: IP
#    arg2: Short local hostname
#    arg3: FQDN
#
# The names of the scripts in this directory must correspond to the interface
# that the context broker knows about, not the local interface which may not
# match.
# 
# Particular scripts may be absent.  The entire directory configuration
# may also be absent.

thishostfinalizedir: /opt/nimbus/ctx-scripts/5-thishost-finalize


# "problem" script
# In case of problems, could call poweroff.  This script will be called after
# an attempt to notify the service of the error (that notification provides
# a log of the run to the context broker).
#
# Must be configured if "--poweroff" (-p) argument is used, will not be
# consulted if that argument is not used.

problemscript: /opt/nimbus/ctx-scripts/problem.sh


[ctxservice]

# logfile of the run
# If config is missing, no log will be written and nothing will be sent to
# service for error reporting.
logfilepath: /opt/nimbus/ctxlog.txt

# Directory where the program can write temporary files
scratchspacedir: /opt/nimbus/ctx/tmp

retr_template: /opt/nimbus/ctx/lib/retr-template-001.xml
retr_template2: /opt/nimbus/ctx/lib/retr-template-002.xml
err_template: /opt/nimbus/ctx/lib/err-template-001.xml
err_template2: /opt/nimbus/ctx/lib/err-template-002.xml
ok_template: /opt/nimbus/ctx/lib/ok-template-001.xml
ok_template2: /opt/nimbus/ctx/lib/ok-template-002.xml



[ec2]

#### EC2 instantiation (alternative to regular method)

# URLs for the Amazon REST instance data API
localhostnameURL:  http://169.254.169.254/2007-01-19/meta-data/local-hostname
publichostnameURL: http://169.254.169.254/2007-01-19/meta-data/public-hostname
localipURL:        http://169.254.169.254/2007-01-19/meta-data/local-ipv4
publicipURL:       http://169.254.169.254/2007-01-19/meta-data/public-ipv4
publickeysURL:     http://169.254.169.254/2007-01-19/meta-data/public-keys/
userdataURL:       http://169.254.169.254/2007-01-19/user-data


"""

# }}} END: Embedded, default configuration file

# ############################################################
# Imports
# #########################################################{{{

import ConfigParser
import commands
import fcntl
import logging
import optparse
import os
import shutil
import signal
import socket
import stat
import string
import struct
import sys
import time
import tempfile
import traceback

try:
    import elementtree.ElementTree as ET
except ImportError:
    print "elementtree not installed on the system, trying our backup copy"
    import embeddedET.ElementTree as ET
    
try:
    from threading import Thread
except ImportError:
    from dummy_threading import Thread

from commands import getstatusoutput
from xml.dom import minidom

# not using subprocess package to maintain at least python 2.3 compat.
from popen2 import Popen3

# }}} END: Imports

# ############################################################
# Exceptions
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
    
    """Exception for when the program cannot proceed."""
    
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
        
# }}} END: Exceptions

# ############################################################
# Logging
# #########################################################{{{

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
        _log = logging.getLogger("ctx-retrieve")
        _log.setLevel(logging.DEBUG)
    return _log
    
def addFileLogging(logger, logfilepath, formatter, level, trace=False):
    
    """adds file logging to preexisting logger, returns path to file"""
    
    if not formatter:
        if trace:
            formatstring = "%(asctime)s %(levelname)s @%(lineno)d: %(message)s"
        else:
            formatstring = "%(asctime)s %(levelname)s: %(message)s"
        formatter = logging.Formatter(formatstring)
    
    try:
        f = None
        try:
            f = file(logfilepath, 'a')
            f.write("\n## auto-generated @ %s\n\n" % time.ctime())
        finally:
            if f:
                f.close()
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        msg = "%s: %s" % (str(exceptname), str(sys.exc_value))
        raise IncompatibleEnvironment(msg)
        
    logfilehandler = logging.FileHandler(logfilepath)
    logfilehandler.setLevel(level)
    logfilehandler.setFormatter(formatter)
    logger.addHandler(logfilehandler)
    
    return logfilepath

def configureLogging(level, 
                     formatstring=None, 
                     logger=None, 
                     trace=False,
                     stdout=True,
                     logfilepath=None):
    """Configure the logging format and mechanism.  Sets global 'log' variable.
    
    Required parameter:
        
    * level -- log level

    Keyword arguments:

    * formatstring -- Custom logging format (default None, uses time+level+msg)

    * logger -- Custom logger (default None)
    
    * trace -- trace (default False)
    
    * stdout -- log to stdout (default True)
    
    * logfilepath -- log file path (default None)

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
    
    if logfilepath:
        addFileLogging(logger, logfilepath, formatter, level, trace)
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

# }}} END: Logging

# ############################################################
# Timer
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
        try:
            log.critical(msg)
        except:
            print >> sys.stderr, msg
        return None

    t1 = time.time()
    return t1 - _t0

# }}} END: Timer

# ############################################################
# Termination
# #########################################################{{{

def setterminateok(problemscript):
    global _terminateOK
    _terminateOK = problemscript

def terminateok():
    try:
        _terminateOK
    except:
        return None
    return _terminateOK

# }}} END: Termination

# ############################################################
# Path/system utilities
# #########################################################{{{

def uuidgen():
    return commands.getoutput('uuidgen')
        
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
    
def mode600(mode):
    mode = stat.S_IMODE(mode)
    if not mode & stat.S_IRUSR:
        return False
    if not mode & stat.S_IWUSR:
        return False
    for i in ("GRP", "OTH"):
        for perm in "R", "W", "X":
            if mode & getattr(stat, "S_I"+ perm + i):
                return False
    return True
            
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
    
    if killtime > 0:
        thr = SimpleRunThread(cmd, killsig=signal.SIGKILL, killtime=killtime)
    else:
        thr = SimpleRunThread(cmd)
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
            
# ifconfig and _ifinfo are snippets from python mailing list (very nice)
def _ifinfo(sock, addr, ifname):
    iface = struct.pack('256s', ifname[:15])
    info  = fcntl.ioctl(sock.fileno(), addr, iface)
    if addr == 0x8927:
        hwaddr = []
        for char in info[18:24]:
            hwaddr.append(hex(ord(char))[2:])
        return ':'.join(hwaddr)
    else:
        return socket.inet_ntoa(info[20:24])

def ifconfig(ifname):
    ifreq = {'ifname': ifname}
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        ifreq['addr']    = _ifinfo(sock, 0x8915, ifname) # SIOCGIFADDR
        ifreq['brdaddr'] = _ifinfo(sock, 0x8919, ifname) # SIOCGIFBRDADDR
        ifreq['netmask'] = _ifinfo(sock, 0x891b, ifname) # SIOCGIFNETMASK
        ifreq['hwaddr']  = _ifinfo(sock, 0x8927, ifname) # SIOCSIFHWADDR
    except:
        pass
        # exceptions are normal
        #log.exception()
    sock.close()
    return ifreq
    
def write_repl_file(path, outputtext):
    """TODO: switch this to use tempfile.mkstemp"""
    f = None
    try:
        try:
            # touch the file or replace what was there
            f = open(path, 'w')
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
                     % (path, name, err)
            log.error(errmsg)
            raise UnexpectedError(errmsg)
    finally:
        if f:
            f.close()
            
    # chmod user-only read/write
    target = stat.S_IRUSR | stat.S_IWUSR
    try:
        os.chmod(path, target)
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
        raise UnexpectedError(errmsg)
        
    # make sure it happened
    midx = stat.ST_MODE
    errmsg = "Failed to modify '%s' to %s" % (path, modeStr(target))
    en2 = os.stat(path)
    if not mode600(en2[midx]):
        raise UnexpectedError(errmsg)
        
    log.debug("Created '%s' and modified permissions to 600" % path)
    
    f = None
    try:
        try:
            f = open(path, 'w')
            f.write(outputtext)
            f.write("\n")
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            name = str(exceptname)
            err = str(sys.exc_value)
            errmsg = "Problem writing to '%s': %s: %s\n" \
                     % (path, name, err)
            log.error(errmsg)
            raise UnexpectedError(errmsg)
    finally:
        if f:
            f.close()

    log.info("Wrote '%s'." % path)
            
# }}} END: Path/system utilities

# ############################################################
# Bootstrap consumption (v3)
# #########################################################{{{

class Bootstrap:
    
    """Class for bootrap parsing, in order to override later on.
       This is for syntax v3.
    """
    
    # spec is at least 20 ='s:
    BOOTSTRAP_FIELD_SEPARATOR = "===================="
    
    def __init__(self, text):
        self.cluster = None
        self.service_url = None
        self.resource_key = None
        self.credential_string = None
        self.private_key_string = None
        self.parse_xml_userdata(text)
            
    #### utlities etc.
    
    def parse_xml_userdata(self, text):
        
        if not isinstance(text, str):
            log.error("Bootstrap text input is null or not a string?")
            return None
        
        if text.strip() == "":
            log.error("Bootstrap text is empty?")
            return None
            
        log.debug("First 20 chars of userdata: '%s'" % text[:20])
        
        try:
            tree = ET.fromstring(text)
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            name = str(exceptname)
            err = str(sys.exc_value)
            errmsg = "Problem parsing userdata: %s: %s\n" % (name, err)
            log.error(errmsg)
            return None
        
        if tree.tag != "NIMBUS_CTX":
            raise UnexpectedError("unknown element in userdata: '%s' (expecting NIMBUS_CTX)" % str(tree.tag))
            
        namespace_context = NS_CTX
        namespace_types = NS_CTXTYPES
        namespace_desc = NS_CTXDESC
        
        contactTag = "{%s}contact" % namespace_desc
        contact = tree.find(contactTag)
        
        clusterTag = "{%s}cluster" % namespace_desc
        clusterXML = tree.find(clusterTag)
                
        if contact == None:
            raise UnexpectedError("could not locate broker contact type in userdata")
        
        if clusterXML == None:
            raise UnexpectedError("could not locate cluster context document in userdata")
        clustertext = ET.tostring(clusterXML, encoding="UTF-8")
        clusterXMllines = clustertext.split("\n")
        self.cluster = "\n".join(clusterXMllines[1:])
            
        brokerURLTag = "{%s}brokerURL" % namespace_desc
        self.service_url = contact.find(brokerURLTag)
        
        contextIDTag = "{%s}contextID" % namespace_desc
        self.resource_key = contact.find(contextIDTag)
        
        secretTag = "{%s}secret" % namespace_desc
        secret = contact.find(secretTag)
        
        if self.service_url == None:
            raise UnexpectedError("could not locate broker URL in userdata")
        else:
            self.service_url = self.service_url.text
            
        if self.resource_key == None:
            raise UnexpectedError("could not locate context ID in userdata")
        else:
            keyprefix = "{%s}NimbusContextBrokerKey=" % namespace_context
            key = self.resource_key.text.strip()
            if key[:79] != keyprefix:
                raise UnexpectedError("context ID has unexpected namespace: '%s'" % key)
            self.resource_key = key[79:]
            
        if secret == None:
            raise UnexpectedError("could not locate secret in userdata")
        
        sections = self.get_sections_tuple(secret.text)
        self.credential_string = sections[1]
        self.private_key_string = sections[2]
        
    def get_sections_tuple(self, text):
        if not isinstance(text, str):
            log.error("text input is null or not a string?")
            return None
        
        if text.strip() == "":
            log.error("text is empty?")
            return None
            
        lines = text.split("\n")
            
        sections = []
        buf = ""
        for line in lines:
            if line.strip() == "":
                continue
            if line.strip().startswith(Bootstrap.BOOTSTRAP_FIELD_SEPARATOR):
                sections.append(buf)
                buf = ""
                continue
            buf += line + "\n"
        
        return sections

# }}} Bootstrap consumption (v3)

# ############################################################
# Response consumption v2 (XML)
# #########################################################{{{

def response2_parse_file(path, trace=False, responsetype=2):
    """Return RetrieveResult object if <retrieveResponse> was in
    the response.  Return partial response (i.e., not locked or
    not complete), let caller decide what behavior is appropriate
    in that situation.
    
    Raise nothing.
    
    """
    
    try:
        tree = ET.parse(path)
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        name = str(exceptname)
        err = str(sys.exc_value)
        errmsg = "Problem parsing '%s': %s: %s\n" % (path, name, err)
        log.error(errmsg)
        return None
        
    if responsetype == 2:
        namespace_context = NS_CTX
        namespace_types = NS_CTXTYPES
        namespace_desc = NS_CTXDESC
    else:
        raise UnexpectedError("unknown response type '%s'" % str(responsetype))
        
    # <retrieveResponse> is all we care about
    retrv =  tree.find('*/{%s}retrieveResponse' % namespace_context)
    
    if retrv == None:
        if trace:
            log.debug("retrieveResponse not found in response file")
        return None
        
    cancelled = retrv.find('{%s}cancelled' % namespace_types)
    
    locked = retrv.find('{%s}noMoreInjections' % namespace_types)
    
    if locked == None:
        # incomplete response, just return
        if trace:
            log.debug("noMoreInjections element not found in response?")
        return None
        
    complete = retrv.find('{%s}complete' % namespace_types)
    
    if complete == None:
        # incomplete response, just return
        if trace:
            log.debug("complete element not found in response?")
        return None


    # Going to return a RetrieveResult from now on
    result = RetrieveResult()
    
    if cancelled != None:
        if cancelled.text != None:
            if cancelled.text.strip().lower() == "true":
                raise UnexpectedError("broker reported that the context was cancelled")
                
    isLocked = False
    if locked.text != None:
        if locked.text.strip().lower() == "true":
            isLocked = True
    result.locked = isLocked
    
    if trace:
        if isLocked:
            log.debug("resource is locked")
        else:
            log.debug("resource is not locked")
            
    isComplete = False
    if complete.text != None:
        if complete.text.strip().lower() == "true":
            isComplete = True
    result.complete = isComplete
    
    if trace:
        if isComplete:
            log.debug("resource is complete")
        else:
            log.debug("resource is not complete")
    
    requires_array = retrv.find('{%s}requires' % namespace_types)
    
    if requires_array == None:
        if trace:
            log.debug("no requires found in response")
        return result

    all_identities = requires_array.findall('{%s}identity' % namespace_desc)

    if trace:
        log.debug("Found %d identities" % len(all_identities))
        
    for x,ident in enumerate(all_identities):
        if trace:
            log.debug("Examining identity #%d" % x)
        identity = response2_parse_one_identity(ident, trace=trace)
        if identity != None:
            result.identities.append(identity)
            
    all_roles = requires_array.findall('{%s}role' % namespace_desc)
    
    if trace:
        log.debug("Found %d roles" % len(all_roles))
        
    for x,role in enumerate(all_roles):
        if trace:
            log.debug("Examining role #%d" % x)
        resprole = response2_parse_one_role(role, trace)
        if resprole != None:
            result.roles.append(resprole)
            
    all_data = requires_array.findall('{%s}data' % namespace_desc)
    
    if trace:
        log.debug("Found %d data elements" % len(all_data))
        
    for x,data in enumerate(all_data):
        if trace:
            log.debug("Examining data #%d" % x)
        respdata = response2_parse_one_data(data, trace)
        if respdata != None:
            result.data.append(respdata)
    
    return result
    
def response2_parse_one_role(role, trace=False):
    if role == None:
        if trace:
            log.debug("  - role is null?")
        return None
        
    resprole = ResponseRole()
    
    if len(role.items()) > 1:
        # ok to continue here
        log.error("unexpected, role has more than one attr")
        
    if len(role.items()) < 1:
        log.error("error, role has zero attrs?")
        return None
        
    attrtuple = role.items()[0]
    
    if attrtuple == None:
        log.error("error, role has null in items list?")
        return None
        
    if len(attrtuple) != 2:
        log.error("error, role has object in item list not length 2?")
        return None
        
    namekey = "{%s}name" % NS_CTXDESC
    if attrtuple[0] == namekey:
        resprole.name = attrtuple[1]
        if trace:
            log.debug("  - name: '%s'" % attrtuple[1])
    else:
        log.error("error, role has attr not named 'name'?")
        return None
        
    resprole.ip = role.text
    if trace:
        log.debug("  - ip: '%s'" % role.text)
    
    return resprole
    
def response2_parse_one_data(data, trace=False):
    if data == None:
        if trace:
            log.debug("  - data is null?")
        return None
        
    respdata = OpaqueData()
    
    if len(data.items()) > 1:
        # ok to continue here
        log.error("unexpected, data has more than one attr")
        
    if len(data.items()) < 1:
        log.error("error, data has zero attrs?")
        return None
        
    attrtuple = data.items()[0]
    
    if attrtuple == None:
        log.error("error, data has null in items list?")
        return None
        
    if len(attrtuple) != 2:
        log.error("error, data has object in item list not length 2?")
        return None
    
    namekey = "{%s}name" % NS_CTXDESC
    if attrtuple[0] == namekey:
        respdata.name = attrtuple[1]
        if trace:
            log.debug("  - data name: '%s'" % attrtuple[1])
    else:
        log.error("error, role has attr not named 'name'?")
        return None
        
    respdata.data = data.text
    if trace:
        log.debug("  - first 32 of data: '%s'" % data.text[:32])
    
    return respdata

def response2_parse_one_identity(ident, trace=False, responsetype=2):
    if ident == None:
        if trace:
            log.debug("  - ident is null?")
        return None
        
    identity = Identity()
    
    if responsetype == 2:
        namespace_desc = NS_CTXDESC
    else:
        raise UnexpectedError("unknown response type '%s'" % str(responsetype))
    
    ip = ident.find('{%s}ip' % namespace_desc)
    if ip != None:
        identity.ip = ip.text
        if trace:
            log.debug("  - found ip: %s" % ip.text)

    host = ident.find('{%s}hostname' % namespace_desc)
    if host != None:
        identity.host = host.text
        if trace:
            log.debug("  - found host: %s" % host.text)

    pubkey = ident.find('{%s}pubkey' % namespace_desc)
    if pubkey != None:
        identity.pubkey = pubkey.text
        if trace:
            log.debug("  - found pubkey: %s" % pubkey.text)
    
    return identity

def response2_parse_for_fatal(path, trace=False):
    if path == None:
        return
    
    f = None
    try:
        try:
            f = open(path)
            for line in f:
                if line.rfind("NoSuchResourceException") >= 0:
                    msg = "Response contained NoSuchResourceException"
                    raise UnexpectedError(msg)
        except UnexpectedError:
            raise
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            name = str(exceptname)
            err = str(sys.exc_value)
            errmsg = "Problem looking at '%s': %s: %s\n" \
                     % (path, name, err)
            log.error(errmsg)
    finally:
        if f:
            f.close()

# }}} END: Response consumption v2 (XML)

# ############################################################
# InstantiationResult
# #########################################################{{{

class InstantiationResult:

    """Class holding result of bootstrap and identity population.
    
       Only supporting two interfaces right now.
    
    """
    
    def __init__(self):
        self.iface_name = None
        self.iface_ip = None
        self.iface_hostname = None
        self.iface_short_hostname = None
        
        self.iface2_name = None
        self.iface2_ip = None
        self.iface2_hostname = None
        self.iface2_short_hostname = None
        
        # what the onboard VM interface is called.  In some cases this might
        # not even be populated (for example on EC2)
        self.iface_REAL_name = None
        self.iface2_REAL_name = None
        
        # full text of local key
        self.pub_key = None
        
        self.ctx_certtext = None
        self.ctx_keytext = None
        self.ctx_url = None
        self.ctx_key = None
        
        # full cluster xml text for retrieve operation
        self.cluster_text = None
    
# }}} END: InstantiationResult

# ############################################################
# RetrieveResult
# #########################################################{{{

class Identity:
    """Class holding one identity result.  RetrieveResult houses
    a list of these."""
    
    def __init__(self):
        """Instantiate fields."""
        
        self.ip = None
        self.host = None
        self.pubkey = None
        
class ResponseRole:
    """Class holding one role result.  RetrieveResult houses
    a list of these."""
    
    def __init__(self):
        """Instantiate fields."""
        
        self.name = None
        self.ip = None
        
class OpaqueData:
    """Class holding one data result.  RetrieveResult houses
    a list of these."""
    
    def __init__(self):
        """Instantiate fields."""
        
        self.name = None
        self.data = None

class RetrieveResult:

    """Class holding contextualization result."""
    
    def __init__(self):
        """Instantiate fields."""
        
        self.locked = False
        self.complete = False
        
        # list of Identity objects
        self.identities = []
        
        # list of ResponseRole objects
        self.roles = []
        
        # list of OpaqueData objects
        self.data = []

    
# }}} END: RetrieveResult

# ############################################################
# Action
# #########################################################{{{

class Action:

    """Parent class of every action."""
    
    def __init__(self, commonconf):
        """Initialize result and common conf fields.
        
        Required parameters:

        * commonconf -- CommonConf instance
        
        """
        self.common = commonconf
        self.result = None

    def run(self):
        """Start.

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """

        pass

# }}} END: Action

# ############################################################
# RegularInstantiation(Action)
# #########################################################{{{

class RegularInstantiation(Action):

    """Class implementing bootstrap retrieval from Nimbus metadata server.  It
    also populates the identity and sshd pubkey values."""
    
    def __init__(self, commonconf, regconf):
        """Instantiate object with configurations necessary to operate.

        Required parameters:

        * commonconf -- CommonConf instance

        * regconf -- ReginstConf instance

        Raise InvalidConfig if problem with the supplied configurations.
        
        Sets its result field to IdentityAndCredentialResult instance.

        """
        
        Action.__init__(self, commonconf)
        self.conf = regconf
        self.result = None
        
    def run(self):
        """Start.

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """
        
        self.realnics = self.analyze_active_nics()
        self.numnics = len(self.realnics)
        if self.numnics < 1:
            raise UnexpectedError("No network interfaces to resolve")
        elif self.numnics > 2:
            raise UnexpectedError("Too many network interfaces to resolve")
        
        # all fields are None in new instance:
        self.result = InstantiationResult()
        
        self._populate_one_or_two()
        
        # get generated pubkey from file
        self.result.pub_key = self.consume_sshd_key()
        if self.result.pub_key == None or self.result.pub_key == "":
            raise UnexpectedError("Couldn't obtain sshd pubkey")
            
        if self.common.trace:
            log.debug("Found sshd key: '%s'" % self.result.pub_key)
            
        self.consume_bootstrap()
        
    def _populate_one_or_two(self):
        
        # This does not set iface_name or iface2_name.
        # Those names must come from context broker and be matched to
        # what we've found on the real system (matching done by ip).
        
        self.result.iface_REAL_name = self.realnics[0]
        ifreq = ifconfig(self.realnics[0])
        addr = ifreq['addr']
        self.result.iface_hostname = ""
        self.result.iface_short_hostname = ""
        self.result.iface_ip = addr
            
        if self.numnics == 2:
            self.result.iface2_REAL_name = self.realnics[1]
            ifreq = ifconfig(self.realnics[1])
            addr = ifreq['addr']
            self.result.iface2_hostname = ""
            self.result.iface2_short_hostname = ""
            self.result.iface2_ip = addr
        
    def has_ip_assignment(self, iface):
        try:
            log.debug("Checking addresses of '%s' interface" % iface)
            ifreq = ifconfig(iface)
            log.debug("Address check of '%s' interface: " % ifreq)
            if ifreq and ifreq.has_key('addr'):
                addr = ifreq['addr']
                if addr:
                    msg = "Found address '%s' for interface '%s'" % (addr, iface)
                    log.debug(msg)
                    return True
        except:
            log.exception("exception looking for '%s' ip assignment" % iface)
        return False
        
    def analyze_active_nics(self):
        nics = []
        for nicname in self.conf.niclist:
            if self.has_ip_assignment(nicname):
                nics.append(nicname)
        return nics
        
    def consume_sshd_key(self):
        
        path = self.common.sshdkeypath
        
        if not os.path.exists(path):
            raise UnexpectedError("'%s' does not exist on filesystem" % path)
        
        # TODO consume better later
        text = ""
        f = open(path)
        try:
            for line in f:
                text += line
        finally:
            f.close()
        
        # strip any newlines and extra whitespace
        return text.replace("\n", "").strip()
        
    def get_stdout(self, url):
        # todo: switch to python classes
        
        timeout = 5
        curlcmd = "%s --silent --url %s -o /dev/stdout" % (self.common.curlpath, url)
        
        (exit, stdout, stderr) = runexe(curlcmd, killtime=timeout+1)
        result = "'%s': exit=%d, stdout='%s'," % (curlcmd, exit, stdout)
        result += " stderr='%s'" % (stderr)
        
        if self.common.trace:
            log.debug(result)
        
        if exit != 0:
            msg = "PROBLEM: curl command failed, "
            msg += "result: %s" % result
            log.error(msg)
            return None
        return stdout
        
    # can raise UnexpectedError
    def consume_bootstrap(self):
        
        # get the metadata server URL
        try:
            f = None
            text = ""
            try:
                f = open(self.conf.path)
                for line in f:
                    text += line
            finally:
                if f:
                    f.close()
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            msg2 = "%s: %s" % (str(exceptname), str(sys.exc_value))
            
            msg = "Problem reading metadata URL file @ '%s'.  " % self.conf.path
            msg += msg2
            log.error(msg)
            raise UnexpectedError(msg)
        
        if text == None or text.strip() == "":
            raise UnexpectedError("no metadata URL to get userdata")
            
        metadataServerURL = text.strip()
        
        log.info("Found metadata server URL: '%s'" % metadataServerURL)
        
        metadataServerURL = metadataServerURL + "/2007-01-19/"
            
        geturl = metadataServerURL + "user-data"
        userdataText = self.get_stdout(geturl)
        if userdataText == None or userdataText.strip() == "":
            raise UnexpectedError("could not obtain userdata @ '%s'" % geturl)
        
        bootstrap = Bootstrap(userdataText)
        
        url = bootstrap.service_url
        if self.common.trace:
            log.debug("contextualization service URL = '%s'" % url)
        self.result.ctx_url = url
        
        key = bootstrap.resource_key
        if self.common.trace:
            log.debug("contextualization service key = '%s'" % key)
        self.result.ctx_key = key
        
        privPEM = bootstrap.private_key_string
        #log.debug("private key PEM = '%s'" % privPEM)
        self.result.ctx_keytext = privPEM
            
        pubPEM = bootstrap.credential_string
        if self.common.trace:
            log.debug("public cert PEM = '%s'" % pubPEM)
        self.result.ctx_certtext = pubPEM
        
        self.result.cluster_text = bootstrap.cluster

        # now figure out which onboard NIC we found corresponds to which NIC in
        # metadata server
        
        # start by getting NIC information from the metadata server
        
        geturl = metadataServerURL + "meta-data/local-hostname"
        localHostname = self.get_stdout(geturl)
        if localHostname != None and localHostname.strip() == "":
            localHostname = None
        
        geturl = metadataServerURL + "meta-data/local-ipv4"
        localIP = self.get_stdout(geturl)
        if localIP != None and localIP.strip() == "":
            localIP = None
            
        geturl = metadataServerURL + "meta-data/public-hostname"
        publicHostname = self.get_stdout(geturl)
        if publicHostname != None and publicHostname.strip() == "":
            publicHostname = None
            
        geturl = metadataServerURL + "meta-data/public-ipv4"
        publicIP = self.get_stdout(geturl)
        if publicIP != None and publicIP.strip() == "":
            publicIP = None
        
        if localIP == None and publicIP == None:
            raise UnexpectedError("could not obtain any network information from metadata server @ '%s'" % metadataServerURL)
            
        if localIP:
            localIP = localIP.strip()
        if publicIP:
            publicIP = publicIP.strip()
        if localHostname:
            localHostname = localHostname.strip()
        if publicHostname:
            publicHostname = publicHostname.strip()
            
        # only one NIC found on system:
        if self.result.iface_ip and not self.result.iface2_ip:
            
            if self.result.iface_ip not in (localIP, publicIP):
                msg = "Found one local NIC IP ('%s')" % self.result.iface_ip
                msg += " but it is not described in the metadata server."
                raise UnexpectedError(msg)
                
            if self.result.iface_ip == localIP:
                self.result.iface_hostname = localHostname
                self.result.iface_name = "localnic"
            elif self.result.iface_ip == publicIP:
                self.result.iface_hostname = publicHostname
                self.result.iface_name = "publicnic"
            self.result.iface_short_hostname = self.result.iface_hostname.split(".")[0]
            
        # two
        if self.result.iface_ip and self.result.iface2_ip:
            # Real IPs
            rip1 = self.result.iface_ip
            rip2 = self.result.iface2_ip
            
            # Bootstrap IPs
            bip1 = localIP
            bip2 = publicIP
            
            log.info("Local IPs: #1:'%s' and #2:'%s'" % (rip1, rip2))
            log.info("Bootstrap IPs: #1:'%s' and #2:'%s'" % (bip1, bip2))
            
            matched = False
            flipped = False
            
            if rip1 == bip1:
                log.debug("1's match")
                if rip2 == bip2:
                    log.debug("and 2's match")
                    matched = True
                else:
                    msg = "One real NIC and one metadata server NIC match, but the "
                    msg += "other doesn't?  '%s' equals '%s' " % (rip1, bip1)
                    msg += "but the local IP '%s' that was found " % (rip2)
                    msg += "does not match '%s' in metadata server." % (bip2)
                    raise UnexpectedError(msg)
            
            elif rip1 == bip2:
                log.debug("1 flipped IP match")
                if rip2 == bip1:
                    log.debug("and other flip matches")
                    flipped = True
                else:
                    msg = "One real NIC and one metadata server NIC match, but the "
                    msg += "other doesn't?  '%s' equals '%s' " % (rip1, bip2)
                    msg += "but the local IP '%s' that was found " % (rip2)
                    msg += "does not match '%s' in metadata server." % (bip1)
                    raise UnexpectedError(msg)
                    
            elif rip2 == bip1:
                msg = "One real NIC and one metadata server NIC match, but the "
                msg += "other doesn't?  '%s' equals '%s' " % (rip2, bip1)
                msg += "but the local IP '%s' that was found " % (rip1)
                msg += "does not match '%s' in metadata server." % (bip2)
                raise UnexpectedError(msg)
                
            elif rip2 == bip2:
                msg = "One real NIC and one metadata server NIC match, but the "
                msg += "other doesn't?  '%s' equals '%s' " % (rip2, bip2)
                msg += "but the local IP '%s' that was found " % (rip1)
                msg += "does not match '%s' in metadata server." % (bip1)
                raise UnexpectedError(msg)
                
            else:
                msg = "Neither local NIC that was found matches to the "
                msg += "available IPs in the metadata server."
                raise UnexpectedError(msg)
                
                
            nicname1 = "localnic"
            nicname2 = "publicnic"
                
            host1 = localHostname
            host2 = publicHostname
            
            shorthost1 = host1.split(".")[0]
            shorthost2 = host2.split(".")[0]
                
            if matched:
                
                self.result.iface_name = nicname1
                self.result.iface_hostname = host1
                self.result.iface_short_hostname = shorthost1
                
                self.result.iface2_name = nicname2
                self.result.iface2_hostname = host2
                self.result.iface2_short_hostname = shorthost2
                
            elif flipped:
                
                self.result.iface_name = nicname2
                self.result.iface_hostname = host2
                self.result.iface_short_hostname = shorthost2
                
                self.result.iface2_name = nicname1
                self.result.iface2_hostname = host1
                self.result.iface2_short_hostname = shorthost1
                
            else:
                raise ProgrammingError("either matched or flipped here only")
                
        
# }}} END: RegularInstantiation(Action)

# ############################################################
# AmazonInstantiation(Action)
# #########################################################{{{

class AmazonInstantiation(Action):

    """Class implementing bootstrap retrieval from EC2.  It also populates
       the identity and sshd pubkey values."""
    
    def __init__(self, commonconf, ec2conf):
        """Instantiate object with configurations necessary to operate.

        Required parameters:

        * commonconf -- CommonConf instance

        * ec2conf -- AmazonConf instance

        Raise InvalidConfig if problem with the supplied configurations.
        
        Sets its result field to IdentityAndCredentialResult instance.

        """
        
        Action.__init__(self, commonconf)
        self.conf = ec2conf
        
    def run(self):
        """Start.

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """
        
        #    self.conf.localhostnameURL
        #    self.conf.publichostnameURL
        #    self.conf.userdataURL
        
        result = InstantiationResult()
        
        # force these names on amazon, 
        # i.e. ctx document given to broker must match
        result.iface_name = "publicnic" # public
        result.iface2_name = "localnic" # private LAN address
        
        r = self.get_stdout(self.conf.publicipURL)
        if r == None:
            raise UnexpectedError("Couldn't obtain pub IP")
        result.iface_ip = r.replace("\n", "").strip()
        
        r = self.get_stdout(self.conf.localipURL)
        if r == None:
            raise UnexpectedError("Couldn't obtain local IP")
        result.iface2_ip = r.replace("\n", "").strip()
         
        r = self.get_stdout(self.conf.publichostnameURL)
        if r == None:
            raise UnexpectedError("Couldn't obtain pub hostname")
        result.iface_hostname = r.replace("\n", "").strip()
        result.iface_short_hostname = result.iface_hostname.split(".")[0]
         
        r = self.get_stdout(self.conf.localhostnameURL)
        if r == None:
            raise UnexpectedError("Couldn't obtain local hostname")
        result.iface2_hostname = r.replace("\n", "").strip()
        result.iface2_short_hostname = result.iface2_hostname.split(".")[0]
        
        # get generated pubkey from file
        result.pub_key = self.consume_sshd_key()
        if result.pub_key == None or result.pub_key == "":
            raise UnexpectedError("Couldn't obtain sshd pubkey")
            
        if self.common.trace:
            log.debug("Found sshd key: '%s'" % result.pub_key)
            
        self.result = result
            
        self.consume_bootstrap()
        
    def consume_sshd_key(self):
        
        path = self.common.sshdkeypath
        
        if not os.path.exists(path):
            raise UnexpectedError("'%s' does not exist on filesystem" % path)
        
        # TODO consume properly later
        text = ""
        f = open(path)
        try:
            for line in f:
                text += line
        finally:
            f.close()
        
        # strip any newlines and extra whitespace
        return text.replace("\n", "").strip()
        
    # can raise UnexpectedError
    def consume_bootstrap(self):
        text = self.get_stdout(self.conf.userdataURL)
        if text == None:
            raise UnexpectedError("no user data for bootstrap")
            
        bootstrap = Bootstrap(text)
        
        url = bootstrap.service_url
        if self.common.trace:
            log.debug("contextualization service URL = '%s'" % url)
        self.result.ctx_url = url
        
        key = bootstrap.resource_key
        if self.common.trace:
            log.debug("contextualization service key = '%s'" % key)
        self.result.ctx_key = key
        
        privPEM = bootstrap.private_key_string
        if self.common.trace:
            log.debug("private key PEM = '%s'" % privPEM)
        self.result.ctx_keytext = privPEM
            
        pubPEM = bootstrap.credential_string
        if self.common.trace:
            log.debug("public cert PEM = '%s'" % pubPEM)
        self.result.ctx_certtext = pubPEM
        
        self.result.cluster_text = bootstrap.cluster
        
    def get_stdout(self, url):
        # todo: switch to python classes
        
        timeout = 5
        curlcmd = "%s --silent --url %s -o /dev/stdout" % (self.common.curlpath, url)
        
        (exit, stdout, stderr) = runexe(curlcmd, killtime=timeout+1)
        result = "'%s': exit=%d, stdout='%s'," % (curlcmd, exit, stdout)
        result += " stderr='%s'" % (stderr)
        
        if self.common.trace:
            log.debug(result)
        
        if exit != 0:
            msg = "PROBLEM: curl command failed, "
            msg += "result: %s" % result
            log.error(msg)
            return None
        return stdout
    
# }}} END: AmazonInstantiation(Action)

# ############################################################
# DefaultRetrieveAction(Action)
# #########################################################{{{

class DefaultRetrieveAction(Action):

    """Class implementing getting the contextualization information.
    
       Right now there is only one implementation.
    """
    
    def __init__(self, commonconf, instresult):
        """Instantiate object with configurations necessary to operate.

        Required parameters:

        * commonconf -- CommonConf instance
        
        * instresult -- valid InstantiationResult instance

        Raise InvalidConfig if problem with the supplied configurations.
        
        Sets its result field to RetrieveResult instance.

        """
        
        Action.__init__(self, commonconf)
        
        if instresult == None:
            raise InvalidConfig("supplied instantiation result is None")
            
        # self.result is reserved for results of actions, not previous results
        self.result = None
        self.instresult = instresult
        
        # todo: could validate instresult here
        
    def run(self):
        """Start.

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """

        self.priv_pem_path = os.path.join(self.common.scratchdir, "ctxkey.pem")
        write_repl_file(self.priv_pem_path, self.instresult.ctx_keytext)
        
        self.pub_pem_path = os.path.join(self.common.scratchdir, "ctxcert.pem")
        write_repl_file(self.pub_pem_path, self.instresult.ctx_certtext)
        
        # stores path to filled in template copy
        self.postdocumentpath = None
        self.okdocumentpath = None
        self.errdocumentpath = None
        
        self.poll_until_done()
        
    def poll_until_done(self):
        
        result = None
        while (not self.analyze_result(result)):
            if self.common.trace:
                log.debug("Waiting %d seconds before poll." % self.common.polltime)
            time.sleep(self.common.polltime)
            result = self.retrieve_result()

        self.result = result
        
    def retrieve_result(self):
        
        timeout = 8
        
        responsepath = os.path.join(self.common.scratchdir, "response.xml")
        
        if self.postdocumentpath == None:
            self.filltemplates()
            self.zeroresponsepath(responsepath)
        
            curlpath = self.common.curlpath
            
            curlcmd = "%s --cert %s " % (curlpath, self.pub_pem_path)
            curlcmd += "--key %s " % self.priv_pem_path
            curlcmd += "--max-time %s " % timeout
            
            # The --insecure flag means that we do not validate remote
            # host which would close spoof possibility: for future version.
            curlcmd += " --insecure --random-file /dev/urandom --silent "
            
            # RETR
            curlretr = curlcmd + "--output %s " % responsepath
            curlretr += "--upload-file %s " % self.postdocumentpath
            curlretr += "%s" % self.instresult.ctx_url
            self.runcmd = curlretr
            
            # OK operation, set up for later
            curlok = curlcmd + "--upload-file %s " % self.okdocumentpath
            curlok += "%s" % self.instresult.ctx_url
            set_broker_okaction(DefaultOK(curlok))
            
            # ERR operation, set up for later
            curlerr = curlcmd + "--upload-file %s " % self.errdocumentpath
            curlerr += "%s" % self.instresult.ctx_url
            set_broker_erraction(DefaultERR(curlerr, self.errdocumentpath))
        
        log.info("Contacting service (retrieve operation)")
        
        if not self.runcmd:
            raise ProgrammingError("no runcmd setup for retrieve action")
        
        (exit, stdout, stderr) = runexe(self.runcmd, killtime=timeout+1)
        result = "'%s': exit=%d, stdout='%s'," % (self.runcmd, exit, stdout)
        result += " stderr='%s'" % (stderr)
        
        if self.common.trace:
            log.debug(result)
        
        if exit != 0:
            msg = "PROBLEM: curl command failed, "
            msg += "result: %s" % result
            log.error(msg)
            return None
        
        result = response2_parse_file(responsepath, self.common.trace)
        
        # Case for specific responses that mean there is just no way
        # we can continue (that will raise an UnexpectedError here)
        if result == None:
            response2_parse_for_fatal(responsepath, self.common.trace)
            
        return result
        
    def zeroresponsepath(self, path):
        f = None
        try:
            try:
                # touch the file or replace what was there
                f = open(path, 'w')
                f.close()
                f = None
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                ename = str(exceptname)
                err = str(sys.exc_value)
                errmsg = "Problem zeroing '%s': %s: %s\n" \
                         % (path, ename, err)
                log.error(errmsg)
                raise UnexpectedError(errmsg)
        finally:
            if f:
                f.close()
        
    # returns true if locked and complete
    def analyze_result(self, result):
        if result == None:
            return False
            
        if not isinstance(result, RetrieveResult):
            log.error("Only handling None or RetrieveResult")
            return False
            
        if result.locked and result.complete:
            return True
        
        return False
            
    def copy_template(self, source, dest):
        try:
            shutil.copyfile(source, dest)
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            ename = str(exceptname)
            err = str(sys.exc_value)
            errmsg = "Problem creating '%s': %s: %s\n" % (dest, ename, err)
            log.error(errmsg)
            raise UnexpectedError(errmsg)
            
    def filltemplates(self):
        """The SOAP engine..."""
        
        numnics = 0
        
        if self.instresult.iface_ip:
            numnics += 1
        if self.instresult.iface2_ip:
            numnics += 1
            
        if not numnics:
            raise UnexpectedError("no template is valid for no NICs")
            
        if numnics == 1:
            path = self.common.retr_template
            log.debug("Using retrieve template for one nic: '%s'" % path)
            
            okpath = self.common.ok_template
            log.debug("Using ok template for one nic: '%s'" % okpath)
            
            errpath = self.common.err_template
            log.debug("Using error template for one nic: '%s'" % errpath)
        elif numnics == 2:
            path = self.common.retr_template2
            log.debug("Using retrieve template for two nics: '%s'" % path)
            
            okpath = self.common.ok_template2
            log.debug("Using ok template for two nics: '%s'" % okpath)
            
            errpath = self.common.err_template2
            log.debug("Using error template for two nics: '%s'" % errpath)
        else:
            raise UnexpectedError("no templates are valid for > 2 NICs")
            
        if not os.path.exists(path):
            raise UnexpectedError("template '%s' doesn't exist?" % path)
        if not os.path.exists(okpath):
            raise UnexpectedError("template '%s' doesn't exist?" % okpath)
        if not os.path.exists(errpath):
            raise UnexpectedError("template '%s' doesn't exist?" % errpath)
            
        self.postdocumentpath = \
                        os.path.join(self.common.scratchdir, "retr.xml")
        self.okdocumentpath = \
                        os.path.join(self.common.scratchdir, "ok.xml")
        self.errdocumentpath = \
                        os.path.join(self.common.scratchdir, "err.xml")
                        
        self.copy_template(path, self.postdocumentpath)
        self.copy_template(okpath, self.okdocumentpath)
        self.copy_template(errpath, self.errdocumentpath)
            
        # all messages send at least this subset
        
        tpaths = [ self.postdocumentpath, 
                   self.okdocumentpath, 
                   self.errdocumentpath ]
        
        for i,tpath in enumerate(tpaths):
        
            # assuming template contents are correct.
            text = ""
            f = open(tpath)
            try:
                for line in f:
                    text += line
            finally:
                f.close()
        
            message_id = None
            try:
                message_id = uuidgen()
            except:
                # uuidgen not installed. hack is to use resource key ...
                log.error("could not generate uuid for message ID, using "
                          "resource key ...")
                message_id = self.theresult.ctx_key
            
            r = self.instresult
            
            text = text.replace("REPLACE_MESSAGE_ID", message_id)
            text = text.replace("REPLACE_SERVICE_URL", r.ctx_url)
            text = text.replace("REPLACE_RESOURCE_KEY", r.ctx_key)
            
            # iface_name is what the broker knows this as, the real interface
            # name is not relevant to the broker
            text = text.replace("REPLACE_IFACE_NAME", r.iface_name)
            text = text.replace("REPLACE_IFACE_IP", r.iface_ip)
            text = text.replace("REPLACE_IFACE_HOSTNAME", r.iface_hostname)
            text = text.replace("REPLACE_IFACE_SSH_KEY", r.pub_key)
            
            if numnics == 2:
                text = text.replace("REPLACE_IFACE2_NAME", r.iface2_name)
                text = text.replace("REPLACE_IFACE2_IP", r.iface2_ip)
                text = text.replace("REPLACE_IFACE2_HOSTNAME", r.iface2_hostname)
                text = text.replace("REPLACE_IFACE2_SSH_KEY", r.pub_key)
                
            if i == 0:
                text = text.replace("REPLACE_CLUSTER_XML", str(r.cluster_text))
            
            write_repl_file(tpath, text)
        
# }}} END: DefaultRetrieveAction(Action)

# ############################################################
# DefaultConsumeRetrieveResult(Action)
# #########################################################{{{

class DefaultConsumeRetrieveResult(Action):

    """Class implementing consuming the common RetrieveResult object that
       comes from the combination of actions that have been run before.
       
       Calls task scripts to handle each role.
       
       Adjusts SSHd to do host based authorization with nodes in the
       contextualization (hard coded for now, this should probably be a 
       task specific decision).
       
       Does not set its result field, there is no 'result' besides returning
       without exception.
       
       Right now there is only one implementation.
    """
    
    def __init__(self, commonconf, retrresult, instresult):
        """Instantiate object with configurations necessary to operate.

        Required parameters:

        * commonconf -- CommonConf instance
        
        * retrresult -- valid RetrieveResult instance  (todo: name better)
        
        * instresult -- valid InstantiationResult instance
        

        Raise InvalidConfig if problem with the supplied configurations.

        """
        
        Action.__init__(self, commonconf)
        
        if retrresult == None:
            raise InvalidConfig("supplied retrresult is None")
        if instresult == None:
            raise InvalidConfig("supplied instresult is None")
            
        self.retrresult = retrresult
        self.instresult = instresult
        
    def run(self):
        """Start.

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """

        self.handle_identities()
        self.handle_opensshd() 
        self.handle_roles()
        self.handle_thishost()
        self.handle_opaquedata()
        self.handle_restarts()
        self.handle_thishost(finalize=True)
        
    # todo: this could be treated as just another role in the future
    # perhaps passing in a parameter that both ip/hostname are required
    # for this "role"
    def handle_identities(self):
        etchostspath = self.common.etchosts_exe
        
        # todo verify exe exists and is executable etc.
        
        for ident in self.retrresult.identities:
            
            # no point in adding to /etc/hosts if ip or host is missing
            if ident.ip == None or ident.host == None:
                msg = "identity missing IP or host, not sending to 'etchosts'"
                log.debug(msg)
                continue
            
            
            short_host = ident.host.split(".")[0]
            
            cmd = "%s %s %s %s" % (etchostspath, ident.ip, short_host, ident.host)
            (exit, stdout, stderr) = runexe(cmd, killtime=0)
            result = "'%s': exit=%d, stdout='%s'," % (cmd, exit, stdout)
            result += " stderr='%s'" % (stderr)
            
            if self.common.trace:
                log.debug(result)
            
            if exit != 0:
                msg = "PROBLEM: etchosts addition command failed, "
                msg += "result: %s" % result
                raise UnexpectedError(msg)
        
    # assuming openssh for now, todo: turn this into a task script itself
    def handle_opensshd(self):
        
        # server config must have HostbasedAuthentication already turned on
        # recommended to use "IgnoreUserKnownHosts yes" and
        # "IgnoreRhosts yes"
        
        # entries are added to "hostbasedconfig" with this syntax:
        # "$hostname"
        # No "+" wildcarding is supported, accounts on one machine may only
        # freely access the same account name on this machine.  A matching
        # /etc/passwd is implied across the whole contextualization group.
        # To fix a problem with reverse DNS lookups and hostbased authN (HBA),
        # also adding an IP line
        
        # entries are added to "knownhostsconfig" with this syntax:
        # "$hostname,$ip $pubkey"
        
        equivpath = self.common.hostbasedconfig
        knownpath = self.common.knownhostsconfig
        
        log.debug("Adjusting equiv policies in '%s'" % equivpath)
        log.debug("Adjusting known keys in '%s'" % knownpath)
        
        equivlines = []
        knownlines = []
        for iden in self.retrresult.identities:
            if iden.ip == None or iden.host == None or iden.pubkey == None:
                log.debug("Skipping identity with IP '%s' because it does "
                          "not have ip and host and pubkey" % iden.ip)
                continue
            equivlines.append("%s" % iden.host)
            equivlines.append("%s" % iden.ip) # fixes rev dns issues for HBA
            knownlines.append("%s,%s %s" % (iden.host, iden.ip, iden.pubkey))
            knownlines.append("%s,%s %s" % (iden.host.split(".")[0], iden.ip, iden.pubkey))
        
        # replace both files -- right now not supporting client rigged
        # known hosts (they could manually adjust after contextualization
        # for now...)
        
        # bail on any error
        f = None
        try:
            try:
                f = open(equivpath, 'w')
                text = ""
                for line in equivlines:
                    text += line
                    text += "\n"
                text += "\n"
                f.write(text)
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                name = str(exceptname)
                err = str(sys.exc_value)
                errmsg = "Problem writing to '%s': " % equivpath
                errmsg += "%s: %s\n" % (name, err)
                raise UnexpectedError(errmsg)
        finally:
            if f:
                f.close()

        f = None
        try:
            try:
                f = open(knownpath, 'w')
                text = ""
                for line in knownlines:
                    text += line
                    text += "\n"
                text += "\n"
                f.write(text)
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                name = str(exceptname)
                err = str(sys.exc_value)
                errmsg = "Problem writing to '%s': " % knownpath
                errmsg += "%s: %s\n" % (name, err)
                raise UnexpectedError(errmsg)
        finally:
            if f:
                f.close()
        
    def handle_roles(self):
        for role in self.retrresult.roles:
            rolename = role.name
            taskpath = os.path.join(self.common.ipandhostdir, rolename)
            if not os.path.exists(taskpath):
                log.info("No ipandhost script for required role %s" % rolename)
                continue
            cmd = "%s %s" % (taskpath, role.ip)
            
            ident = self.locate_identity(role.ip)
            if ident == None:
                # assuming just FOR NOW that this is because IP is all that
                # is needed and there was no all-identities requirement
                # in the contextualization schema.
                log.error("No identity located for '%s'" % role.ip)
            elif ident.host != None:
                cmd += " %s %s" % (ident.host.split(".")[0], ident.host)
            
            (exit, stdout, stderr) = runexe(cmd, killtime=0)
            result = "'%s': exit=%d, stdout='%s'," % (cmd, exit, stdout)
            result += " stderr='%s'" % (stderr)
            
            if self.common.trace:
                log.debug(result)
            
            if exit != 0:
                msg = "PROBLEM: task command failed, "
                msg += "result: %s" % result
                raise UnexpectedError(msg)
    
    def locate_identity(self, ip):
        for ident in self.retrresult.identities:
            if ip == ident.ip:
                return ident
        return None
        
    def data_file(self, taskdir, scratchdir, name, data):
        """Fill a file in directory 'scratchdir' called name prefix 'name' 
        with contents 'data'.  Opaque data field names can repeat so file
        needs to be unique to this bit of data.  Then call task with the
        name if it exists in taskdir"""
        
        if not name:
            raise UnexpectedError("given data instance has no name?")
        if not data:
            log.info("data instance ('%s') has no data (file will be touched)" % name)
        else:
            # ok when only handling text files
            data += "\n"
        
        abspath = None
        p = name + "-"
        try:
            (fd, abspath) = tempfile.mkstemp(prefix=p, dir=scratchdir, text=True)
            log.info("Data file created for '%s': %s" % (name, abspath))
            numbytes = 0
            if data:
                numbytes = os.write(fd, data)
                os.fsync(fd)
                log.info("Wrote %d bytes to %s" % (numbytes, abspath))
            os.close(fd)
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            ename = str(exceptname)
            err = str(sys.exc_value)
            errmsg = "Problem with file creation for data instance of "
            errmsg += "'%s':: %s: %s\n" % (name, ename, err)
            raise UnexpectedError(errmsg)
            
        taskpath = os.path.join(taskdir, name)
        cmd = "%s %s" % (taskpath, abspath)
        
        log.debug("CMD: %s" % cmd)
        
        (exit, stdout, stderr) = runexe(cmd, killtime=0)
        result = "'%s': exit=%d, stdout='%s'," % (cmd, exit, stdout)
        result += " stderr='%s'" % (stderr)
        
        if self.common.trace:
            log.debug(result)
        
        if exit != 0:
            msg = "PROBLEM: data task command failed, "
            msg += "result: %s" % result
            raise UnexpectedError(msg)
        
    def handle_opaquedata(self):
        
        if len(self.retrresult.data) == 0:
            log.debug("No opaque data fields")
            return
            
        if not self.common.datadir:
            msg = "Opaque data task directory not configured but data supplied"
            raise UnexpectedError(msg) # abort
            
        if not os.path.exists(self.common.datadir):
            msg = "Opaque data task directory configured but does not exist ('%s')" % (self.common.datadir)
            raise UnexpectedError(msg) # abort
            
        if not os.path.exists(self.common.scratchdir):
            msg = "No scratchdir for data ('%s')" % (self.common.scratchdir)
            raise UnexpectedError(msg) # abort
        
        for one in self.retrresult.data:
            self.data_file(self.common.datadir, self.common.scratchdir, one.name, one.data)
        
    def handle_thishost(self, finalize=False):
        
        if finalize:
            taskdir = self.common.thishostfinalizedir
            logname = "thishost-finalize"
        else:
            taskdir = self.common.thishostdir
            logname = "thishost"
            
        if taskdir == None:
            log.debug("%s directory not configured" % logname)
            return
            
        if not os.path.exists(taskdir):
            msg = "%s '%s' directory configured but does not exist" % (logname, taskdir)
            raise UnexpectedError(msg)
            
        #    arg1: IP
        #    arg2: short hostname
        #    arg3: FQDN
        
        # list of tuples.
        # (NAME, IP, short hostname, FQDN)
        possible_scripts = []
        
        if self.instresult.iface_name:
            possible_scripts.append( (self.instresult.iface_name,
                                      self.instresult.iface_ip,
                                      self.instresult.iface_short_hostname,
                                      self.instresult.iface_hostname) )
        if self.instresult.iface2_name:
            possible_scripts.append( (self.instresult.iface2_name,
                                      self.instresult.iface2_ip,
                                      self.instresult.iface2_short_hostname,
                                      self.instresult.iface2_hostname) )
        
        if len(possible_scripts) == 0:
            log.error("No identities for %s?" % logname)
            return
            
        for ident in possible_scripts:
            
            log.debug("Trying %s for identity '%s'" % (logname, ident[0]))
            
            taskpath = os.path.join(taskdir, ident[0])
            
            if not os.path.exists(taskpath):
                log.debug("No %s script for identity '%s'" % (logname, ident[0]))
                if self.common.trace:
                    log.debug("Does not exist: '%s'" % taskpath)
                    
                # OK if it's absent, just taken to mean nothing to do
                continue
            
            cmd = "%s %s %s %s" % (taskpath, ident[1], ident[2], ident[3])
            
            (exit, stdout, stderr) = runexe(cmd, killtime=0)
            result = "'%s': exit=%d, stdout='%s'," % (cmd, exit, stdout)
            result += " stderr='%s'" % (stderr)
            
            if self.common.trace:
                log.debug(result)
            
            if exit != 0:
                msg = "PROBLEM: %s command failed, " % logname
                msg += "result: %s" % result
                log.error(msg)
                raise UnexpectedError(msg)
            else:
                log.info("Successfully ran '%s'" % cmd)
        
    def handle_restarts(self):
        
        seenroles = []
        
        for role in self.retrresult.roles:
            
            # there could have been and often are duplicates, only do it
            # once per role name
            if role.name in seenroles:
                continue
            seenroles.append(role.name)
            
            taskpath = os.path.join(self.common.restartdir, role.name)
            if not os.path.exists(taskpath):
                if self.common.trace:
                    log.debug("role '%s' has no restart script configured")
                continue
                
            (exit, stdout, stderr) = runexe(taskpath, killtime=0)
            result = "'%s': exit=%d, stdout='%s'," % (taskpath, exit, stdout)
            result += " stderr='%s'" % (stderr)
            
            if self.common.trace:
                log.debug(result)
            
            if exit != 0:
                msg = "PROBLEM: restart task command failed, "
                msg += "result: %s" % result
                raise UnexpectedError(msg)
            else:
                log.info("Successfully ran '%s'" % taskpath)
        
# }}} END: DefaultConsumeRetrieveResult(Action)
    
# ############################################################
# OK/ERR reporting
# #########################################################{{{
    
def setlogfilepath(path):
    global _logfilepath
    _logfilepath = path

def getlogfilepath():
    try:
        _logfilepath
    except:
        return None
    return _logfilepath
    
class DefaultOK:
    
    def __init__(self, cmd):
        self.runcmd = cmd
        
    def run(self):
        if not self.runcmd:
            errmsg = "no runcmd configured for defaultOK (?)"
            try:
                log.error(errmsg)
            except:
                print >>sys.stderr, errmsg
            return
        
        msg = "Attempting OK report to context broker."
        try:
            log.info(msg)
        except:
            print >>sys.stderr, msg
        
        log.info("CMD: " + self.runcmd)
        
        (exit, stdout, stderr) = runexe(self.runcmd, killtime=10)
        result = "'%s': exit=%d, stdout='%s'," % (self.runcmd, exit, stdout)
        result += " stderr='%s'" % (stderr)
        
        log.debug(result)
        log.info("Reported OK to context broker.")
        
class DefaultERR:
    
    def __init__(self, cmd, templatepath):
        self.runcmd = cmd
        self.templatepath = templatepath
        
    def run(self, errcodestr, errmessage):
        
        if not self.runcmd:
            errmsg = "no runcmd configured for defaultERR (?)"
            try:
                log.error(errmsg)
            except:
                print >>sys.stderr, errmsg
            return
            
        if not self.templatepath:
            errmsg = "no templatepath configured for defaultERR (?)"
            try:
                log.error(errmsg)
            except:
                print >>sys.stderr, errmsg
            return
            
        self.complete_template(errcodestr, errmessage)
            
        msg = "Attempting error report to context broker."
        try:
            log.info(msg)
        except:
            print >>sys.stderr, msg
            
        log.info("CMD: " + self.runcmd)
        
        (exit, stdout, stderr) = runexe(self.runcmd, killtime=10)
        result = "'%s': exit=%d, stdout='%s'," % (self.runcmd, exit, stdout)
        result += " stderr='%s'" % (stderr)
        
        log.info(result)
        log.info("Reported ERROR to context broker.")
        
    def complete_template(self, errcodestr, errmessage):
        text = ""
        f = open(self.templatepath)
        try:
            for line in f:
                text += line
        finally:
            f.close()
            
        text = text.replace("REPLACE_ERRORCODE", str(errcodestr))
        text = text.replace("REPLACE_ERRORMSG", str(errmessage))
        write_repl_file(self.templatepath, text)
        
def set_broker_okaction(instance):
    global _brokerOK
    _brokerOK = instance
    
def get_broker_okaction():
    try:
        _brokerOK
    except:
        return None
    return _brokerOK
    
def set_broker_erraction(instance):
    global _brokerERR
    _brokerERR = instance
    
def get_broker_erraction():
    try:
        _brokerERR
    except:
        return None
    return _brokerERR

# }}} END: OK/ERR reporting
    
# ############################################################
# Configuration objects
# #########################################################{{{

# These are objects that are populated from a combination of
# commandline and config file settings.  Their fields are set
# to control the behavior of the action.

class CommonConf:

    """Class for configurations common to all actions and objects."""

    def __init__(self, trace, evaluate, ipandhostdir, restartdir, polltime, sshdkeypath, scratchdir, retr_template, retr_template2, err_template, err_template2, ok_template, ok_template2, hostbasedconfig, knownhostsconfig, thishostdir, thishostfinalizedir, logfilepath, curlpath, hostnamepath, datadir, etchosts_exe):
        """Set the configurations.

        todo: many of these configs can be scoped out of common, the list
              has gotten long ...
                     
        Required parameters:

        * trace -- Make extra log statements to DEBUG level (boolean).

        * evaluate -- "Dryrun" mode (boolean).
        
        * ipandhostdir -- The directory with role-specific task scripts that
        accept IP as arg1 and hostname as arg2
        
        * restartdir -- The directory with role-specific task scripts that,
        if they exist, will be called after a role has received all new
        information (presumably to restart or start the service with its
        new config).
        
        * polltime -- Poll delay in seconds.
        
        * sshdkeypath -- Path to created sshd pubkey.
        
        * scratchdir -- Directory for writing temporary files to.
        
        * retr_template -- Template XML file for WS retrieval
        
        * retr_template2 -- Template XML file for WS retrieval
        
        * err_template -- Template XML file for WS error reporting
        
        * err_template2 -- Template XML file for WS error reporting
        
        * ok_template -- Template XML file for WS OK reporting
        
        * ok_template2 -- Template XML file for WS OK reporting
        
        * hostbasedconfig -- for adding hostnames to do host based authz
        
        * knownhostsconfig -- for adding pubkeys to do host based authz
        
        * thishostdir -- 'thishost' directory, see config file
        
        * thishostfinalizedir -- 'thishostfinalize' dir, see config file
        
        * logfilepath -- Path to write log file
        
        * curlpath -- relative or abs command for curl
        
        * hostnamepath -- relative or abs command for hostname
        
        * datadir -- Directory with data specific task scripts that will be
        called when a data field is present in the context information.
        
        * etchosts_exe -- path to send all identity info

        """

        self.trace = trace
        self.evaluate = evaluate
        
        try:
            polltime = int(polltime)
        except:
            raise InvalidConfig("polltime is required to be an integer (number of seconds)")
            
        self.polltime = polltime
        self.sshdkeypath = sshdkeypath
        self.scratchdir = scratchdir
        self.hostbasedconfig = hostbasedconfig
        self.knownhostsconfig = knownhostsconfig
        self.ipandhostdir = ipandhostdir
        self.restartdir = restartdir
        self.thishostdir = thishostdir
        self.thishostfinalizedir = thishostfinalizedir
        self.logfilepath = logfilepath
        self.curlpath = curlpath
        self.hostnamepath = hostnamepath
        self.datadir = datadir
        self.etchosts_exe = etchosts_exe
        
        # On the crazier side of things -- perhaps use ZSI but we also might
        # not use WS at all next round, so sticking with text-over-http hack
        # One important characteristic: a very low dependency situation (curl)
        self.retr_template = retr_template
        self.retr_template2 = retr_template2
        self.err_template = err_template
        self.err_template2 = err_template2
        self.ok_template = ok_template
        self.ok_template2 = ok_template2
        
class AmazonConf:

    """Class for amazon configurations, version 1."""

    def __init__(self, localhostnameURL, publichostnameURL, localipURL, publicipURL, publickeysURL, userdataURL):
        """Set the configurations.

        Required parameters:

        * localhostnameURL -- The 

        * publichostnameURL -- The 
        
        * localipURL --  The
        
        * publicipURL -- The
        
        * publickeysURL -- The
        
        * userdataURL -- The

        Raise InvalidConfig if there is a problem with parameters.

        """
        
        self.localhostnameURL = localhostnameURL
        self.publichostnameURL = publichostnameURL
        self.localipURL = localipURL
        self.publicipURL = publicipURL
        self.publickeysURL = publickeysURL
        self.userdataURL = userdataURL

class ReginstConf:

    """Class for regular configurations, version 1."""

    def __init__(self, path, niclist):
        """Set the configurations.

        Required parameters:

        * path -- Path to bootstrap file
        
        * niclist -- list of possible nic names

        Raise InvalidConfig if there is a problem with parameters.

        """
        
        if not path:
            raise InvalidConfig("No bootstrap path, illegal argument")
        
        name = "metadata server URL path"
        if not os.path.exists(path):
            msg = "%s '%s' does not exist on filesystem" % (name, path)
            raise InvalidConfig(msg)
            
        if not os.path.isabs(path):
            msg = "%s '%s' should be absolute path" % (name, path)
            raise InvalidConfig(msg)
            
        self.path = path
        
        if not isinstance(niclist, list):
            raise InvalidConfig("niclist must be a list")
            
        if len(niclist) == 0:
            raise InvalidConfig("niclist must not be empty")
            
        self.niclist = niclist

# }}} END: Configuration objects

# ############################################################
# Convert configurations
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
        
    polltime = POLL_DELAY_SECONDS_DEFAULT
    if opts.polltime:
        polltime = opts.polltime
        
    try:
        sshdkeypath = config.get("sshd", "generatedkey")
        hostbasedconfig = config.get("sshd", "hostbasedconfig")
        knownhostsconfig = config.get("sshd", "knownhostsconfig")
        scratchdir = config.get("ctxservice", "scratchspacedir")
        retr_template = config.get("ctxservice", "retr_template")
        retr_template2 = config.get("ctxservice", "retr_template2")
        err_template = config.get("ctxservice", "err_template")
        err_template2 = config.get("ctxservice", "err_template2")
        ok_template = config.get("ctxservice", "ok_template")
        ok_template2 = config.get("ctxservice", "ok_template2")
        ipandhostdir = config.get("taskpaths", "ipandhostdir")
        restartdir = config.get("taskpaths", "restartdir")
        thishostdir = config.get("taskpaths", "thishostdir")
        thishostfinalizedir = config.get("taskpaths", "thishostfinalizedir")
        logfilepath = config.get("ctxservice", "logfilepath")
        curl = config.get("systempaths", "curl")
        hostname = config.get("systempaths", "hostname")
        datadir = config.get("taskpaths", "datadir")
        etchosts_exe = config.get("taskpaths", "etchosts")
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        msg = "%s: %s" % (str(exceptname), str(sys.exc_value))
        raise InvalidConfig(msg)
        
    # no evaluate yet, pass False for now
    return CommonConf(opts.trace, False, ipandhostdir, restartdir, polltime, sshdkeypath, scratchdir, retr_template, retr_template2, err_template, err_template2, ok_template, ok_template2, hostbasedconfig, knownhostsconfig, thishostdir, thishostfinalizedir, logfilepath, curl, hostname, datadir, etchosts_exe)

def getReginstConf(opts, config):
    """Return populated reginstConf object or raise InvalidConfig

    Required parameters:

    * opts -- parsed optparse opts

    * config -- parsed ConfigParser
    
    Raise InvalidConfig if there is a problem.

    """
    
    if not opts:
        raise ProgrammingError("opts is None")
    if not config:
        raise ProgrammingError("config is None")
        
    path = opts.bootstrap_path
    
    try:
        # commandline takes precendence
        if not path:
            path = config.get("reginst", "path")
        nicnames = config.get("reginst", "nicnames")
        niclist = map(string.strip, nicnames.split(","))
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        msg = "%s: %s" % (str(exceptname), str(sys.exc_value))
        raise InvalidConfig(msg)
    
    return ReginstConf(path, niclist)
    
def getAmazonConf(opts, config):
    """Return populated AmazonConf object or raise InvalidConfig

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
        localhostnameURL = config.get("ec2", "localhostnameURL")
        publichostnameURL = config.get("ec2", "publichostnameURL")
        localipURL = config.get("ec2", "localipURL")
        publicipURL = config.get("ec2", "publicipURL")
        publickeysURL = config.get("ec2", "publickeysURL")
        userdataURL = config.get("ec2", "userdataURL")
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        msg = "%s: %s" % (str(exceptname), str(sys.exc_value))
        raise InvalidConfig(msg)
    
    return AmazonConf(localhostnameURL, publichostnameURL, localipURL, publicipURL, publickeysURL, userdataURL)
    
# }}} END: Convert configurations

# ############################################################
# External configuration
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
    
# }}} END: External configuration

# ############################################################
# Commandline arguments
# #########################################################{{{

class ARGS:

    """Class for command-line argument constants"""

    AMAZON="--amazon"
    REGULAR="--regular"
    TRYALL="--tryall"
    
    BOOTSTRAP_PATH="--bootstrap-path"
    POLLTIME="--polltime"
    TRACE="--trace"
    CONFIGPATH="--configpath"
    POWEROFF="--poweroff"

def parsersetup():
    """Return configured command-line parser."""

    ver="Workspace VM context agent %s, http://workspace.globus.org" % VERSION
    usage="see help (-h)."
    parser = optparse.OptionParser(version=ver,usage=usage)

    # ----

    group = optparse.OptionGroup(parser,  "Output options", "-------------")

    group.add_option("-q", "--quiet",
                      action="store_true", dest="quiet", default=False,
                      help="don't log any messages (unless error occurs).")

    group.add_option("-v", "--verbose",
                      action="store_true", dest="verbose",
                      default=False, help="log debug messages")

    group.add_option("-t", ARGS.TRACE,
                      action="store_true", dest="trace", default=False,
                      help="log all debug messages")

    parser.add_option_group(group)

    # ----

    group = optparse.OptionGroup(parser,
                                 "Required action, one is required",
                                 "-------------------------------")

    group.add_option("-z", ARGS.TRYALL,
                     action="store_true", dest="tryall",
                     default=False,
                     help="Try all environments.  Currently there are two. "
                          "The order is to operate in normal cluster " "environment first, then sense Amazon")

    group.add_option("-r", ARGS.REGULAR,
                     action="store_true", dest="regular", default=False,
                     help="Operate in normal cluster environment")

    group.add_option("-a", ARGS.AMAZON,
                     action="store_true", dest="amazon", default=False,
                     help="Operate in Amazon EC2 environment")
                     
    parser.add_option_group(group)

    # ----

    group = optparse.OptionGroup(parser,
                                 "Optional arguments",
                                 "------------------")
    
    group.add_option("-b", ARGS.BOOTSTRAP_PATH, 
                     metavar="PATH", 
                     dest="bootstrap_path", 
                     help="Used with normal cluster environment, this will "
                          "override the default path to local metadata URL "
                          "file")

    group.add_option("-s", ARGS.POLLTIME,
                     dest="polltime", metavar="SECS",
                     help="The number of seconds between polls.")
                     
    group.add_option("-p", ARGS.POWEROFF,
                     action="store_true", dest="poweroff", default=False,
                     help="If there is any problem, the 'problem' script "
                          "in the task directory will be called -- "
                          "this will usually be set to poweroff the VM.")
                            
    group.add_option("-c", ARGS.CONFIGPATH,
                    dest="configpath", metavar="PATH",
                    help="Path to configuration file that overrides the "
                    "default.  If there is a problem with the supplied "
                    "configs, the program will NOT fall back to "
                    "the defaults.")
                          
    parser.add_option_group(group)
    
    return parser

def validateargs(opts):
    """Validate command-line argument combination.

    Required arguments:

    * opts -- Parsed optparse opts object

    Raise InvalidInput if there is a problem.

    """

    actions = [opts.regular, opts.amazon, opts.tryall]

    count = 0
    for action in actions:
        if action:
            count += 1

    seeh = "see help (-h)"

    if not count:
        raise InvalidInput("You must supply an action, %s." % seeh)

    if count != 1:
        raise InvalidInput("You may only supply one action, %s." % seeh)

# }}} END: Commandline arguments

# ############################################################
# Standalone entry and exit
# #########################################################{{{

def mainrun(argv=None):
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

    loglevel = None
    if opts.verbose or opts.trace:
        loglevel = logging.DEBUG
    elif opts.quiet:
        loglevel = logging.ERROR
    else:
        loglevel = logging.INFO
    configureLogging(loglevel, trace=opts.trace)

    try:
        validateargs(opts)

        #if opts.evaluate:
        #    log.info("EVALUATE MODE ENABLED")

        if opts.configpath:
            config = getconfig(filepath=opts.configpath)
        else:
            config = getconfig(string=DEFAULTCONFIG)
            
        commonconf = getCommonConf(opts, config)
        
        if commonconf.logfilepath:
            addFileLogging(log, commonconf.logfilepath, None, loglevel, trace=opts.trace)
            setlogfilepath(commonconf.logfilepath)
            log.debug("[file logging enabled @ '%s'] " % commonconf.logfilepath)

        if opts.tryall:
            log.debug("action %s" % ARGS.TRYALL)
        elif opts.amazon:
            log.debug("action %s" % ARGS.AMAZON)
        elif opts.regular:
            log.debug("action %s" % ARGS.REGULAR)
            
        if opts.poweroff:
            log.info("OK to poweroff")
            try:
                problemscript = config.get("taskpaths", "problemscript")
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                msg = "%s: %s" % (str(exceptname), str(sys.exc_value))
                raise InvalidConfig(msg)
            
            setterminateok(problemscript)
        
        #######################################
        ##  I. Run one Instantiation action  ##
        #######################################
        
        # try-all could be replaced by supporting multiple action flags and
        # having an order (the order itself could be set via conf)
        iactionresult = None
        if not opts.tryall:
        
            if opts.regular:
                regconf = getReginstConf(opts, config)
                iaction = RegularInstantiation(commonconf, regconf)
            elif opts.amazon:
                ec2conf = getAmazonConf(opts, config)
                iaction = AmazonInstantiation(commonconf, ec2conf)
                
            log.info("Running instantiation action")
            iaction.run()
            iactionresult = iaction.result
        
        else:
            
            # embedded run order right now
            try:
                log.info("First running regular instantiation action")
                regconf = getReginstConf(opts, config)
                reg_iaction = RegularInstantiation(commonconf, regconf)
                reg_iaction.run()
                iactionresult = reg_iaction.result
            except:
                msg = "Problem with regular instantiation action: "
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                msg += "%s: %s" % (str(exceptname), str(sys.exc_value))
                log.error(msg)
                
                log.info("Second, running Amazon instantiation action")
                
                ec2conf = getAmazonConf(opts, config)
                ec2_iaction = AmazonInstantiation(commonconf, ec2conf)
                ec2_iaction.run()
                iactionresult = ec2_iaction.result
            
        # If there was an issue, exception should have been thrown:
        if iactionresult == None:
            raise ProgrammingError("Instantiation action(s) ran to completion but no result?")
            
            
        #############################################################
        ## II. Run one Retrieval action                            ##
        ##     The Instantiation action throws an exception or     ##
        ##     places InstantiationResult in its "result" field.   ##
        #############################################################
        
        ractionresult = None
        
        # only one impl right now:
        raction = DefaultRetrieveAction(commonconf, iactionresult)
        log.info("Running retrieval action")
        raction.run()
        ractionresult = raction.result
        
        if ractionresult == None:
            raise ProgrammingError("Retrieve Action ran to completion but no result?")
            
            
        ###############################################################
        ## III. Run one Consumption action                           ##
        ##      The Retrieval action either throws an exception or   ##
        ##      places RetrieveResult object in its "result" field.  ##
        ###############################################################
            
        # only one impl right now:
        caction = DefaultConsumeRetrieveResult(commonconf, ractionresult, iactionresult)
        log.info("Running consume action")
        caction.run()
        
        return 0

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
        msg += "   If this is a non-modified release, please report all"
        msg += "   following output:"
        msg += "   MESSAGE: %s" % e.msg
        if log:
            log.critical(msg)
        else:
            print >>sys.stderr, msg
        traceback.print_tb(sys.exc_info()[2])
        return 42
        
def attempt_ok_broker():
    try:
        ok = get_broker_okaction()
        if not ok:
            # shouldn't be possible, since all would not be OK...
            print >>sys.stderr, "OK action was not configured?"
            return
            
        ok.run()
        
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        name = str(exceptname)
        err = str(sys.exc_value)
        errmsg = "==> Problem sending success report to context broker: %s: %s\n" % (name, err)
        print >>sys.stderr, errmsg

def attempt_error_broker(errcode):
    try:
        filepath = getlogfilepath()
        if not filepath or not os.path.exists(filepath):
            print >>sys.stderr, "==> Problem and no log was available to send to context broker as error report."
            return
            
        erraction = get_broker_erraction()
        if not erraction:
            print >>sys.stderr, "No error reporting action was configured, cannot inform context broker of this problem."
            return
            
        text = ""
        f = open(filepath)
        try:
            for line in f:
                text += line
        finally:
            f.close()
            
        erraction.run(str(errcode), text)
            
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        name = str(exceptname)
        err = str(sys.exc_value)
        errmsg = "==> Problem running broker error report: %s: %s\n" % (name, err)
        print >>sys.stderr, errmsg
    
def attempt_problem_script():
    try:
        problemscript = terminateok()
        if not problemscript:
            # (If the problem was before options were parsed, there is no
            # way to locate the poweroff script)
            return
            
        print >>sys.stderr, "==> Problem and problem script is configured. Running it:\n"
        runexe(problemscript, killtime=0)
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        name = str(exceptname)
        err = str(sys.exc_value)
        errmsg = "==> Problem running terminate script: %s: %s\n" % (name, err)
        print >>sys.stderr, errmsg

def main(argv=None):
    exitcode = mainrun(argv) # run
    if not exitcode:
        attempt_ok_broker() # notify ctx broker of success
    else:
        attempt_error_broker(exitcode) # notify ctx broker of failure
        attempt_problem_script() # do something in VM after failure
    return exitcode
    
if __name__ == "__main__":
    
    try:
        sys.exit(main())
    except SystemExit:
        raise
    except KeyboardInterrupt:
        raise
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        name = str(exceptname)
        err = str(sys.exc_value)
        errmsg = "\n==> Uncaught problem, please report all following output:\n  %s: %s" % (name, err)
        print >>sys.stderr, errmsg
        traceback.print_tb(sys.exc_info()[2])
        sys.exit(97)

# }}} END: Standalone entry and exit


