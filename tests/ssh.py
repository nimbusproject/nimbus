#!/usr/bin/env python

import pexpect
import sys
from ConfigParser import SafeConfigParser

logfile = sys.stdout
print "try ssh"
try:
    print "setting up ssh knowhosts"
    cmd = "ssh localhost hostname"
    child = pexpect.spawn (cmd, timeout=8, maxread=20000, logfile=logfile)
    child.expect ('(yes/no)?')
    print child.before
    child.sendline ('yes')
    rc = child.expect(pexpect.EOF)
    print child.before
except Exception, ex:
    print ex
