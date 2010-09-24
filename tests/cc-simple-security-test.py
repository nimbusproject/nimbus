#!/usr/bin/env python

import pexpect
import sys
import os

to=90
cc_home=os.environ['CLOUD_CLIENT_HOME']
logfile = sys.stdout

cmd = "%s/bin/cloud-client.sh --security" % (cc_home)
child = pexpect.spawn (cmd, timeout=to, maxread=20000, logfile=logfile)
rc = child.expect ('Identity:')
if rc != 0:
    print "group not found in the list"
    sys.exit(1)
rc = child.expect ('Subject:')
if rc != 0:
    print "group not found in the list"
    sys.exit(1)
rc = child.expect ('Issuer:')
if rc != 0:
    print "group not found in the list"
    sys.exit(1)
rc = child.expect(pexpect.EOF)
if rc != 0:
    print "run"
    sys.exit(1)
sys.exit(0)
