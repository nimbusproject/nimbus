import pexpect
import sys
from ConfigParser import SafeConfigParser


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

try:
    print "setting up ssh knowhosts"
    child = pexpect.spawn (cmd, timeout=8, maxread=20000, logfile=logfile)
    child.expect ('(yes/no)?')
    print child.before
    child.sendline ('yes')
except:
    pass
