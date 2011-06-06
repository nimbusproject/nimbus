#!/usr/bin/env python

import pexpect
import sys
import os

to=90
cc_home=os.environ['CLOUD_CLIENT_HOME']
logfile = sys.stdout

src_file = os.environ['NIMBUS_TEST_IMAGE']
sfa = src_file.split("/")
image_name = sfa[len(sfa) - 1]
cmd = "%s/bin/cloud-client.sh --transfer --sourcefile %s" % (cc_home, src_file)
(x, rc)=pexpect.run(cmd, withexitstatus=1)

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

cmd = "%s/bin/cloud-client.sh --delete --name %s" % (cc_home, image_name)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1)
print x
if rc != 0:
    print "failed to delete"
    sys.exit(1)
sys.exit(0)
