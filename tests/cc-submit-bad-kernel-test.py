#!/usr/bin/env python

import pexpect
import sys
import os

tst_image_name = os.environ['NIMBUS_TEST_IMAGE']
tst_image_src = os.environ['NIMBUS_SOURCE_TEST_IMAGE']
to=int(os.environ["NIMBUS_TEST_TIMEOUT"])
cc_home=os.environ['CLOUD_CLIENT_HOME']
logfile = sys.stdout

cmd = "%s/bin/cloud-client.sh --transfer --sourcefile %s" % (cc_home, tst_image_src)
(x, rc)=pexpect.run(cmd, withexitstatus=1, timeout=to)

cmd = "%s/bin/cloud-client.sh --kernel notthere --run --name %s --hours .5" % (cc_home, tst_image_name)
(x, rc)=pexpect.run(cmd, withexitstatus=1, timeout=to)
if rc == 0:
    print "a bad kernel was submitted, should have failed"
    sys.exit(1)

sys.exit(0)
