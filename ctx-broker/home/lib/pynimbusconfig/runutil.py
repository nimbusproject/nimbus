import os
import signal
import sys
import time

from pynimbusconfig.setuperrors import *

try:
    from threading import Thread
except ImportError:
    from dummy_threading import Thread

from commands import getstatusoutput

# not using subprocess package to maintain at least python 2.3 compat.
from popen2 import Popen3

def generic_bailout(header, exitcode, stdout, stderr):
    if exitcode != 0:
        # header something like "Problem creating CA."
        msg = "%s\n" % header
        if stdout:
            msg += "[<<< stdout: '%s'\n" % stdout
        if stderr:
            msg += "[<<< stderr: '%s'\n" % stderr
        raise UnexpectedError(msg)

class SimpleRunThread(Thread):
    """Run a command with timeout options, delay, stdin, etc."""
    
    def __init__ (self, cmd, log, killsig=-1, killtime=0, stdin=None, delay=None):
        """Populate the thread.
        
        Required parameters:
        
        * cmd -- command to run
        
        * log -- logger
        
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
        self.log = log
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
                
        self.exit = p.wait() >> 8
        self.stdout = p.fromchild.read()
        self.stderr = p.childerr.read()
        p.fromchild.close()
        p.childerr.close()
        self.log.debug("program ended: '%s'" % self.cmd)
        
def runexe(cmd, log, killtime=2.0):
    """Run a system program.
    
    Required parameter:
    
    * cmd -- command to run, string
    
    * log -- logger
    
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
        thr = SimpleRunThread(cmd, log, killsig=signal.SIGKILL, killtime=killtime)
    else:
        thr = SimpleRunThread(cmd, log)
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
