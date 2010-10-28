#!/usr/bin/env python

import pexpect
import sys
import os
import filecmp
import uuid
import datetime

to=90
cc_home=os.environ['CLOUD_CLIENT_HOME']
logfile = sys.stdout
newname=str(uuid.uuid1()).replace("-", "")
localfile=str(uuid.uuid1()).replace("-", "")

src_file = "/etc/group"
sfa = src_file.split("/")
image_name = sfa[len(sfa) - 1]
size=os.path.getsize(src_file)

cmd = "%s/bin/cloud-client.sh --transfer --sourcefile %s" % (cc_home, src_file)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)

cmd = "%s/bin/cloud-client.sh --run --name %s --hours .25" % (cc_home, image_name)
child = pexpect.spawn (cmd, timeout=to, maxread=20000, logfile=logfile)
rc = child.expect ('Running:')
if rc != 0:
    print "group not found in the list"
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

cmd="s3cmd info s3://Repo/VMS/%s/%s" % (os.environ['NIMBUS_TEST_USER_CAN_ID'], newname)
print cmd
child = pexpect.spawn (cmd, timeout=to, maxread=20000, logfile=logfile)
rc = child.expect ('MD5 sum:')
if rc != 0:
    print "group not found in the list"
    sys.exit(1)
sum1 = child.readline().strip()
rc = child.expect(pexpect.EOF)
if rc != 0:
    print "s3 info failed"
    sys.exit(1)
<<<<<<< HEAD
# down load the new name with s3cmd
cmd="s3cmd get s3://Repo/VMS/%s/%s %s" % (os.environ['NIMBUS_TEST_USER_CAN_ID'], image_name, localfile)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
print x
if rc != 0:
    print "failed to save"
    sys.exit(1)

rc = filecmp.cmp(localfile, "/etc/group")
os.remove(localfile)
if not rc:
    print "files differ"
    sys.exit(1)
=======
>>>>>>> f57bf2a56314b132ba2545f73f152d02e3530038

cmd="s3cmd info s3://Repo/VMS/%s/%s" % (os.environ['NIMBUS_TEST_USER_CAN_ID'], image_name)
print cmd
child = pexpect.spawn (cmd, timeout=to, maxread=20000, logfile=logfile)
rc = child.expect ('MD5 sum:')
if rc != 0:
    print "group not found in the list"
    sys.exit(1)
sum2 = child.readline().strip()
rc = child.expect(pexpect.EOF)
if rc != 0:
    print "s3 info failed"
    sys.exit(1)

if sum1 != sum2:
    print "sums not the same %s %s" % (sum1, sum2)
    sys,exit(1)

cmd = "%s/bin/cloud-client.sh --delete --name %s" % (cc_home, newname)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
cmd = "%s/bin/cloud-client.sh --delete --name %s" % (cc_home, image_name)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)


sys.exit(0)


