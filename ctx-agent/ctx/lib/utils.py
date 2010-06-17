import os
import sys
import time
import stat
import signal
import struct
import fcntl
import socket
import commands

try:
    from threading import Thread
except ImportError:
    from dummy_threading import Thread

# not using subprocess package to maintain at least python 2.3 compat.
from popen2 import Popen3

#local imports
from ctx_exceptions import UnexpectedError, IncompatibleEnvironment
from ctx_logging import getlog


# #########################################################
# Timer
# #########################################################
def starttimer():
    """Record current time"""
    global _t0
    _t0 = time.time()
    return None


# #########################################################
# Termination
# #########################################################

def setterminateok(problemscript):
    global _terminateOK
    _terminateOK = problemscript

def terminateok():
    try:
        _terminateOK
    except:
        return None
    return _terminateOK

# #########################################################
# log path utils
# #########################################################
 
def setlogfilepath(path):
    global _logfilepath
    _logfilepath = path

def getlogfilepath():
    try:
        _logfilepath
    except:
        return None
    return _logfilepath
    
 
# #########################################################
# Path/system utilities
# #########################################################

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
    
    def __init__(self, cmd, killsig=-1, killtime=0, stdin=None, delay=None, log_override=None):
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
        self.log = getlog(override=log_override)
        
    def run(self):
        if self.delay:
            self.log.debug("delaying for %.3f secs: '%s'" % (self.delay, self.cmd))
            time.sleep(self.delay)
        self.log.debug("program starting '%s'" % self.cmd)
	p = Popen3(self.cmd, True)
        if self.stdin:
            if p.poll() == -1:
                p.tochild.write(self.stdin)
                p.tochild.flush()
                p.tochild.close()
                #log.debug("wrote '%s' to child" % self.stdin)
            else:
                self.log.error("child exited before stdin was written to")
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
                self.log.exception("problem killing")
                self.exception = e
                return
                
        status = p.wait()
        if os.WIFSIGNALED(status):
            self.exit = "SIGNAL: " + str(os.WTERMSIG(status))
        elif os.WIFEXITED(status):
            self.exit = str(os.WEXITSTATUS(status))
        else:
            self.exit = "UNKNOWN"
            
        self.stdout = p.fromchild.read()
        self.stderr = p.childerr.read()
        p.fromchild.close()
        p.childerr.close()
        self.log.debug("program ended: '%s'" % self.cmd)
        
def runexe(cmd, killtime=2.0, retry=0):
    """Run a system program.
    
    Required parameter:
    
    * cmd -- command to run, string
    
    * retry -- how many retry we will do when the exit status is non-zero
    Default is 0.
    
    Keyword parameter:
    
    * killtime -- how many seconds to wait before SIGKILL (int or float)
    Default is 2.0 seconds.
    
    Return (exitcode, stdout, stderr)
    
    * exitcode -- string exit code or msg
    
    * stdout -- stdout or None
    
    * stderr -- stderr or None
    
    Raises IncompatibleEnvironment for serious issue (but not on non-zero exit)
    
    """
    
    for i in range(retry+1):
        if killtime > 0:
            thr = SimpleRunThread(cmd, killsig=signal.SIGKILL, killtime=killtime)
        else:
            thr = SimpleRunThread(cmd)
        thr.start()
        thr.join()
    
        # sudo child won't take signals
        if thr.exception:
            raise IncompatibleEnvironment(str(thr.exception))

        if thr.exit == "0":
            break
        else:
            time.sleep(0.5)
        
    return (thr.exit, thr.stdout, thr.stderr)
       

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

   
def write_repl_file(path, outputtext, log_override=None):
    """TODO: switch this to use tempfile.mkstemp"""
    log = getlog(override=log_override)
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
