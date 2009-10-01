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
# check_nimbus_ws_slots
# Version 0.3
# Author: Michael Paterson (mhp@uvic.ca)
# Checks the remaining slots of a network pool 

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
parser.add_option("-w", "--warning", dest="warning_threshold", help="Set WARNING threshold to value.", default=60)
parser.add_option("-c", "--critical", dest="critical_threshold", help="Set WARNING threshold to value.", default=95)
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

# Copy the Database and remove the locks
copy_command = 'cp -Rf '+os.environ['GLOBUS_LOCATION']+'/var/nimbus/WorkspacePersistenceDB /tmp/.'
status,output = commands.getstatusoutput(copy_command)
if status != 0:
    if verbose == 3:
        print "DEBUG: Copy Error"
status, output = commands.getstatusoutput("rm /tmp/WorkspacePersistenceDB/*.lck")
if status != 0:
    if verbose == 3:
        print "DEBUG: Error Removing locks"
if verbose == 3:
    print "Execute Query for Free slots"
plugin_command = os.environ['_']
plugin_dir_tokens = plugin_command.split('/')
relative_dir = ''
for x in range(len(plugin_dir_tokens) - 1):
    relative_dir += plugin_dir_tokens[x]+'/'
query_command = os.environ['DERBY_HOME']+'/bin/ij < '+relative_dir+'nimbus_vm_freeslot.sql 2> /dev/null'
if verbose == 3:
    print query_command
status, outputFree = commands.getstatusoutput(query_command)
if status != 0:
    if verbose == 3:
        print "DEBUG: Error Executing Query"
if verbose == 3:
    print "Execute Query for Total slots"
query_command = os.environ['DERBY_HOME']+'/bin/ij < '+relative_dir+'/nimbus_vm_totalslot.sql 2> /dev/null'
if verbose == 3:
    print query_command
status, outputTotal = commands.getstatusoutput(query_command)
if status != 0:
    if verbose == 3:
        print "DEBUG: Error Executing Query"

"""
This parser takes 2 inputs, the results of sql queries to count the used ips in a network pool, and the other for the total ips in the pool
the files contain data that looks like this
FREE      
-----------
6 
and
TOTAL      
-----------
6 

For both we look for the ----- line and then save the line after containing the count. once both files have been read the values are output to stdout seperated by a space ie. "6 6" to be read by the Nagios plugin.
"""

exit_status = 0
free = -1
total = -1
lines = outputFree.splitlines()
foundDash = False
for line in lines:
    if not foundDash and line.find('---') > -1:
        foundDash = True
        continue
    elif not foundDash:
        continue
    freeline = line.strip()
    try:
        free = int(freeline)
        break
    except:
        print line #why are we even here?
        continue # bad line try next
lines = outputTotal.splitlines()
foundDash = False
for line in lines:
    if not foundDash and line.find('---') > -1:
        foundDash = True
        continue
    elif not foundDash:
        continue
    totline = line.strip()
    try:
        total = int(totline)
    except:
        continue

return_code = OK
#Build the output string for Nagios
print 'WSSLOT_STATUS:',

if status == 0: #Successful last call, print output
    percentfree = 0.0
    if total >=  0:
        percentfree = ((total-free)/float(total))*100.0
    else:
        return_code = UNKNOWN
        print "UNKNOWN Error: zero total slots found"
        sys.exit(return_code)
    if percentfree < options.warning_threshold:
        return_code = OK
        print "OK", free,"slots remaining"
    elif percentfree < options.critical_threshold:
        return_code = WARNING
        print "WARNING",free,"slots remaining"
    elif percentfree >= options.critical_threshold:
        return_code = CRITICAL
        print 'CRITICAL',free,'slots remaining'
else:
    return_code = UNKNOWN
    print 'UNKNOWN: Unexpected Error, Could not execute plugin'

# Verbose output
if verbose:
    print '',
if verbose > 1:
    print outputFree, outputTotal
    print status
# Return code for Nagios
sys.exit(return_code)
