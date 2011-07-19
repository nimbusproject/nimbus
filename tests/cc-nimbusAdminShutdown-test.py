#!/usr/bin/env python

import pexpect
import sys
import os

to=90
cc_home=os.environ['CLOUD_CLIENT_HOME']
nimbus_home=os.environ['NIMBUS_HOME']
logfile = sys.stdout

try:
	os.mkdir("%s/history/vm-999" % (cc_home))
except:
	print "The directory already exists"
	pass

cmd = "%s/bin/cloud-client.sh --transfer --sourcefile /etc/group" % (cc_home)
(x, rc)=pexpect.run(cmd, withexitstatus=1)

cmd = "%s/bin/cloud-client.sh --run --name group --hours .25" % (cc_home)
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

cmd = "%s/bin/nimbus-admin --shutdown --id 1" % (nimbus_home)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1)
print x
if rc != 0:
    print "error"
    sys.exit(1)

cmd = "%s/bin/cloud-client.sh --run --name group --hours .25" % (cc_home)
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

cmd = "%s/bin/nimbus-admin --shutdown --dn /O=Auto/OU=CA/CN=nimbus@" % (nimbus_home)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1)
print x
if rc != 0:
	print "error"
	sys.exit(1)

cmd = "%s/bin/nimbus-admin --shutdown --host localhost" % (nimbus_home)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1)
print x
if rc != 0:
    print "error"
    sys.exit(1)

cmd = "%s/bin/cloud-client.sh --run --name group --hours .25" % (cc_home)
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

cmd = "%s/bin/nimbus-admin --shutdown --gid 1" % (nimbus_home)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1)
print x
if rc != 0:
    print "error"
    sys.exit(1)

cmd = "%s/bin/cloud-client.sh --run --name group --hours .25" % (cc_home)
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

cmd = "%s/bin/nimbus-admin --shutdown --gname groupname" % (nimbus_home)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1)
print x
if rc != 0:
    print "error"
    sys.exit(1)

cmd = "%s/bin/nimbus-admin --shutdown --user user1 --seconds 35" % (nimbus_home)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1)
print x
if rc != 0:
    print "error"
    sys.exit(1)

cmd = "%s/bin/nimbus-admin --shutdown --all --seconds 30" % (nimbus_home)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1)
print x
if rc != 0:
    print "error"
    sys.exit(1)

cmd = "%s/bin/cloud-client.sh --delete --name group" % (cc_home)
(x, rc)=pexpect.run(cmd, withexitstatus=1)
if rc != 0:
    print "error"
    sys.exit(1)

sys.exit(0)
