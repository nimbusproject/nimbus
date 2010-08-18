#!/usr/bin/env python

import pexpect
import sys
import os
import filecmp
import uuid

cc_home=os.environ['CLOUD_CLIENT_HOME']
logfile = sys.stdout
newname=str(uuid.uuid1()).replace("-", "")
try:
    os.remove(newname)
except:
    pass
cmd = "%s/bin/cloud-client.sh --transfer --sourcefile /etc/group" % (cc_home)
(x, rc)=pexpect.run(cmd, withexitstatus=1)

cmd = "%s/bin/cloud-client.sh --run --name group --hours .25 --newname %s" % (cc_home, newname)
child = pexpect.spawn (cmd, timeout=30, maxread=20000, logfile=logfile)
rc = child.expect ('Running:')
if rc != 0:
    print "group not found in the list"
    sys.exit(1)
handle = child.readline().strip().replace("'", "")
rc = child.expect(pexpect.EOF)
if rc != 0:
    print "run"
    sys.exit(1)

cmd = "%s/bin/cloud-client.sh --handle %s --save" % (cc_home, handle)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1)
print x
if rc != 0:
    print "failed to save"
    sys.exit(1)
cmd = "%s/bin/cloud-client.sh --list" % (cc_home)
(x, rc)=pexpect.run(cmd, withexitstatus=1)
cmd = "%s/bin/cloud-client.sh --download --name %s --localfile %s" % (cc_home, newname, newname)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1)
print x
if rc != 0:
    print "failed to terminate"
    sys.exit(1)

rc = filecmp.cmp(newname, "/etc/group")
os.remove(newname)
if rc:
    sys.exit(0)
else:
    print "files differ"
    sys.exit(1)
sys.exit(0)


