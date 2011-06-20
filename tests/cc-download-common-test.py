#!/usr/bin/env python

import pexpect
import sys
import os
import uuid
import tempfile
import filecmp

tst_image_name = os.environ['NIMBUS_TEST_IMAGE']
tst_image_src = os.environ['NIMBUS_SOURCE_TEST_IMAGE']


to=int(os.environ["NIMBUS_TEST_TIMEOUT"])
cc_home=os.environ['CLOUD_CLIENT_HOME']
nh=os.environ['NIMBUS_HOME']
logfile = sys.stdout
common_image = str(uuid.uuid1()).replace("-", "")

cmd = "%s/bin/nimbus-public-image %s %s" % (nh, tst_image_src, common_image)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
if rc != 0:
    print "failed create the public image"
    sys.exit(1)

outFileName = "/tmp/%s" % (common_image)
cmd = "%s/bin/cloud-client.sh --download --name %s --localfile=%s" % (cc_home, common_image, outFileName)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
if rc == 0:
    print "This should have had an error"
    sys.exit(1)
cmd = "%s/bin/cloud-client.sh --common --download --name %s --localfile=%s" % (cc_home, common_image, outFileName)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
if rc != 0:
    print "Download of the common image failed"
    sys.exit(1)

rc = filecmp.cmp(outFileName, tst_image_src)
os.remove(outFileName)
if not rc:
    sys.exit(1)

cmd = "%s/bin/nimbus-public-image --delete %s" % (nh, common_image)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
if rc != 0:
    print "failed create the public image"
    sys.exit(1)
print "Success"
sys.exit(0)
