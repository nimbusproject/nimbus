#!/usr/bin/env python

import pexpect
import sys
from ConfigParser import SafeConfigParser


cmd = "%s %s" % (sys.argv[1], sys.argv[2])
print cmd
logfile = sys.stdout
child = pexpect.spawn (cmd, timeout=800, maxread=200000, logfile=logfile)
child.expect ('CA Name:')
print child.before
child.sendline ('CA')
child.expect ('Hostname:')
print child.before
child.sendline ('localhost')
rc = child.expect(pexpect.EOF)
print child.before
x = child.readlines()
print x
