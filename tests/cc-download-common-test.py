#!/usr/bin/env python

import pexpect
import sys
import os
import uuid
import tempfile
import filecmp

to=90
cc_home=os.environ['CLOUD_CLIENT_HOME']
nh=os.environ['NIMBUS_HOME']
logfile = sys.stdout
common_image = str(uuid.uuid1()).replace("-", "")

cmd = "%s/bin/nimbus-public-image /etc/group %s" % (nh, common_image)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
if rc != 0:
    print "failed create the public image"
    sys.exit(1)

(tmpFD, outFileName) = tempfile.mkstemp()
os.close(tmpFD)
cmd = "%s/bin/cloud-client.sh --force --download --name %s --localfile=%s" % (cc_home, common_image, outFileName)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
if rc == 0:
    print "This should have had an error"
    sys.exit(1)
cmd = "%s/bin/cloud-client.sh --common --download --name %s --localfile=%s" % (cc_home, common_image, outFileName)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
if rc != 0:
    print "Download of the common image failed"
    sys.exit(1)

rc = filecmp.cmp(outFileName, "/etc/group")
os.remove(outFileName)
if rc != 0:
    sys.exit(1)

cmd = "%s/bin/nimbus-public-image --delete %s" % (nh, common_image)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
if rc != 0:
    print "failed create the public image"
    sys.exit(1)
sys.exit(0)
