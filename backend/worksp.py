#!/usr/bin/env python

# Copyright 1999-2006 University of Chicago
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy
# of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

# For more information see: http://workspace.globus.org

import os,re,getopt,sys,logging,ConfigParser,commands,string,time

# Bootstrapping: allow default conf file to be configured here,
# use absolute path. Otherwise, the --conf parameter needs to
# be given for every invocation

# && DEFAULTCONF WILL BE REPLACED BY INSTALLER &&
DEFAULTCONF="/opt/workspace/worksp.conf"


import workspace
from workspace.err import *

log = logging.getLogger("workspace")
log.setLevel(logging.DEBUG)


class workspaceImpl:
    """implementation parent class"""
    def __init__(self, conffile, action):
        pass
        
    # Actions:
    def propagateImage(self, dryrun):
        """Propagates (with a pull) image from a remote node (can also
           happen as a result of calling --create (add)"""
        pass
           
    def unpropagateImage(self, dryrun):
        """reverse of propagate, image is the target (path's tail
           must match a target)
        """
        pass
        
    def addWorkspace(self, dryrun):
        """Creates or leases workspace"""
        pass
    
    def removeWorkspace(self, dryrun):
        """Shuts down workspace"""
        pass
        
    def rebootWorkspace(self, dryrun):
        """Reboots workspace"""
        pass
        
    def pauseWorkspace(self, dryrun):
        """Pauses workspace"""
        pass
        
    def unpauseWorkspace(self, dryrun):
        """Unpauses workspace"""
        pass
        
    def workspaceInfo(self):
        """Returns string of status"""
        pass
        
    # Other:
    def parameters(self):
        """Hooks for cmdline parameters"""
        pass
        
    def validate(self):
        """Validate is called before any actions are taken.
        If an exception is thrown, action not taken."""
        pass
        
###########################
# manager implementations #
###########################

class xen_v2(workspaceImpl):
    """Xen 2 specific commands"""
    
    def __init__(self, conffile, action):
        workspaceImpl.__init__(self, conffile, action)
        from workspace.vms.xen import xen_v2
        self.params = xen_v2.parameters(conffile, action)
        self.run = xen_v2.instance(self.params)

    def propagateImage(self, dryrun):
        #workspaceImpl.propagateImage(self, dryrun)
        return self.run.propagateImage(dryrun)
        
    def unpropagateImage(self, dryrun):
        #workspaceImpl.unpropagateImage(self, dryrun)
        return self.run.unpropagateImage(dryrun)

    def addWorkspace(self, dryrun):
        #workspaceImpl.addWorkspace(self, dryrun)
        return self.run.addWorkspace(dryrun) 
            
    def removeWorkspace(self, dryrun):
        #workspaceImpl.removeWorkspace(self, dryrun)
        return self.run.removeWorkspace(dryrun)
        
    def rebootWorkspace(self, dryrun):
        #workspaceImpl.rebootWorkspace(self, dryrun)
        return self.run.rebootWorkspace(dryrun)
        
    def pauseWorkspace(self, dryrun):
        #workspaceImpl.pauseWorkspace(self, dryrun)
        return self.run.pauseWorkspace(dryrun)
        
    def unpauseWorkspace(self, dryrun):
        #workspaceImpl.unpauseWorkspace(self, dryrun)
        return self.run.unpauseWorkspace(dryrun)
            
    def workspaceInfo(self):
        #workspaceImpl.workspaceInfo(self)
        return self.run.workspaceInfo()
        
    def parameters(self):
        """returns parameters object"""
        workspaceImpl.parameters(self)
        return self.params
        
    def validate(self):
        workspaceImpl.validate(self)
        self.params.validate()
        
class xen_v3(xen_v2):
    """Xen 3 commands"""

    def __init__(self, conffile, action):
        workspaceImpl.__init__(self, conffile, action)
        from workspace.vms.xen import xen_v3
        self.params = xen_v3.parameters(conffile, action)
        self.run = xen_v3.instance(self.params)

##########
# main() #
##########

def uuidgen():
    return commands.getoutput('uuidgen')

def help():
    print "Usage: %s [command]" % sys.argv[0]
    print """This program is called programmatically
no help yet (waiting for move to optparse)
some networking syntax notes in extended help (--longhelp)"""
    
def extendedhelp():
    print """An extended help message

Apologies, there is a lot of work to on these help messages.
This program is called programmatically.

one mnt task:
--mnttasks 2b9ace3b-a83a-4fdc-be7c-1913cac3c0e8;/root/.ssh/authorized_keys

two mnt tasks:
--mnttasks 2b9ace3b-a83a-4fdc-be7c-1913cac3c0e8;/root/.ssh/authorized_keys;;\
0927bc98-b08c-4a2a-86f0-eb99e8ad8428;/etc/some.conf
    
one networking parameter
 fields separated by ;
multiple NICs separated by ;;

 1st field, nic name, currently unused

 2nd field, association string, see docs, this is deployment and/or
            mode of configuration specific.  e.g. "public"
 
 3rd field, MAC address, or 'ANY' -- in future iterations, we may not
            ever want Xen to decide, so we can ensure consistency
            inside and out of the VM wrt to the settings.  For example,
            if we pass in settings to inside the VM, it should know
            which NIC to apply them to, MAC is the only sane way of
            doing this.
    
 4th field, type of NIC (only bridged supported right now) (case insensitive)
    BRIDGED|NAT|VNET
    
 5th field, mode of configuration (case insensitive)
            (only static and independent supported right now)
    
            STATIC|INDEPENDENT
    
 remaining fields:
 
        ip;gateway;broadcast;subnetmask;dns;hostname;certname;keyname;certpath;keypath
        
        form '10.10.0.0/16' not accepted yet, but if broadcast or
        subnetmask is 'null' then they are derived from whether the
        ip address is a class A, B, or C address
        
        gateway, dns, hostname can be 'null'
        
        cert and key paths can be 'null'
        
 e.g., 'eth0;public;ANY;BRIDGED;STATIC;192.168.2.3;192.168.2.1;null;null;192.168.2.1;abc.com;null;null;null;null'
        
    """
        
    # logging level can be:
    # DEBUG
    # INFO
    # WARNING
    # ERROR
    # CRITICAL
    
    # NONE -> NOTSET

    # mention that you must create/remove with the same config file
    # so if passing in an option, remove must pass the same option
    # otherwise, unpickling may not work correctly (not such an issue
    # yet, a bigger issue if registering teardown tasks...)
    
def main(argv=None):
    if argv is None:
        argv = sys.argv
        
    # for logging whole invocation to DEBUG later
    invocationline = ' '.join(argv)
    
    ## Defaults ##
    action = None
    workspaceimpl = None
    manager = None
    dryrun = False
    loglevel = "INFO"
    conffile = None
    
    # defaults for parameters() interface
    image = None
    imagemount = None
    persistencedir = None
    name = None
    kernel = None
    kernelargs = None
    ramdisk = None
    memory = None
    networking = None
    checkshutdown = None
    checkshutdownpause = None
    notify = None
    startpaused = False
    deleteall = False
    mnttasks = None
    unproptargets = None
    
    # TODO: move to optparse module instead of getopt 
    # (see install.py, workspacepilot.py, etc.)
    
    try:
        ## Collect ##
        if len(argv) < 2:
            raise UsageError("arguments?")
        try:
            opts, args = getopt.getopt(argv[1:], "c:dhl:m:i:p:n:k:w:r:", \
            ["help", "longhelp", "create", "remove", "info", "dryrun", \
            "loglevel=", "workspaceimpl=", "conf=", "images=", "imagemounts=",\
            "persistencedir=", "name=", "kernel=", "kernelargs=", "mnttasks=",\
            "memory=", "checkshutdown=", "checkshutdownpause=", \
            "networking=", "ramdisk=", "startpaused",  "reboot", "pause", \
            "unpause", "propagate", "unpropagate", "deleteall", "notify=", \
            "unproptargets="])
        except getopt.error, msg:
             raise UsageError(msg)
    
        ## Assign ##
        for o,a in opts:
            if o in ("--create","--remove", "--info", "--reboot", "--pause", "--unpause", "--propagate", "--unpropagate"):
                if action: 
                    raise UsageError("only one action can be chosen, '%s' \
indicated but '%s' was already indicated" % 
                    (o[2:], action)) # they actually get parsed in reverse...
                action = o[2:]

            # letter is taken already:
            elif o == "--imagemounts":
                imagemount = a
            elif o == "--checkshutdown":
                checkshutdown = a
            elif o == "--checkshutdownpause":
                checkshutdownpause = a
            elif o == "--kernelargs":
                kernelargs = [a]
                # right now, kernelargs is the only parameter that takes
                # multiple values (the networking parameter is parsed into
                # multiple values, but the parameter itself is not delimetd
                # by spaces, it's by ;)
                # So, we assume that any leftover arguments should get
                # tacked on to kernelargs (unfortunate this is not so
                # reliable, but it is a Python problem)
                if args:
                    kernelargs.extend(args)
                
            elif o == "--networking":
                networking = a
            elif o == "--longhelp":
                extendedhelp()
                return 0
            elif o == "--notify":
                notify = a
            elif o == "--startpaused":
                startpaused = True
            elif o == "--deleteall":
                deleteall = True
            elif o == "--mnttasks":
                mnttasks = a
            elif o == "--unproptargets":
                unproptargets = a
                
            # regular
            elif o in ("-h", "--help"):
                help()
                return 0
            elif o in ("-l", "--loglevel"):
                loglevel = a
            elif o in ("-d", "--dryrun"):
                dryrun = True
            elif o in ("-m", "--memory"):
                memory = a
            elif o in ("-c", "--conf"):
                conffile = a
            elif o in ("-i", "--images"):
                image = a
            elif o in ("-p", "--persistencedir"):
                persistencedir = a        
            elif o in ("-n", "--name"):
                name = a
            elif o in ("-k", "--kernel"):
                kernel = a
            elif o in ("-r", "--ramdisk"):
                ramdisk = a  
            elif o in ("-w", "--workspaceimpl"):
                workspaceimpl = a

        if action == "create":
            action = workspace.CREATE
        if action == "remove":
            action = workspace.REMOVE
        if action == "reboot":
            action = workspace.REBOOT
        if action == "pause":
            action = workspace.PAUSE
        if action == "unpause":
            action = workspace.UNPAUSE
        if action == "info":
            action = workspace.INFO
        if action == "propagate":
            action = workspace.PROPAGATE
        if action == "unpropagate":
            action = workspace.UNPROPAGATE
            
            
        # set up the console logger, file logger set up later
        ch = logging.StreamHandler()
        if loglevel == "DEBUG":
            ch.setLevel(logging.DEBUG)
        elif loglevel == "INFO":
            ch.setLevel(logging.INFO)
        elif loglevel == "WARNING":
            ch.setLevel(logging.WARNING)
        elif loglevel == "ERROR":
            ch.setLevel(logging.ERROR)
        elif loglevel == "CRITICAL":
            ch.setLevel(logging.CRITICAL)
        elif loglevel == "NONE":
            ch.setLevel(logging.NOTSET)
        else:
            ch.setLevel(logging.INFO)
            
        formatter = logging.Formatter("%(asctime)s - %(levelname)s - %(name)s \
(%(lineno)d) - %(message)s")
        ch.setFormatter(formatter)
        log.addHandler(ch)
            
        ## Validate ##
            
        if action in (workspace.CREATE, workspace.REMOVE, workspace.REBOOT, workspace.PAUSE, workspace.UNPAUSE, workspace.INFO, workspace.PROPAGATE, workspace.UNPROPAGATE):
            if not conffile:
                conffile = DEFAULTCONF
        else:
            raise UsageError("no action supplied")
            
        if not os.path.exists(conffile):
            raise EnvironmentProblem("'%s' does not exist" % conffile)
        
        # anything here should only read from the 'general' section
        # of the config file, because everything else is workspace
        # implementation specific
        config = None
        config = ConfigParser.ConfigParser()
        config.read(conffile)
        
            
        # if manager is not specified, allow it to be in conf file
        if not workspaceimpl:
            try:
                workspaceimpl = config.get("general","workspaceimpl")
            except:
                raise UsageError("no manager implementation for '%s'" % workspaceimpl)
                
        # check for valid manager class, under new exe scheme using import
        # __main__ and getattr doesn't work, just hardcode choices for now
        if workspaceimpl == 'xen_v2':
            manager = xen_v2(conffile, action)
            log.debug("'%s' manager instantiated" % workspaceimpl)
        elif workspaceimpl == 'xen_v3':
            manager = xen_v3(conffile, action)
            log.debug("'%s' manager instantiated" % workspaceimpl)
        else:
            raise UsageError("no implementation for '%s'" % workspaceimpl)
        
        # register parameters with manager's parameter object
        
        if action in (workspace.CREATE, workspace.REMOVE, workspace.REBOOT, workspace.PAUSE, workspace.UNPAUSE, workspace.PROPAGATE, workspace.UNPROPAGATE, workspace.CREATE):
            if name:
                # removed call to prefix names with letter
                if name[0] not in string.ascii_letters:
                    raise UsageError("name must begin with letter (xen issue)")
                else:
                    manager.parameters().set_name(name)
            else:
                raise UsageError("no name")

        if action in (workspace.REMOVE, workspace.REBOOT, workspace.PAUSE, workspace.UNPAUSE, workspace.PROPAGATE):
            if not name:
                raise UsageError("no workspace name to run %s" % action)
                
        logfiledir = None
        try:
            logfiledir = config.get("general", "logfiledir")
        except:
            pass
            
        if not logfiledir:
            log.debug("no logfiledir configuration, file logging is disabled")
        
        logfilehandler = None
        logfilepath = None
        if logfiledir:
            if logfiledir[0] != '/':
                raise UsageError("logfiledir configuration is not an absolute path")
            # base filename on time, action and name
            logfilepath = logfiledir + "/" + time.strftime("%b-%d-%Y-%H:%M:%S")
            logfilepath += "-" + str(name) + "-" + action
            
            f = None
            try:
                f = file(logfilepath, 'a')
                f.write("\n## auto-generated @ %s\n\n" % time.ctime())
            finally:
                if f:
                    f.close()
                    
            logfilehandler = logging.FileHandler(logfilepath)
            logfilehandler.setLevel(logging.DEBUG)
            formatter = logging.Formatter("%(asctime)s - %(levelname)s - "
                                         "%(name)s (%(lineno)d) - %(message)s")
            logfilehandler.setFormatter(formatter)
            log.addHandler(logfilehandler)
            log.debug("file logging enabled, path = '%s'" % logfilepath)
            
        log.debug("original invocation: %s" % invocationline)

        # util.control.parameters class is extended by a class in each
        # implementation, a hierarchy can be created later
        
        manager.parameters().set_images(image)
        manager.parameters().set_imagemounts(imagemount)
        manager.parameters().set_kernel(kernel)
        manager.parameters().set_kernelarguments(kernelargs)
        manager.parameters().set_ramdisk(ramdisk)
        manager.parameters().set_persistencedir(persistencedir)
        manager.parameters().set_memory(memory)
        manager.parameters().set_networking(networking)
        manager.parameters().set_checkshutdown(checkshutdown)
        manager.parameters().set_checkshutdownpause(checkshutdownpause)
        manager.parameters().set_notify(notify)
        manager.parameters().set_logfile(logfilepath, logfilehandler)
        manager.parameters().set_startpaused(startpaused)
        manager.parameters().set_deleteall(deleteall)
        manager.parameters().set_mnttasks(mnttasks)
        manager.parameters().set_unproptargets(unproptargets)
        
        # let the implementation decide to proceed or not
        manager.parameters().validate()
        
    except UsageError,err:
        print >>sys.stderr, err.msg
        #print >>sys.stderr, "for help use --help"
        return 2            
            
    except WorkspaceError,err:
        log.critical("Problem validating configuration:\n%s" % err.msg)
        print >>sys.stderr, "for help use --help"
        return 2
        
    except Exception, err:
        log.exception("Problem validating configuration:")
        return 2
        
    ## Run ##
    try:
        log.debug("Starting to execute")
        
        # TODO: use a dictionary of functions
        if action == workspace.PROPAGATE:
            return manager.propagateImage(dryrun)
            
        if action == workspace.UNPROPAGATE:
            return manager.unpropagateImage(dryrun)

        if action == workspace.CREATE:
            return manager.addWorkspace(dryrun)
        
        if action == workspace.REMOVE:
            return manager.removeWorkspace(dryrun)
            
        if action == workspace.REBOOT:
            return manager.rebootWorkspace(dryrun)
            
        if action == workspace.PAUSE:
            return manager.pauseWorkspace(dryrun)
            
        if action == workspace.UNPAUSE:
            return manager.unpauseWorkspace(dryrun)
            
        if action == workspace.INFO:
            status = manager.workspaceInfo()
            if not status:
                print "No workspace status"
                return 1
            else:
                print status
                return 0
            
    except SystemExit, e:
        log.error("SystemExit? exc_value : %s" % exc_value)
        return 3
        
    except Exception, e:
        log.exception("Problem running:")
        return 3
        
if __name__ == '__main__':
    sys.exit(main())


