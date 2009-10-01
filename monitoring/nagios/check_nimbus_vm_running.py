#!/usr/bin/python
""" *
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
# nimbus_check_ws_running
# Version 0.3
# Author: Michael Paterson (mhp@uvic.ca)
# Counts the number of running workspaces 

import sys
import commands
import os
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
status, output = commands.getstatusoutput(copy_command)
if status != 0:
    if verbose == 3:
        print "DEBUG: Copy Error"
status, output = commands.getstatusoutput("rm /tmp/WorkspacePersistenceDB/*.lck")
if status != 0:
    if verbose == 3:
        print "DEBUG: Error Removing locks"
if verbose == 3:
    print "Execute Query"
plugin_command = os.environ['_']
plugin_dir_tokens = plugin_command.split('/')
relative_dir = ''
for x in range(len(plugin_dir_tokens) - 1):
    relative_dir += plugin_dir_tokens[x]+'/'
query_command = os.environ['DERBY_HOME']+'/bin/ij < '+relative_dir+'nimbus_vm_running.sql 2> /dev/null'
if verbose == 3:
    print query_command
status, output = commands.getstatusoutput(query_command)
if status != 0:
    if verbose == 3:
        print "DEBUG: Error Executing Query"

"""
Example output of what the input file looks like
ij version 10.4
ij> > > > > > > > > > > > > > > > > > > > > ij> > > RUNNING    
-----------
2          

1 row selected
ij> ij> 

We look for the ---- line knowing the next line is the number of interest
then print just that number out to be read by the Nagios plug in
"""
running = 0
exit_status = 0
try:
    lines = output.splitlines()
    foundDash = False
    for line in lines:
        if not foundDash and line.find('----') > -1 or len(line) == 0:
            foundDash = True
            continue
        elif not foundDash:
            continue
        try:
            running = int(line)
            break
        except:
            continue # could not cast, try again on next line
except:
    print "exception opening file bad path or file missing"
    sys.exit(-1)

return_code = 0
#Build the output string for Nagios
print 'WSRUN_STATUS:',
if status == 0:
    if running < options.warning_threshold:
        return_code = OK
        print running,'workspaces running'
    elif running < options.critical_threshold:
        return_code = WARNING
        print running,'workspaces running'
    elif running >= options.critical_threshold:
        return_code = CRITICAL
        print running,'workspaces running'
    else:
        return_code = UNKNOWN
        print 'UNKNOWN: error running plugin'
else:
    return_code = UNKNOWN
    print 'UNKNOWN: Unexpected Error, Could not execute plugin'

# Verbose output
if verbose:
    print '',
if verbose > 1:
    print output
    print status
# Return code for Nagios
sys.exit(return_code)
