# #########################################################
# Logging
# #########################################################

import time
import sys
import logging

#local imports
from ctx_exceptions import IncompatibleEnvironment


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

    if trace and tracemessage:
        logger.debug(tracemessage)
    return logger

