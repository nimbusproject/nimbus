#!/usr/bin/env python

import pexpect
import sys
import os
import filecmp

to=180

cc_home=os.environ['CLOUD_CLIENT_HOME']
logfile = sys.stdout
newname="XXXX"
try:
    os.remove(newname)
except:
    pass
cmd = "%s/bin/cloud-client.sh --transfer --sourcefile %s" % (cc_home, os.environ['NIMBUS_TEST_IMAGE'])
(x, rc)=pexpect.run(cmd, withexitstatus=1)

cmd = "%s/bin/cloud-client.sh --run --name group --hours .25" % (cc_home)
child = pexpect.spawn (cmd, timeout=to, maxread=20000, logfile=logfile)
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
(x, rc)=pexpect.run(cmd, withexitstatus=1, timeout=to, logfile=logfile)
print x
if rc != 0:
    print "failed to save"
    sys.exit(1)
sys.exit(0)


