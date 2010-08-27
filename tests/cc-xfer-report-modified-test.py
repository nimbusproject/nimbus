#!/usr/bin/env python

import pexpect
import sys
import os

cc_home=os.environ['CLOUD_CLIENT_HOME']
logfile = sys.stdout

src_file = "/etc/group"
sfa = src_file.split("/")
image_name = sfa[len(sfa) - 1]
size = os.path.getsize(src_file)
cmd = "%s/bin/cloud-client.sh --transfer --sourcefile %s" % (cc_home, src_file)
(x, rc)=pexpect.run(cmd, withexitstatus=1)

cmd = "%s/bin/cloud-client.sh --list" % (cc_home)
child = pexpect.spawn (cmd, timeout=30, maxread=20000, logfile=logfile)
rc = child.expect (image_name)
if rc != 0:
    print "%s not found in the list" % (image_name)
    sys.exit(1)

line = child.readline()
token = "Modified: "
ndx = line.find(token)
if ndx < 0:
    print "%s not found in line %s" % (token, line)
    sys.exit(1)
line = line[ndx + len(token):]
ndx = line.find("Size")
if ndx < 0:
    print "%s not found in line %s" % (token, line)
    sys.exit(1)
line = line[0:ndx].strip()

print "modified at %s" % (line)

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
