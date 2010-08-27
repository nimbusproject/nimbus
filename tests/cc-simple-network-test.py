#!/usr/bin/env python

import pexpect
import sys
import os

cc_home=os.environ['CLOUD_CLIENT_HOME']
logfile = sys.stdout

cmd = "%s/bin/cloud-client.sh --transfer --sourcefile /etc/group" % (cc_home)
(x, rc)=pexpect.run(cmd, withexitstatus=1)

cmd = "%s/bin/cloud-client.sh --networks" % (cc_home)
child = pexpect.spawn (cmd, timeout=30, maxread=20000, logfile=logfile)
rc = child.expect ('Network')
if rc != 0:
    print "group not found in the list"
    sys.exit(1)
line = child.readline()
ndx = line.find("public")
if ndx < 0:
    print "no public network found"
    sys.exit(1)
ndx = line.find("private")
if ndx < 0:
    print "no private network found"
    sys.exit(1)

sys.exit(0)

