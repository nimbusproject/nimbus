import os
import resource
from workspacecontrol.api.exceptions import *

# Default maximum for the number of available file descriptors.
MAXFD = 1024

# The standard I/O file descriptors are redirected to /dev/null by default.
if (hasattr(os, "devnull")):
    REDIRECT_TO = os.devnull
else:
    REDIRECT_TO = "/dev/null"
    
def daemonize(common, func, funcargs):
    
    if not common:
        raise ProgrammingError("common is required")
        
    if not func:
        raise ProgrammingError("daemonizing with no work to do?")

    common.log.warn("Daemonizing, goodbye.")

    #all log entries are duplicated without closing here first
    common.close_logfile()

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

    common.reopen_logfile()
    common.log.info("\n\n## Process has daemonized, file logging is now re-enabled.\n\n")

    if not funcargs:
        func()
    else:
        func(*funcargs)
