import pexpect
import sys

cmd = "%s %s" % (sys.argv[1], sys.argv[2])
logfile = sys.stdout
child = pexpect.spawn (cmd, timeout=800, maxread=200000, logfile=logfile)
child.expect ('CA Name:')
print child.before
child.sendline ('CA')
child.expect ('Hostname:')
print child.before
child.sendline ('localhost')
x = child.readlines()
print x
