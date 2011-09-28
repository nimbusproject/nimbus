#!/usr/bin/env python

import pexpect
import sys
import os
import uuid

tst_image_name = os.environ['NIMBUS_TEST_IMAGE']
tst_image_src = os.environ['NIMBUS_SOURCE_TEST_IMAGE']

to=int(os.environ["NIMBUS_TEST_TIMEOUT"])
cc_home=os.environ['CLOUD_CLIENT_HOME']
logfile = sys.stdout

(x, rc)=pexpect.run("%s/bin/cloud-client.sh --delete --name %s" % (cc_home, tst_image_name), withexitstatus=1)
print x

image_description = "Hello Worl Nimbus %s" % (str(uuid.uuid4()))
cmd = "%s/bin/cloud-client.sh --imagedesc '%s' --transfer --sourcefile %s" % (cc_home, image_description, tst_image_src)
(x, rc)=pexpect.run(cmd, withexitstatus=1)
print x
if rc != 0:
    print "failed to transfer"
    sys.exit(1)

cmd = "%s/bin/cloud-client.sh --list" % (cc_home)
child = pexpect.spawn (cmd, timeout=to, maxread=20000, logfile=logfile)
rc = child.expect (tst_image_name)
if rc != 0:
    print "%s not found in the list" % (tst_image_name)
    sys.exit(1)
print child.before
rc = child.expect(image_description)
if rc != 0:
    print "%s not found in the list" % (image_description)
    sys.exit(1)
rc = child.expect(pexpect.EOF)
print child.before
if rc != 0:
    print "failed to list"
    sys.exit(1)

(x, rc)=pexpect.run("%s/bin/cloud-client.sh --delete --name %s" % (cc_home, tst_image_name) , withexitstatus=1)
print x
if rc != 0:
    print "failed to delete"
    sys.exit(1)
sys.exit(0)
