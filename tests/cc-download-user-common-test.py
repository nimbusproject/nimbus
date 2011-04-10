#!/usr/bin/env python

import pexpect
import sys
import os
import uuid

to=90
cc_home=os.environ['CLOUD_CLIENT_HOME']
nh=os.environ['NIMBUS_HOME']
logfile = sys.stdout
common_image = str(uuid.uuid1()).replace("-", "")
user_image_src = "/bin/bash"
os.system("cp /bin/bash /tmp/%s" % (common_image))

print "upload common image %s" % (common_image)
cmd = "%s/bin/nimbus-public-image /etc/group %s" % (nh, common_image)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
if rc != 0:
    print "failed create the public image"
    sys.exit(1)

print "upload user image with same name"
cmd = "%s/bin/cloud-client.sh --transfer --sourcefile /tmp/%s" % (cc_home, common_image)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)


(tmpFD, userfile) = tempfile.mkstemp()
os.close(tmpFD)
cmd = "%s/bin/cloud-client.sh --download --name %s --localfile=%s" % (cc_home, common_image, userfile)
(x, rc)=pexpect.run(cmd, withexitstatus=1)
if rc != 0:
    print "Unable to download the user image"
    sys.exit(1)
(tmpFD, common_file) = tempfile.mkstemp()
os.close(tmpFD)
cmd = "%s/bin/cloud-client.sh --common --download --name %s --localfile=%s" % (cc_home, common_image, common_file)
(x, rc)=pexpect.run(cmd, withexitstatus=1)
if rc != 0:
    print "Down load of the common image failed"
    sys.exit(1)

rc = filecmp.cmp(common_file, "/etc/group")
os.remove(outFileName)
if rc != 0:
    print "The common file download was wrong"
    sys.exit(1)

rc = filecmp.cmp(userfile, "/bin/bash")
os.remove(outFileName)
if rc != 0:
    print "The user file download was wrong"
    sys.exit(1)

cmd = "%s/bin/nimbus-public-image --delete %s" % (nh, common_image)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
if rc != 0:
    print "failed create the public image"
    sys.exit(1)
cmd = "%s/bin/cloud-client.sh --delete --name %s" % (cc_home, common_image)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1)

sys.exit(0)


