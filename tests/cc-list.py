import pexpect
import sys
import os

cc_home=os.environ['CLOUD_CLIENT_HOME']
logfile = sys.stdout

(x, rc)=pexpect.run("%s/bin/cloud-client.sh --delete --name group" % (cc_home), withexitstatus=1)
print x

cmd = "%s/bin/cloud-client.sh --transfer --sourcefile /etc/group" % (cc_home)
(x, rc)=pexpect.run(cmd, withexitstatus=1)
print x
if rc != 0:
    sys.exit(1)

cmd = "%s/bin/cloud-client.sh --list" % (cc_home)
child = pexpect.spawn (cmd, timeout=30, maxread=20000, logfile=logfile)
child.expect ('group')
print child.before
x = child.readlines()
print x
if child.status != 0:
    sys.exit(1)

(x, rc)=pexpect.run("%s/bin/cloud-client.sh --delete --name group" % (cc_home) , withexitstatus=1)
print x
if rc != 0:
    sys.exit(1)
sys.exit(0)
