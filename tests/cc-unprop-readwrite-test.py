#!/usr/bin/env python

import pexpect
import sys
import os
import uuid
import datetime

tst_image_name = os.environ['NIMBUS_TEST_IMAGE']
tst_image_src = os.environ['NIMBUS_SOURCE_TEST_IMAGE']
to=int(os.environ["NIMBUS_TEST_TIMEOUT"])
cc_home=os.environ['CLOUD_CLIENT_HOME']
logfile = sys.stdout
newname=str(uuid.uuid1()).replace("-", "")

src_file = tst_image_src
sfa = src_file.split("/")
image_name = sfa[len(sfa) - 1]

cmd = "%s/bin/cloud-client.sh --transfer --sourcefile %s" % (cc_home, src_file)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)

cmd = "%s/bin/cloud-client.sh --run --name %s --hours .25" % (cc_home, image_name)
child = pexpect.spawn (cmd, timeout=to, maxread=20000, logfile=logfile)
rc = child.expect ('Running:')
if rc != 0:
    print "Running not found in the list"
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

# check the various params
cmd = "%s/bin/cloud-client.sh --list" % (cc_home)
child = pexpect.spawn (cmd, timeout=to, maxread=20000, logfile=logfile)
rc = child.expect (image_name)
if rc != 0:
    print "%s not found in the list" % (image_name)
    sys.exit(1)

# the rest of this line should have Read/write.
line = child.readline().replace("'", "").strip()
if line.find('Read/write') < 0:
    print "%s is not listed as Read/write" % (image_name)
    sys.exit(1)

rc = child.expect(pexpect.EOF)
if rc != 0:
    print "run"
    sys.exit(1)

cmd = "%s/bin/cloud-client.sh --delete --name %s" % (cc_home, newname)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
cmd = "%s/bin/cloud-client.sh --delete --name %s" % (cc_home, image_name)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)


sys.exit(0)


