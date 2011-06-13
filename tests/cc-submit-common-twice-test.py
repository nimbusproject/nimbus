#!/usr/bin/env python

import pexpect
import sys
import os
import uuid

tst_image_name = os.environ['NIMBUS_TEST_IMAGE']
tst_image_src = os.environ['NIMBUS_SOURCE_TEST_IMAGE']
int(os.environ[NIMBUS_TEST_TIMEOUT])
cc_home=os.environ['CLOUD_CLIENT_HOME']
nh=os.environ['NIMBUS_HOME']
logfile = sys.stdout
common_image = str(uuid.uuid1()).replace("-", "")

cmd = "%s/bin/nimbus-public-image %s %s" % (nh, tst_image_src, common_image)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
if rc != 0:
    print "failed create the public image"
    sys.exit(1)

for i in range(0, 2):
    cmd = "%s/bin/cloud-client.sh --run --name %s --hours .25" % (cc_home, common_image)
    child = pexpect.spawn (cmd, timeout=to, maxread=20000, logfile=logfile)
    rc = child.expect ('Running:')
    if rc != 0:
        print "run failed on try %d" % (i)
        sys.exit(1)
    handle = child.readline().strip().replace("'", "")
    rc = child.expect(pexpect.EOF)
    if rc != 0:
        print "run"
        sys.exit(1)

    cmd = "%s/bin/cloud-client.sh --terminate --handle %s" % (cc_home, handle)
    print cmd
    (x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile, timeout=to)
    print x
    if rc != 0:
        print "failed to terminate %s on try %d" % (handle, i)
        sys.exit(1)

cmd = "%s/bin/nimbus-public-image --delete %s" % (nh, common_image)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
if rc != 0:
    print "failed create the public image"
    sys.exit(1)
sys.exit(0)
