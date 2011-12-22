#!/usr/bin/env python
import uuid
import base64
import os
from boto.ec2.connection import EC2Connection
import unittest
import pycb
import pynimbusauthz
from  pynimbusauthz.db import *
from  pynimbusauthz.user import *
import pycb.test_common
import pexpect
import sys
import os

tst_image_name = os.environ['NIMBUS_TEST_IMAGE']
tst_image_src = os.environ['NIMBUS_SOURCE_TEST_IMAGE']
to=int(os.environ["NIMBUS_TEST_TIMEOUT"])
cc_home=os.environ['CLOUD_CLIENT_HOME']
logfile = sys.stdout

src_file = tst_image_src
sfa = src_file.split("/")
image_name = sfa[len(sfa) - 1]
size = os.path.getsize(src_file)
cmd = "%s/bin/cloud-client.sh --transfer --sourcefile /etc/group" % (cc_home)
(x, rc)=pexpect.run(cmd, withexitstatus=1)
cmd = "%s/bin/cloud-client.sh --transfer --sourcefile /bin/sh" % (cc_home)
(x, rc)=pexpect.run(cmd, withexitstatus=1)

# boto stuff
host = 'localhost'
ec2port = 8444
try:
    ec2port = int(os.environ['NIMBUS_TEST_EC2_PORT'])
except:
    pass

db = DB(pycb.config.authzdb)
friendly = os.environ['NIMBUS_TEST_USER']
can_user = User.get_user_by_friendly(db, friendly)
s3a = can_user.get_alias_by_friendly(friendly, pynimbusauthz.alias_type_s3)

s3id = s3a.get_name()
s3pw = s3a.get_data()

ec2conn = EC2Connection(s3id, s3pw, host=host, port=ec2port)
ec2conn.host = host

image_name = "sdfsfdsfsd"
image_obj = ec2conn.get_image(image_name)
obj_name = str(image_obj).replace("Image:", "")
print image_obj
print obj_name
if image_name == obj_name:
    print "the image should not match %s %s"  % (image_name, obj_name)
    sys.exit(1)

image_name = "sh"
image_obj = ec2conn.get_image(image_name)
obj_name = str(image_obj).replace("Image:", "")
print obj_name
print image_obj
if image_name != obj_name:
    print "the image should match %s %s"  % (image_name, obj_name)
    sys.exit(1)

image_name = "group"
image_obj = ec2conn.get_image(image_name)
obj_name = str(image_obj).replace("Image:", "")
print image_obj
print obj_name
if image_name != obj_name:
    print "the image should match %s %s"  % (image_name, obj_name)
    sys.exit(1)


cmd = "%s/bin/cloud-client.sh --delete --name group" % (cc_home)
(x, rc)=pexpect.run(cmd, withexitstatus=1)
print x
if rc != 0:
    print "failed to delete"
    sys.exit(1)
cmd = "%s/bin/cloud-client.sh --delete --name sh" % (cc_home)
(x, rc)=pexpect.run(cmd, withexitstatus=1)
print x
if rc != 0:
    print "failed to delete"
    sys.exit(1)
sys.exit(0)
