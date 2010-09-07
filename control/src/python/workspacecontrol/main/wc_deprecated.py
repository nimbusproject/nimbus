from workspacecontrol.api.exceptions import *
from workspacecontrol.main import get_all_configs, ACTIONS

# special opts knowledge needed

def deprecated_args(opts):
    """Check for deprecated args and take the appropriate action.
    Return debug message if something is notable"""
    
    if opts.startpaused:
        raise InvalidInput("The start-paused argument has been retired.")
    if opts.ramdisk:
        raise InvalidInput("The ramdisk argument has been retired.")
        
    actionmsg = _actionargs(opts)
    ignoremsg = _ignoreargs(opts)
    
    dbgmsg = ""
    if actionmsg:
        dbgmsg += actionmsg + "\n"
    if ignoremsg:
        dbgmsg += ignoremsg + "\n"
    return dbgmsg
    
def _actionargs(opts):
    """Backwards compatibility for action triggers.
    Converts action to '--action' form for internal consistency.
    Returns a string for debug if there was a switch"""
    
    count = 0
    ret = None
    
    if opts.create:
        count += 1
        ret = "Converted '--%s' to '--action %s'" % ("create", ACTIONS.CREATE)
        opts.action = ACTIONS.CREATE
    if opts.remove:
        count += 1
        ret = "Converted '--%s' to '--action %s'" % ("remove", ACTIONS.REMOVE)
        opts.action = ACTIONS.REMOVE
    if opts.info:
        count += 1
        ret = "Converted '--%s' to '--action %s'" % ("info", ACTIONS.INFO)
        opts.action = ACTIONS.INFO
    if opts.reboot:
        count += 1
        ret = "Converted '--%s' to '--action %s'" % ("reboot", ACTIONS.REBOOT)
        opts.action = ACTIONS.REBOOT
    if opts.pause:
        count += 1
        ret = "Converted '--%s' to '--action %s'" % ("pause", ACTIONS.PAUSE)
        opts.action = ACTIONS.PAUSE
    if opts.unpause:
        count += 1
        ret = "Converted '--%s' to '--action %s'" % ("unpause", ACTIONS.UNPAUSE)
        opts.action = ACTIONS.UNPAUSE
    if opts.propagate:
        count += 1
        ret = "Converted '--%s' to '--action %s'" % ("propagate", ACTIONS.PROPAGATE)
        opts.action = ACTIONS.PROPAGATE
    if opts.unpropagate:
        count += 1
        ret = "Converted '--%s' to '--action %s'" % ("unpropagate", ACTIONS.UNPROPAGATE)
        opts.action = ACTIONS.UNPROPAGATE
        
    if count > 1:
        raise InvalidInput("You may not specify more than one action")
        
    return ret
        
def _ignoreargs(opts):
    """Return string of ignored arg warnings, for printing later"""
    ret = ""
    if opts.loglevel:
        ret += "|loglevel|"
    if opts.longhelp:
        ret += "|longhelp|"
    if opts.persistencedir:
        ret += "|persistencedir|"
    if opts.workspaceimpl:
        ret += "|workspaceimpl|"
    if opts.checkshutdown:
        ret += "|checkshutdown|"
    if opts.checkshutdownpause:
        ret += "|checkshutdownpause|"
    
    if ret:
        return "Ignored arguments: %s" % ret
    else:
        return None
