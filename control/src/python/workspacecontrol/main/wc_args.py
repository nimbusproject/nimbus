import string

a = []
ALL_WC_ARGS_LIST = a

class ControlArg:
    
    def __init__(self, name, short_syntax, noval=False, since=None, deprecated=False, createarg=True, metavar=None):
        """Long syntax is always "--" + name
        
        short_syntax may be None
        
        If 'noval' is True, this is an arg that, if present, will trigger the
        value to be 'True', otherwise 'False'.  i.e., "was the flag present."
        Otherwise, it is presumed a string intake arg
        
        createarg -- Most of the arguments are for the create action. Use a
        False createarg so they are grouped differently from the create args
        
        if no metavar (see wc_optparse), metavar is capitalization of name
        """
        if not name:
            raise Exception("no arg name")
            
        self.name = name
        self.dest = name
        if not since:
            self.since = "v1"
        else:
            self.since = "v%s" % since
        self.short_syntax = short_syntax
        self.long_syntax = "--" + name
        self.help = None
        self.boolean = noval
        self.string = not noval
        self.deprecated = deprecated
        self.createarg = createarg
        self.metavar = metavar
        if not metavar:
            self.metavar = string.upper(name)
    
    def __repr__(self):
        return "ControlArg: %s" % self.name


################################################################################
# WC ARGUMENTS
#
# The following cmdline arguments may be queried via Parameters, using either
# the 'name' as the argument or simply the object like:
#   
#   params.get_arg_or_none(wc_args.KERNEL)
# 
################################################################################

ACTION = ControlArg("action", "-a", since="2.3", createarg=False)
ACTION.help = "Action for the program to take: create, remove, info, reboot, pause, unpause, propagate, unpropagate, printxml"
a.append(ACTION)

CONF = ControlArg("conf", "-c", createarg=False, metavar="PATH")
CONF.help = "Absolute path to main.conf.  * Always required *"
a.append(CONF)

DELETE_ALL = ControlArg("deleteall", None, createarg=False, noval=True)
DELETE_ALL.help = "Used with remove action to trigger an ungentle destruction"
a.append(DELETE_ALL)

DRYRUN = ControlArg("dryrun", None, createarg=False, noval=True)
DRYRUN.help = "Do as little as possible 'for real', will still affect filesystem, for example logs and information persistence"
a.append(DRYRUN)

EXTRA_ARGS = ControlArg("prop-extra-args", None)
EXTRA_ARGS.help = "addition arguments passed to the propagation adapter.  The format is deterimed by the propagation adapter in use."
a.append(EXTRA_ARGS)

KERNEL = ControlArg("kernel", "-k", metavar="FILENAME (not path)")
KERNEL.help = "Override the configured kernel"
a.append(KERNEL)

KERNELARGS = ControlArg("kernelargs", None)
KERNELARGS.help = "If allowed, send additional args to kernel at boot"
a.append(KERNELARGS)

IMAGES = ControlArg("images", "-i", metavar="FILESPEC(s)")
IMAGES.help = "Image spec(s) for the action.  See examples."
a.append(IMAGES)

IMAGEMOUNTS = ControlArg("imagemounts", None, metavar="MOUNTPOINT")
IMAGEMOUNTS.help = "For create action, where to mount each file? See examples, list length must match '%s' list." % IMAGES.long_syntax
a.append(IMAGEMOUNTS)

MEMORY = ControlArg("memory", "-m", metavar="MB")
MEMORY.help = "VM memory in MB"
a.append(MEMORY)

MOUNT_TASKS = ControlArg("mnttasks", None, metavar="TASKLIST")
MOUNT_TASKS.help = "List of mount+edit tasks to perform before VM launch"
a.append(MOUNT_TASKS)

NAME = ControlArg("name", "-n", metavar="VM_NAME", createarg=False)
NAME.help = "Unique VM handle for management"
a.append(NAME)

NETWORKING = ControlArg("networking", None, metavar="NETSPEC(s)")
NETWORKING.help = "NIC spec(s) for the VM"
a.append(NETWORKING)

NOTIFY = ControlArg("notify", None, metavar="user@host:port/path")
NOTIFY.help = "Required for async actions"
a.append(NOTIFY)

UNPROPTARGETS = ControlArg("unproptargets", None, metavar="FILESPEC(s)")
UNPROPTARGETS.help = "Use to 'save-as' a file"
a.append(UNPROPTARGETS)

VALIDATE_ONLY = ControlArg("validate-only", None, since="2.3", createarg=False, noval=True)
VALIDATE_ONLY.help = "Run through validation routines and exit, only a log file will be created"
a.append(VALIDATE_ONLY)

VCPUS = ControlArg("vcpus", None, since="2.3", metavar="NUM")
VCPUS.help = "Number of vcpus to assign the VM, overrides configuration"
a.append(VCPUS)

CACHECKSUM = ControlArg("cachecksum", None, since="2.7.1", metavar="STRING")
CACHECKSUM.help = "This argument enables a cache lookup for a propagated image based on the associated argument which is the key to the cache.  An md5sum checksum is assumed"
a.append(CACHECKSUM)

################################################################################
# DEPRECATED ARGUMENTS
#
# The following cmdline arguments may NOT be queried via Parameters
# They are ONLY used on the cmdline, mostly arguments held over for backwards
# compatibility.
################################################################################

# actions move to the --action argument:

DEPRECATED_CREATE = ControlArg("create", None, deprecated=True, noval=True)
DEPRECATED_CREATE.help = "Works, but use '--action create' from now on"
a.append(DEPRECATED_CREATE)

DEPRECATED_REMOVE = ControlArg("remove", None, deprecated=True, noval=True)
DEPRECATED_REMOVE.help = "Works, but use '--action remove' from now on"
a.append(DEPRECATED_REMOVE)

DEPRECATED_INFO = ControlArg("info", None, deprecated=True, noval=True)
DEPRECATED_INFO.help = "Works, but use '--action info' from now on"
a.append(DEPRECATED_INFO)

DEPRECATED_REBOOT = ControlArg("reboot", None, deprecated=True, noval=True)
DEPRECATED_REBOOT.help = "Works, but use '--action reboot' from now on"
a.append(DEPRECATED_REBOOT)

DEPRECATED_PAUSE = ControlArg("pause", None, deprecated=True, noval=True)
DEPRECATED_PAUSE.help = "Works, but use '--action pause' from now on"
a.append(DEPRECATED_PAUSE)

DEPRECATED_UNPAUSE = ControlArg("unpause", None, deprecated=True, noval=True)
DEPRECATED_UNPAUSE.help = "Works, but use '--action unpause' from now on"
a.append(DEPRECATED_UNPAUSE)

DEPRECATED_PROPAGATE = ControlArg("propagate", None, deprecated=True, noval=True)
DEPRECATED_PROPAGATE.help = "Works, but use '--action propagate' from now on"
a.append(DEPRECATED_PROPAGATE)

DEPRECATED_UNPROPAGATE = ControlArg("unpropagate", None, deprecated=True, noval=True)
DEPRECATED_UNPROPAGATE.help = "Works, but use '--action unpropagate' from now on"
a.append(DEPRECATED_UNPROPAGATE)


# Arguments that are both deprecated and not working. These arguments are taken
# in and rejected with special messages.

DEPRECATED_STARTPAUSED = ControlArg("startpaused", None, deprecated=True, noval=True)
DEPRECATED_STARTPAUSED.help = "Will not work anymore"
a.append(DEPRECATED_STARTPAUSED)

DEPRECATED_RAMDISK = ControlArg("ramdisk", None, deprecated=True)
DEPRECATED_RAMDISK.help = "Will not work anymore: ramdisks are configured in kernels.conf"
a.append(DEPRECATED_RAMDISK)


# Arguments that are deprecated but deemed safe to ignore with a debug message

DEPRECATED_LOGLEVEL = ControlArg("loglevel", None, deprecated=True)
DEPRECATED_LOGLEVEL.help = "Ignored: logging is now only controlled by logging.conf"
a.append(DEPRECATED_LOGLEVEL)

DEPRECATED_LONGHELP = ControlArg("longhelp", None, deprecated=True, noval=True)
DEPRECATED_LONGHELP.help = "Ignored: use --help"
a.append(DEPRECATED_LONGHELP)

DEPRECATED_WORKSPACEIMPL = ControlArg("workspaceimpl", None, deprecated=True)
DEPRECATED_WORKSPACEIMPL.help = "Ignored"
a.append(DEPRECATED_WORKSPACEIMPL)

DEPRECATED_PERSISTENCEDIR = ControlArg("persistencedir", None, deprecated=True)
DEPRECATED_PERSISTENCEDIR.help = "Ignored"
a.append(DEPRECATED_PERSISTENCEDIR)

DEPRECATED_CHKDOWN = ControlArg("checkshutdown", None, deprecated=True)
DEPRECATED_CHKDOWN.help = "Ignored"
a.append(DEPRECATED_CHKDOWN)

DEPRECATED_CHKDOWNP = ControlArg("checkshutdownpause", None, deprecated=True)
DEPRECATED_CHKDOWNP.help = "Ignored"
a.append(DEPRECATED_CHKDOWNP)
