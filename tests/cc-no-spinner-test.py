#!/usr/bin/env python

import pexpect
import sys
import os
import uuid
import tempfile
import filecmp

to=int(os.environ["NIMBUS_TEST_TIMEOUT"])
cc_home=os.environ['CLOUD_CLIENT_HOME']
nh=os.environ['NIMBUS_HOME']
logfile = sys.stdout
common_image = str(uuid.uuid1()).replace("-", "")
user_image_src = "/bin/bash"
os.system("cp /bin/bash /tmp/%s" % (common_image))

# XXX TODO XXX need a way verify that the spinner is actually not printed.
cmd = "%s/bin/cloud-client.sh --nospinner --transfer --sourcefile /tmp/%s" % (cc_home, common_image)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
os.remove("/tmp/%s" % (common_image))
if rc != 0:
    print "Unable to upload the user image with nospinner"
    sys.exit(1)

cmd = "%s/bin/cloud-client.sh --nospinner --download --name %s --localfile=/tmp/%s_dummy" % (cc_home, common_image, common_image)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
os.remove("/tmp/%s_dummy" % (common_image))
if rc != 0:
    print "Unable to download the user image with nospinner"
    sys.exit(1)

cmd = "%s/bin/cloud-client.sh --delete --name %s" % (cc_home, common_image)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1)
if rc != 0:
    print "Unable to delete the image"
    sys.exit(1)

print "Success"
sys.exit(0)


