#!/usr/bin/env python

import pexpect
import sys
import os

to=90
cc_home=os.environ['CLOUD_CLIENT_HOME']
logfile = sys.stdout

(x, rc)=pexpect.run("%s/bin/cloud-client.sh --delete --name group" % (cc_home), withexitstatus=1)
print x

cmd = "%s/bin/cloud-client.sh --transfer --sourcefile /etc/group" % (cc_home)
(x, rc)=pexpect.run(cmd, withexitstatus=1)
print x
if rc != 0:
    print "failed to transfer"
    sys.exit(1)

cmd = "%s/bin/cloud-client.sh --list" % (cc_home)
child = pexpect.spawn (cmd, timeout=to, maxread=20000, logfile=logfile)
rc = child.expect ('group')
if rc != 0:
    print "group not found in the list"
    sys.exit(1)
print child.before
rc = child.expect(pexpect.EOF)
print child.before
if rc != 0:
    print "failed to list"
    sys.exit(1)

(x, rc)=pexpect.run("%s/bin/cloud-client.sh --delete --name group" % (cc_home) , withexitstatus=1)
print x
if rc != 0:
    print "failed to delete"
    sys.exit(1)
sys.exit(0)
