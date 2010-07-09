import string
import random
import os
import sys
import nose.tools
import boto
from boto.ec2.connection import EC2Connection
import boto.ec2 
import sys
from ConfigParser import SafeConfigParser
import time
import unittest
import tempfile
import filecmp
import pycb
import pynimbusauthz
from  pynimbusauthz.db import * 
from  pynimbusauthz.user import * 
import pycb.test_common
from boto.s3.connection import OrdinaryCallingFormat
from boto.s3.connection import S3Connection
import random
import nimbus_remove_user
import nimbus_new_user
import nimbus_list_users
import nimbus_edit_user

class TestUsersFailures(unittest.TestCase):


    def setUp(self):
        self.users = []
        self.nh = os.environ['NIMBUS_HOME']
        os.environ['NIMBUS_HOME'] = "/not/such place"

    def tearDown(self):
        os.environ['NIMBUS_HOME'] = self.nh
        for f in self.users:
            nimbus_remove_user.main([f])

    def get_user_name(self, friendly_name=None):
        if friendly_name == None:
            friendly_name = str(uuid.uuid1())
        self.users.append(friendly_name)
        return friendly_name

    def find_in_file(self, fname, needle):
        found = False
        f = open(fname)
        l = f.readline()
        while l:
            print "#### " + l
            x = l.find(needle)
            if x >= 0:
                found = True
            l = f.readline()
        f.close()
        return found


    def test_new_user(self):
        friendly_name = self.get_user_name()

        (tmpFD, outFileName) = tempfile.mkstemp("cumulustests")
        os.close(tmpFD)

        rc = nimbus_new_user.main([friendly_name])
        self.assertNotEqual(rc, 0, "should not be 0 %d" % (rc))

        # make sure the user was not added
        os.environ['NIMBUS_HOME'] = self.nh
        rc = nimbus_list_users.main(["-b", "-r", "display_name", "-O", outFileName, friendly_name])
        rc = self.find_in_file(outFileName, friendly_name)
        self.assertFalse(rc)

    def test_remove_user(self):
        friendly_name = self.get_user_name()

        (tmpFD, outFileName) = tempfile.mkstemp("cumulustests")
        os.close(tmpFD)

        # add a good user
        os.environ['NIMBUS_HOME'] = self.nh
        rc = nimbus_new_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

        os.environ['NIMBUS_HOME'] = "/nope"
        # remove with an error
        rc = nimbus_remove_user.main([friendly_name])
        self.assertNotEqual(rc, 0, "should not be 0 %d" % (rc))

        # relist to see user is still there
        os.environ['NIMBUS_HOME'] = self.nh
        rc = nimbus_list_users.main(["-b", "-r", "display_name", "-O", outFileName, friendly_name])
        self.assertEqual(rc, 0, "should not be 0 %d" % (rc))
        rc = self.find_in_file(outFileName, friendly_name)
        self.assertTrue(rc)



