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

src_file = "/etc/group"
sfa = src_file.split("/")
image_name = sfa[len(sfa) - 1]
size=os.path.getsize(src_file)

cmd = "%s/bin/nimbus-public-image %s %s" % (nh, src_file, common_image)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
if rc != 0:
    print "failed create the public image"
    sys.exit(1)

cmd = "%s/bin/cloud-client.sh --run --name %s --hours .25" % (cc_home, common_image)
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

cmd = "%s/bin/cloud-client.sh --save --handle %s" % (cc_home, handle)
print cmd
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
print x
if rc != 0:
    print "failed to terminate"
    sys.exit(1)

# check the various params
cmd = "%s/bin/cloud-client.sh --list" % (cc_home)
child = pexpect.spawn (cmd, timeout=to, maxread=20000, logfile=logfile)
rc = child.expect (common_image)
if rc != 0:
    print "%s not found in the list" % (common_image)
    sys.exit(1)

line = child.readline()
print line
line = child.readline()
print line
token = "Size: "
ndx = line.find(token)
if ndx < 0:
    print "%s not found in line %s" % (token, line)
    sys.exit(1)
line = line[ndx + len(token):]
ndx = line.find(" ")
if ndx < 0:
    print "%s ndx space not found %s" % (token, line)
    sys.exit(1)
line = line[0:ndx].strip()

show_size = int(line)

if show_size != size:
    print "%d != %d" % (show_size, size)
    sys.exit(1)


cmd = "%s/bin/nimbus-public-image --delete %s" % (nh, common_image)
(x, rc)=pexpect.run(cmd, withexitstatus=1, logfile=logfile)
if rc != 0:
    print "failed create the public image"
    sys.exit(1)
sys.exit(0)
