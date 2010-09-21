#!/usr/bin/env python

import pexpect
import sys
import os
import uuid

cc_home=os.environ['CLOUD_CLIENT_HOME']
nh=os.environ['NIMBUS_HOME']
logfile = sys.stdout
common_image = str(uuid.uuid1()).replace("-", "")

cmd = "%s/bin/nimbus-public-image /etc/group %s" % (nh, common_image)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
if rc != 0:
    print "failed create the public image"
    sys.exit(1)

cmd = "%s/bin/cloud-client.sh --list" % (cc_home)
child = pexpect.spawn (cmd, timeout=30, maxread=20000, logfile=logfile)
rc = child.expect (common_image)
if rc != 0:
    print "common image not found"
    sys.exit(1)
rc = child.expect(pexpect.EOF)
if rc != 0:
    print "failed to list"
    sys.exit(1)

cmd = "%s/bin/nimbus-public-image --delete %s" % (nh, common_image)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
if rc != 0:
    print "failed create the public image"
    sys.exit(1)
sys.exit(0)
