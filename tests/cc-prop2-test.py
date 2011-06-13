#!/usr/bin/env python

import pexpect
import sys
import os
import filecmp
import uuid

tst_image_name = os.environ['NIMBUS_TEST_IMAGE']
tst_image_src = os.environ['NIMBUS_SOURCE_TEST_IMAGE']
to=int(os.environ["NIMBUS_TEST_TIMEOUT"])
cc_home=os.environ['CLOUD_CLIENT_HOME']
logfile = sys.stdout
newname=str(uuid.uuid1()).replace("-", "")
try:
    os.remove(newname)
except:
    pass
cmd = "%s/bin/cloud-client.sh --transfer --sourcefile %s" % (cc_home, tst_image_src)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)

cmd = "%s/bin/cloud-client.sh --run --name %s --hours .25" % (cc_home, tst_image_name)
child = pexpect.spawn (cmd, timeout=to, maxread=20000, logfile=logfile)
rc = child.expect ('Running:')
if rc != 0:
    print "%s not found in the list" % (tst_image_name)
    sys.exit(1)
handle = child.readline().strip().replace("'", "")
rc = child.expect(pexpect.EOF)
if rc != 0:
    print "run"
    sys.exit(1)

cmd = "%s/bin/cloud-client.sh --handle %s --save --newname %s" % (cc_home, handle, newname)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
print x
if rc != 0:
    print "failed to save"
    sys.exit(1)
cmd = "%s/bin/cloud-client.sh --list" % (cc_home)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
cmd = "%s/bin/cloud-client.sh --download --name %s --localfile %s" % (cc_home, newname, newname)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
print x
if rc != 0:
    print "failed to terminate"
    sys.exit(1)

rc = filecmp.cmp(newname, tst_image_src)
os.remove(newname)
if rc:
    sys.exit(0)
else:
    print "files differ"
    sys.exit(1)
sys.exit(0)


