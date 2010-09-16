#!/usr/bin/env python

import pexpect
import sys
import shutil
import os


logfile = sys.stdout
print "try ssh"
try:
    cmd="ssh-keygen -f %s" % (sys.argv[1])
    cmd="ssh-keygen"
    child = pexpect.spawn (cmd, timeout=30, maxread=20000, logfile=logfile)
    child.expect (':')
    child.sendline ('')
    child.expect (':')
    child.sendline ('')
    rc = child.expect(pexpect.EOF)

    cmd = "cp %s.pub %s/.ssh/authorized_keys" % (sys.argv[1], os.environ['HOME'])
    print cmd
    child = pexpect.spawn (cmd, timeout=10, maxread=20000, logfile=logfile)
    rc = child.expect(pexpect.EOF)
#    print child.before

    print "setting up ssh knowhosts"
    cmd = "ssh -i %s localhost hostname" % (sys.argv[1])
    child = pexpect.spawn (cmd, timeout=10, maxread=20000, logfile=logfile)
    child.expect ('(yes/no)?')
#    print child.before
    child.sendline ('yes')
    rc = child.expect(pexpect.EOF)
#    print child.before

except Exception, ex:
    print ex



