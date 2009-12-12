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
# The following cmdline arguments may be queried via Parameters.get_arg*()
# using the name.
################################################################################

ACTION = ControlArg("action", "-a", since="2.3", createarg=False)
ACTION.help = "Action for the program to take: create, remove, info, reboot, pause, unpause, propagate, unpropagate"
a.append(ACTION)

CONF = ControlArg("conf", "-c", createarg=False, metavar="PATH")
CONF.help = "Absolute path to main.conf.  * Always required *"
a.append(CONF)

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

VCPUS = ControlArg("vcpus", None, since="2.3", metavar="NUM")
VCPUS.help = "Number of vcpus to assign the VM, overrides configuration"
a.append(VCPUS)


################################################################################
# DEPRECATED ARGUMENTS
#
# The following cmdline arguments may NOT be queried via Parameters
# They are ONLY used on the cmdline, mostly arguments held over for backwards
# compatibility.
################################################################################

CREATE = ControlArg("create", None, deprecated=True, noval=True)
CREATE.help = "Works, but use '--action create' from now on"
a.append(CREATE)

REMOVE = ControlArg("remove", None, deprecated=True, noval=True)
REMOVE.help = "Works, but use '--action remove' from now on"
a.append(REMOVE)

INFO = ControlArg("info", None, deprecated=True, noval=True)
INFO.help = "Works, but use '--action info' from now on"
a.append(INFO)

REBOOT = ControlArg("reboot", None, deprecated=True, noval=True)
REBOOT.help = "Works, but use '--action reboot' from now on"
a.append(REBOOT)

PAUSE = ControlArg("pause", None, deprecated=True, noval=True)
PAUSE.help = "Works, but use '--action pause' from now on"
a.append(PAUSE)

UNPAUSE = ControlArg("unpause", None, deprecated=True, noval=True)
UNPAUSE.help = "Works, but use '--action unpause' from now on"
a.append(UNPAUSE)

PROPAGATE = ControlArg("propagate", None, deprecated=True, noval=True)
PROPAGATE.help = "Works, but use '--action propagate' from now on"
a.append(PROPAGATE)

UNPROPAGATE = ControlArg("unpropagate", None, deprecated=True, noval=True)
UNPROPAGATE.help = "Works, but use '--action unpropagate' from now on"
a.append(UNPROPAGATE)

STARTPAUSED = ControlArg("startpaused", None, deprecated=True, noval=True)
STARTPAUSED.help = "Will not work anymore"
a.append(STARTPAUSED)
