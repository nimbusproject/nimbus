#!/usr/bin/env python

import pexpect
import sys
import os
import re

to=int(os.environ["NIMBUS_TEST_TIMEOUT"])
tst_image_name = os.environ['NIMBUS_TEST_IMAGE']
tst_image_src = os.environ['NIMBUS_SOURCE_TEST_IMAGE']

cc_home=os.environ['CLOUD_CLIENT_HOME']
nimbus_home=os.environ['NIMBUS_HOME']
nimbus_user=os.environ['NIMBUS_TEST_USER']
group_name="UNLIMITED"
logfile = sys.stdout

def id_from_handle(handle):
    cmd = "%s/bin/cloud-client.sh --status " % (cc_home)
    #print cmd
    (out, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile, timeout=to)
    m = re.search(".*Workspace #(\d*?)\..*Handle: %s.*" % handle, out, re.S|re.M)
    if m:
        id = m.group(1)
    else:
        print "Error: Couldn't find ID of VM %s" % handle
	sys.exit(1)
    return id

def start_vm():
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

    return id_from_handle(handle)

def assert_no_vms():
    cmd = "%s/bin/nimbus-admin --list " % (nimbus_home)
    #print cmd
    (x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile, timeout=to)
    if rc != 0 or x != "":
        print "error 1"
        sys.exit(1)

def assert_vms():
    cmd = "%s/bin/nimbus-admin --list " % (nimbus_home)
    #print cmd
    (x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile, timeout=to)
    #print x
    if rc != 0 or x == "":
        print "error 2"
        sys.exit(1)


try:
	os.mkdir("%s/history/vm-999" % (cc_home))
except:
	print "The directory already exists"
	pass

cmd = "%s/bin/cloud-client.sh --transfer --sourcefile %s" % (cc_home, tst_image_src)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile, timeout=to)

id = start_vm()
assert_vms()

# Shutdown started VM
cmd = "%s/bin/nimbus-admin --shutdown --id %s" % (nimbus_home, id)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile, timeout=to)
print x
if rc != 0:
    print "error shutdown id %s" % (cmd)
    sys.exit(1)

assert_no_vms()


id = start_vm()
assert_vms()

# Shutdown started VM
cmd = "%s/bin/nimbus-admin --shutdown --dn /O=Auto/OU=CA/CN=%s" % (nimbus_home, nimbus_user)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile, timeout=to)
print x
if rc != 0:
    print "error shutdown dn %s" % (cmd)
    sys.exit(1)

assert_no_vms()

id = start_vm()
assert_vms()

cmd = "%s/bin/nimbus-admin --shutdown --host localhost" % (nimbus_home)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile, timeout=to)
print x
if rc != 0:
    print "error shutdown host %s" % (cmd)
    sys.exit(1)

assert_no_vms()

id = start_vm()
assert_vms()


cmd = "%s/bin/nimbus-admin --shutdown --gid 1" % (nimbus_home)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile, timeout=to)
print x
if rc != 0:
    print "error gid %s" % (cmd)
    sys.exit(1)

assert_no_vms()

id = start_vm()
assert_vms()


cmd = "%s/bin/nimbus-admin --shutdown --gname %s" % (nimbus_home, group_name)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile, timeout=to)
print x
if rc != 0:
    print "error gname %s" % (cmd)
    sys.exit(1)

assert_no_vms()

id = start_vm()
assert_vms()


cmd = "%s/bin/nimbus-admin --shutdown --user %s --seconds 35" % (nimbus_home, nimbus_user)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile, timeout=to)
print x
if rc != 0:
    print "error %dhutdown user %s" % (cmd)
    sys.exit(1)

assert_no_vms()

id = start_vm()
assert_vms()


cmd = "%s/bin/nimbus-admin --shutdown --all --seconds 30" % (nimbus_home)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile, timeout=to)
print x
if rc != 0:
    print "error shutdown all %s" % (cmd)
    sys.exit(1)

assert_no_vms()

cmd = "%s/bin/cloud-client.sh --delete --name %s" % (cc_home, tst_image_name)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile, timeout=to)
if rc != 0:
    print "error delete %s" % (cmd)
    sys.exit(1)

sys.exit(0)

