#!/usr/bin/env python

import os
import sys
import time
from workspacecontrol.main import get_all_configs
from workspacecontrol.main import get_class_by_keyword
from workspacecontrol.main.main_tests_util import guess_basedir

def get_logfiledir(p):
    logfiledir = p.get_conf_or_none("logging", "logfiledir")
    if not logfiledir:
        sys.stderr.write("There is no logfiledir configuration")
        return None
        
    if not os.path.isabs(logfiledir):
        # The following is a copy of the logic in Common which is not ideal but
        # we can't instantiate defaults.Common without causing a new log file to
        # be created
        vardir = p.get_conf_or_none("wcdirs", "var")
        if not vardir:
            raise InvalidConfig("There is no wcdirs->var configuration.  This is required.")
            
        if not os.path.isabs(vardir):
            basedir = guess_basedir()
            vardir = os.path.join(basedir, vardir)
            
        logfiledir = os.path.join(vardir, logfiledir)
        
    return logfiledir
    
if len(sys.argv) != 2:
    sys.stderr.write("This program requires 1 argument, the absolute path to the main.conf file")
    sys.exit(1)

confpath=sys.argv[1]

# mini implementation of the dependency injection used in the real program:
allconfs = get_all_configs(confpath)
p_cls = get_class_by_keyword("Parameters", allconfigs=allconfs)
p = p_cls(allconfs, None)

logfiledir = get_logfiledir(p)
if not logfiledir:
    sys.exit(1)

# find the newest file in the directory:

sys.stderr.write("Log file dir:    %s\n" % logfiledir)

sortme = []
for root, dirs, files in os.walk(logfiledir):
    for name in files:
        path = os.path.join(logfiledir, name)
        if os.path.isfile(path):
            astat = os.stat(path)
            modtime = time.localtime(astat[8])
            sortme.append((modtime, path))
    
    break # only look in the top directory
    
if len(sortme) == 0:
    sys.stderr.write("Could not find any files in: %s" % logfiledir)
    sys.exit(1)
    
sortme.sort()
newest_file = sortme[-1][1]
sys.stderr.write("Newest log file: %s\n" % newest_file)

f = open(newest_file)
for line in f:
    print line,
f.close()
