#!/usr/bin/env python

import pexpect
import sys
import os
import re

tst_ca = os.environ['NIMBUS_TEST_CA']

to=int(os.environ["NIMBUS_TEST_TIMEOUT"])
tst_image_name = os.environ['NIMBUS_TEST_IMAGE']
tst_image_src = os.environ['NIMBUS_SOURCE_TEST_IMAGE']

cc_home=os.environ['CLOUD_CLIENT_HOME']
nimbus_home=os.environ['NIMBUS_HOME']
nimbus_user=os.environ['NIMBUS_TEST_USER']
logfile = sys.stdout

try:
	os.mkdir("%s/history/vm-999" % (cc_home))
except:
	print "The directory already exists"
	pass

cmd = "%s/bin/nimbus-list-users %%" % (nimbus_home)
(x, rc)=pexpect.run(cmd, withexitstatus=1, timeout=to)
print x

cmd = "%s/bin/cloud-client.sh --transfer --sourcefile %s" % (cc_home, tst_image_src)
(x, rc)=pexpect.run(cmd, withexitstatus=1, timeout=to)

cmd = "%s/bin/cloud-client.sh --run --name %s --hours .25" % (cc_home, tst_image_name)
child = pexpect.spawn (cmd, timeout=to, maxread=20000, logfile=logfile)
rc = child.expect ('Running:')
if rc != 0:
    print "%s not found in the list" % (tst_image_name)
    sys.exit(1)
handle = child.readline().strip().replace("'", "")
rc = child.expect(pexpect.EOF)
if rc != 0:
    print "run"
    sys.exit(1)

cmd = "%s/bin/nimbus-admin --list --user %s" % (nimbus_home, nimbus_user)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1, timeout=to)
print x
if rc != 0 or not re.match(".*id\s*?:\s*?\d.*", x):
    print "error"
    sys.exit(1)

cmd = "%s/bin/nimbus-admin --list --dn %s/CN=%s" % (nimbus_home, tst_ca, nimbus_user)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1, timeout=to)
print x
if rc != 0 or not re.match(".*id\s*?:\s*?\d.*", x):
	print "error"
	sys.exit(1)

cmd = "%s/bin/nimbus-admin --list --host localhost" % (nimbus_home)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1, timeout=to)
print x
if rc != 0 or not re.match(".*id\s*?:\s*?\d.*", x):
    print "error"
    sys.exit(1)

cmd = "%s/bin/nimbus-admin --list --gid 1" % (nimbus_home)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1, timeout=to)
print x
if rc != 0 or not re.match(".*id\s*?:\s*?\d.*", x):
    print "error"
    sys.exit(1)

cmd = "%s/bin/nimbus-admin --list --gname UNLIMITED" % (nimbus_home)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1, timeout=to)
print x
if rc != 0 or not re.match(".*id\s*?:\s*?\d.*", x):
    print "error"
    sys.exit(1)

cmd = "%s/bin/nimbus-admin --list" % (nimbus_home)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1, timeout=to)
print x
if rc != 0 or not re.match(".*id\s*?:\s*?\d.*", x):
    print "error"
    sys.exit(1)

cmd = "%s/bin/cloud-client.sh --run --name %s --hours .25" % (cc_home, tst_image_name)
child = pexpect.spawn (cmd, timeout=to, maxread=20000, logfile=logfile)
rc = child.expect ('Running:')
if rc != 0:
    print "%s not found in the list" % (tst_image_name)
    sys.exit(1)
handle = child.readline().strip().replace("'", "")
rc = child.expect(pexpect.EOF)
if rc != 0:
    print "run"
    sys.exit(1)

cmd = "%s/bin/nimbus-admin --nodes" % (nimbus_home)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1, timeout=to)
print x
if rc != 0:
    print "error"
    sys.exit(1)
if not re.search("node\s*:\s*localhost", x):
    print "not showing localhost node?"
    sys.exit(1)
if not re.search("id\s*:\s*\d*,\s\d*", x):
    print "not showing two vms on localhost node?"
    sys.exit(1)

cmd = "%s/bin/nimbus-admin --batch --shutdown --all" % (nimbus_home)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1, timeout=to)
print x
if rc != 0:
    print "error"
    sys.exit(1)

cmd = "%s/bin/cloud-client.sh --delete --name %s" % (cc_home, tst_image_name)
(x, rc)=pexpect.run(cmd, withexitstatus=1, timeout=to)
if rc != 0:
    print "error"
    sys.exit(1)

sys.exit(0)
