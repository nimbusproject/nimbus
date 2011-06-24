import commands
import os
import stat
import sys
import uuid
from commands import getstatusoutput

from pynimbusconfig.setuperrors import *

def ensure_file_exists(path, name, extramsg=None):
    _ensure_exists(path, name, extramsg, "file")
        
def ensure_dir_exists(path, name, extramsg=None):
    _ensure_exists(path, name, extramsg, "directory")
        
def _ensure_exists(path, name, extramsg, ptype):
    if not check_path_exists(path):
        msg = "%s %s '%s' does not exist" % (name, ptype, path)
        if extramsg:
            msg += extramsg
        raise IncompatibleEnvironment(msg)
        
    adir = os.path.isdir(path)
    afile = not adir
    if ptype == "file":
        if not afile:
            msg = "%s %s '%s' is not a file" % (name, ptype, path)
            raise IncompatibleEnvironment(msg)
    elif ptype == "directory":
        if not adir:
            msg = "%s %s '%s' is not a directory" % (name, ptype, path)
            raise IncompatibleEnvironment(msg)
    else:
        raise IncompatibleEnvironment("Unknown path type '%s'" % ptype)
        
def check_path_exists(path):
    return os.path.exists(path)
    
def is_absolute_path(path):
    """works on abstract paths, they do not need to exist"""
    return os.path.isabs(path)
    
def pathjoin(above, below):
    if os.path.isabs(below):
        raise InvalidInput("Absolute paths on the 'right side' produce unexpected result")
    return os.path.join(above, below)

def uuidgen():
    return str(uuid.uuid4())
        
def make_path_rw_private(path):
    mode = stat.S_IRUSR | stat.S_IWUSR
    os.chmod(path, mode)
    
def ensure_path_private(path, name):
    if not is_path_private(path):
        raise IncompatibleEnvironment("%s expected to be private: %s" % (name, path))
    
def is_path_private(path):
    statresult = os.stat(path)
    midx = stat.ST_MODE
    mode = statresult[midx]
    mode = stat.S_IMODE(mode)
    if not mode & stat.S_IRUSR:
        return False
    for i in ("GRP", "OTH"):
        for perm in "R", "W", "X":
            if mode & getattr(stat, "S_I"+ perm + i):
                return False
    return True
        
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
    
def write_repl_file(path, outputtext, log):
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
        #log.error(errmsg)
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
            #log.error(errmsg)
            raise UnexpectedError(errmsg)
    finally:
        if f:
            f.close()

    log.debug("Wrote '%s'." % path)

# from Python 2.6 standard library, slightly modified
def _relpath(path, start):
    """Return a relative version of a path"""

    if not path:
        raise ValueError("no path specified")
    if not start:
        raise ValueError("no start specified")

    start_list = os.path.abspath(start).split(os.path.sep)
    path_list = os.path.abspath(path).split(os.path.sep)

    # Work out how much of the filepath is shared by start and path.
    i = len(os.path.commonprefix([start_list, path_list]))

    rel_list = [os.path.pardir] * (len(start_list)-i) + path_list[i:]
    if not rel_list:
        return curdir
    return os.path.join(*rel_list)

relpath = None
try:
    # os.path.relpath is in 2.6+
    relpath = getattr(os.path, 'relpath')
except AttributeError:
    relpath = _relpath
