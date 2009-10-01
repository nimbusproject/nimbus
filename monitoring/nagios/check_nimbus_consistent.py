#!/usr/bin/python
"""*
 * Copyright 2008 University of Victoria
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * """
# check_client_xm
# Version 0.3
# Author: Michael Paterson (mhp@uvic.ca)
# Checks the nimbus client DB against xm list on nodes 
import sys
import commands
import os
import time
import string
from optparse import OptionParser

_version_ = 0.3


# Nagios return codes
OK = 0
WARNING = 1
CRITICAL = 2
UNKNOWN = 3

# Parse command-line options.
parser = OptionParser()
parser.add_option("-w", "--warning", dest="warning_threshold", help="Set WARNING threshold to value.", default=10)
parser.add_option("-c", "--critical", dest="critical_threshold", help="Set WARNING threshold to value.", default=5)
parser.add_option("-v", "--verbose", dest="verbosity", help="Set verbosity level (0-3).", default=0)
parser.add_option("-V", "--version", dest="version", action="store_false", help="Display version infomoration.", default=True)


# Parse the command line arguments and store them in 'options'
(options, args) = parser.parse_args()

verbose = int(options.verbosity)
if not options.version:
    print _version_
    sys.exit(OK)

# Check options for validity
if (int(options.critical_threshold) < 0) or (int(options.warning_threshold) < 0):
    print 'UNKNOWN: Invalid threshold option (negative threshold)'
    sys.exit(UNKNOWN)

if (verbose < 0) or (verbose > 4):
    print 'UNKNOWN: Invalid verbosity option (not in range 0-3)'
    sys.exit(UNKNOWN)

if verbose == 3:
    print "Copying Database"
copy_command = 'cp -Rf '+os.environ['GLOBUS_LOCATION']+'/var/nimbus/WorkspacePersistenceDB /tmp/.'
if verbose == 3:
    print copy_command
status, outputcp = commands.getstatusoutput(copy_command)
if status != 0:
    if verbose == 3:
        print "DEBUG: Copy Error", outputcp
status, outputrm = commands.getstatusoutput('rm /tmp/WorkspacePersistenceDB/*.lck')
if status != 0:
    if verbose == 3:
        print "DEBUG: Error Removing locks", outputrm
if verbose == 3:
    print "Execute Query"
plugin_command = os.environ['_']
plugin_dir_tokens = plugin_command.split('/')
relative_dir = ''
for x in range(len(plugin_dir_tokens) - 1):
    relative_dir += plugin_dir_tokens[x]+'/'
query_command = os.environ['DERBY_HOME']+'/bin/ij < '+relative_dir+'nimbus_vm_consistent.sql 2> /dev/null'
if verbose == 3:
    print query_command
status,outputq = commands.getstatusoutput(query_command)
if status != 0:
    if verbose == 3:
        print "DEBUG: Error Executing Query", outputq
# Parse the results of the query
if verbose == 3:
    print "Parsing Query results"
    print outputq
nodedict = {}
try:
    lines = outputq.splitlines()
    foundDash = False
    for line in lines:
        if not foundDash and line.find('----') > -1:
            foundDash = True
            continue
        elif not foundDash:
            continue
        elif line.find('rows selected') > -1:
            break
        elif len(line) == 0:
            continue
        pair = line.split('|')
        if len(pair) != 5: #Known number from the sql query select
            continue
        for x in range(len(pair)):
            pair[x] = pair[x].strip()
        pair[3] = pair[3][:-3] # Strip off the trailing 3 places in the time from derby DB time format
        now = time.time()
        shutdown = 0
        try:
            shutdown = int(pair[3])
        except:
            print "casting error - shutdown time", pair, foundDash, outputq
        if now > shutdown: # this VM has been shutdown but not destroyed yet don't look for in 'xm list'
            continue
        try:
            state = int(pair[4])
            if state == 6: # this VM was shutdown properly via workspace --shutdown, don't look for in list
                continue
        except:
            print "casting error - state"
        if pair[2] in nodedict.keys():
            nodedict[pair[2]].append(pair[0])
        else:
            nodedict[pair[2]] = []
            nodedict[pair[2]].append(pair[0])
except:
    print "exception parsing:", sys.exc_info()
    raise
    sys.exit(-1)
output = ''
for key in nodedict.keys():
    vms = len(nodedict[key])
    xmlist = commands.getoutput('ssh root@'+key+' xm list') # remote command execution - Nagios must have passwordless ssh to all nodes for this to function properly
    xlines = xmlist.splitlines()
    for xline in xlines:
        sp = xline.split()
        if len(sp) > 0 and sp[0].find('wrksp') > -1:
            wrksp = sp[0].split('-')
            if nodedict[key].index(wrksp[1]) > -1:
                #all good
                vms -= 1
                nodedict[key].remove(wrksp[1])
    if vms != 0:
        values = string.join(nodedict[key], '')
        output += key+' '+values+' ERROR'+' '+str(vms)+'\n'
    else:
        output += key+' '+'OK\n'
if len(nodedict) == 0:
    output += "No VMs running."

return_code = 0
#Build the output string for Nagios and parse the output
print 'STATUS',
if status == 0:
    lines = output.splitlines()
    err_nodes = 'mismatch on: '
    foundErr = False
    for line in lines:
        if line.find('ERROR') > -1:
            foundErr = True
            pair = line.split()
            err_nodes = err_nodes + " " + pair[0] + " ID(s): "
            missVMs = pair[1].strip('[]\'')
            if len(missVMs) != 0:
                 err_nodes = err_nodes + missVMs
    if not foundErr:
        return_code = OK
        print 'OK'
    else:
        return_code = WARNING
        print 'WARNING: ' + err_nodes
else:
    return_code = UNKNOWN
    print 'UNKNOWN: Unexpected Error, Could not execute plugin'
# Verbose output
if verbose:
    print '',
if verbose >=  1:
    print output, nodedict
# Return code for Nagios
sys.exit(return_code)
