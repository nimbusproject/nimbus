# ------------------------- CONFIGURATIONS ----------------------- #

# PARTITION_FILE file contains a list of physical partitions to give out.  
# One per line.  Use full path.  For example:
#
# /dev/sdx1
# /dev/sdx2
# /dev/sdx3
# /dev/sdx4
#
# *WARNING* Configure the list of physical partitions carefully.  They are
# formatted every time they are used!
#
# You need to create and protect this file yourself.

PARTITION_FILE="/nimbus.tmplease.partitions"


# This will be called, plus the physical partition, e.g. "/dev/sdx1"
# We distribute this with "/bin/true" so you can safely test what "would"
# happen once you get set up.  With "/bin/true" first, nothing can ever
# be formatted.  Remove it to "go live."

FORMAT_COMMAND="/bin/true /sbin/mkfs.ext3 -j -m0"


# These two files are created programatically if they does not exist.  They are
# used to safely keep track of the leases.  If the host machine reboots, they
# will usually be deleted by the init sequence if they are in the /var/run
# or /var/lock directories and that is usually OK since all of the VMs using
# the physical partitions will be gone as well.

STATE_FILE="/var/run/nimbus.tmplease.state"
LOCK_FILE="/var/lock/nimbus.tmplease.lock"



# ---------------------------- IMPORTS ------------------------- #

import commands
import copy
import fcntl
import os
import pickle
import sys


# -------------------------- FORMATTING ------------------------ #

def format(partition):
    cmd = "%s %s" % (FORMAT_COMMAND, partition)
    print >>sys.stderr, "Running command: %s" % cmd
    (status, output) = commands.getstatusoutput(cmd)
    if os.WIFSIGNALED(status):
        sig = str(os.WTERMSIG(status))
        msg = "format of partition %s exited by signal %s" % (partition, sig)
        raise Exception(msg)
    elif os.WEXITSTATUS(status):
        msg = "format of partition %s exited with exit code %d" % (partition, os.WEXITSTATUS(status))
        raise Exception(msg)
    print >>sys.stderr, "Successfully ran command: %s" % cmd

# ---------------------------- STATE --------------------------- #

STATE_AVAILABLE = '0'
STATE_MOUNTED = '1'
STATE_FORMATTING = '2'
STATE_NOTANALYZED = '3'

STATE_MAP = { STATE_AVAILABLE:"available",
              STATE_MOUNTED:"mounted",
              STATE_FORMATTING:"formatting",
              STATE_NOTANALYZED:"notanalyzed", }
    

class Partition:
    def __init__(self, partition_path):
        self.path = partition_path
        self.state = STATE_NOTANALYZED
        self.assoc_vm = None

def persist_partitions(state, statefilepath):
    f = None
    try:
        f = open(statefilepath, 'w')
        pickle.dump(state, f)
    finally:
        if f:
            f.close()

def read_partitions(statefilepath):
    
    if not os.path.exists(statefilepath):
        return dict()
        
    f = None
    try:
        f = open(statefilepath, 'r')
        state = pickle.load(f)
        return state
    finally:
        if f:
            f.close()

def analyze_state(state, partitionpath):
    
    if not isinstance(state, dict):
        raise Exception("expecting a dict object .. not taking any chances, this program is dangerous")
    
    f = None
    try:
        f = open("/etc/mtab", "r")
        mtab = f.readlines()
    finally:
        if f:
            f.close()
            
    mounted = []
    for mline in mtab:
        m = mline.strip()
        parts = m.split()
        mounted.append(parts[0])
    
    f = None
    try:
        f = open(partitionpath, "r")
        content = f.readlines()
    finally:
        if f:
            f.close()

    previous_partitions = copy.deepcopy(state.keys())
    seen_partitions = []

    for line in content:
        p = line.strip()
        if not p:
            continue
        if p[0] == "#":
            continue
        if not p.startswith("/dev/"):
            raise Exception("Found a line in partition configuration that was not a comment and did not start with '/dev/': '%s'" % p)
        if p in mounted:
            raise Exception("Found a line in partition configuration that is mounted locally! (listed in /etc/mtab): '%s'" % p)
        
        if not state.has_key(p):
            newp = Partition(p)
            newp.state = STATE_AVAILABLE
            state[p] = newp
            print >>sys.stderr, "Now tracking tmplease partition %s" % p
        
        seen_partitions.append(p)
            
    for p in previous_partitions:
        if p not in seen_partitions:
            del state[p]
            print >>sys.stderr, "No longer tracking tmplease partition %s" % p


# --------------------------- LOCKING -------------------------- #

def lock(lockfilepath):

    if not os.path.exists(lockfilepath):
        f = open(lockfilepath, "w")
        f.write("\n")
        f.close()

    lockfile = open(lockfilepath, "r+")
    fcntl.flock(lockfile.fileno(), fcntl.LOCK_EX)
    return lockfile

def unlock(lockfile):
    if not lockfile:
        return
    lockfile.close()


# ----------------------------- RUN ----------------------------- #

def getparams(args):
        
    lockfilepath = LOCK_FILE
        
    statefilepath = STATE_FILE
    
    partitionpath = PARTITION_FILE
    if not os.path.exists(partitionpath):
        raise Exception("Partition configuration list does not exist: %s" % partitionpath)
        
    
    addrem = args[1]
    
    if addrem == "--print":
        return (addrem, lockfilepath, statefilepath, partitionpath, None)
    
    if addrem != "--add" and addrem != "--remove":
        raise Exception("Syntax: --add|--remove <lockfile> <statefile> <partitionfile> <vmname>")
    
    vmname = args[2]
    if " " in vmname:
        raise Exception("<vmname> should not contain spaces")
        
    return (addrem, lockfilepath, statefilepath, partitionpath, vmname)

def main():
    
    (addrem, lockfilepath, statefilepath, partitionpath, vmname) = getparams(sys.argv)
    
    if addrem == "--print":
        state = read_partitions(statefilepath)
        analyze_state(state, partitionpath)
        for p in state.keys():
            print "-------------"
            print "partition: %s" % state[p].path
            print "   vm name: %s" % state[p].assoc_vm
            print "   state: %s" % STATE_MAP[state[p].state]
        return 0
    
    partition_to_use = None
    lockfile = lock(lockfilepath)
    try:
        state = read_partitions(statefilepath)
        analyze_state(state, partitionpath)
        if addrem == "--add":
            
            count = 0
            for p in state.keys():
                if state[p].assoc_vm == vmname:
                    count += 1
            
            if count > 0:
                print >>sys.stderr, "Partition state is corrupted, a VM with this name already has a partition reserved"
                return 6
            
            for p in state.keys():
                if state[p].state == STATE_AVAILABLE:
                    state[p].assoc_vm = vmname
                    state[p].state = STATE_FORMATTING
                    partition_to_use = p
            
                    # only stdout this program should create
                    print partition_to_use
                    break
            if not partition_to_use:
                print >>sys.stderr, "No physical partitions are available for temp space partition, administrator should be notified"
                return 7

        elif addrem == "--remove":
            count = 0
            for p in state.keys():
                if state[p].assoc_vm == vmname:
                    count += 1
            
            if count > 1:
                print >>sys.stderr, "Partition state is corrupted, multiple VMs with the same name have reserved a partition"
                return 6
                
            if count == 0:
                print >>sys.stderr, "warning: partition state had no record of this VM reserving a physical partition"
            else:
                for p in state.keys():
                    if state[p].assoc_vm == vmname:
                        state[p].state = STATE_AVAILABLE
                        state[p].assoc_vm = None
            
        persist_partitions(state, statefilepath)

    finally:
        unlock(lockfile)
        lockfile = None
        
    if addrem == "--remove":
        return 0
        
    # If --add, it's a multi step process and the lock should be given up
    # in between steps.
        
    if addrem != "--add":
        # safety check for future programmers, we're about to do some damage
        print >>sys.stderr, "Not --add?"
        return 42
    
    toformat = state[partition_to_use]
    
    # again, check assumptions
    if toformat.state != STATE_FORMATTING:
        print >>sys.stderr, "Not state_formatting?"
        return 43
        
    if toformat.path != partition_to_use:
        print >>sys.stderr, "Path not same as partition object?"
        return 44
    
    format(partition_to_use)
    
    lockfile = lock(lockfilepath)
    try:
        # this information could have changed in the meantime by other 
        # processes, needs to be read back in.
        state = read_partitions(statefilepath)
        analyze_state(state, partitionpath)
        
        if not state.has_key(partition_to_use):
            print >>sys.stderr, "Partition became untracked in the meantime during a format? %s" % partition_to_use
            return 1
            
        state[partition_to_use].state = STATE_MOUNTED
        
        persist_partitions(state, statefilepath)
        
    finally:
        unlock(lockfile)
        lockfile = None
    
if __name__ == "__main__":
    if os.name != 'posix':
        sys.exit("only runs on posix systems") # because of locking
    sys.exit(main())
