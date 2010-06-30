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

class TestUsers(unittest.TestCase):

    def setUp(self):
        self.users = []    

    def tearDown(self):
        for f in self.users:
            nimbus_remove_user.main([f])

    def get_user_name(self, friendly_name=None):
        if friendly_name == None:
            friendly_name = str(uuid.uuid1())
        self.users.append(friendly_name)
        return friendly_name    

    def test_make_remove_user(self):
        friendly_name = self.get_user_name()
        rc = nimbus_new_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))
        rc = nimbus_remove_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

    def test_make_user_twice(self):
        friendly_name = self.get_user_name()
        rc = nimbus_new_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))
        rc = nimbus_new_user.main([friendly_name])
        self.assertNotEqual(rc, 0, "should be 0 %d" % (rc))

        rc = nimbus_remove_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

    def test_remove_user_twice(self):
        friendly_name = self.get_user_name()
        rc = nimbus_new_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

        rc = nimbus_remove_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))
        rc = nimbus_remove_user.main([friendly_name])
        self.assertNotEqual(rc, 0, "should not be 0 %d" % (rc))

    def test_add_remove_add_remove(self):
        friendly_name = self.get_user_name()
        rc = nimbus_new_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))
        rc = nimbus_remove_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

        rc = nimbus_new_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))
        rc = nimbus_remove_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

    def test_remove_unknown_user(self):
        friendly_name = self.get_user_name()
        rc = nimbus_remove_user.main([friendly_name])
        self.assertNotEqual(rc, 0, "should be 0 %d" % (rc))

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

    def test_new_user_s3ids(self):
        friendly_name = self.get_user_name()

        (tmpFD, outFileName) = tempfile.mkstemp("cumulustests")
        os.close(tmpFD)

        s3id = str(uuid.uuid1())
        s3pw = str(uuid.uuid1())
        rc = nimbus_new_user.main(["-a", s3id, "-p", s3pw, "-b", "-r", "access_id,access_secret", "-O", outFileName, friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))
        needle = "%s,%s" % (s3id, s3pw)
        print needle
        rc = self.find_in_file(outFileName, needle)
        os.unlink(outFileName)
        self.assertTrue(rc)

        rc = nimbus_remove_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

    def test_no_cert(self):
        friendly_name = self.get_user_name()

        (tmpFD, outFileName) = tempfile.mkstemp("cumulustests")
        os.close(tmpFD)

        s3id = str(uuid.uuid1())
        s3pw = str(uuid.uuid1())
        rc = nimbus_new_user.main(["--noaccess", "-b", "-r", "access_id,access_secret", "-O", outFileName, friendly_name])
        needle = "None,None"
        rc = self.find_in_file(outFileName, needle)
        os.unlink(outFileName)
        self.assertTrue(rc)
        
        rc = nimbus_remove_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

    def test_no_s3(self):
        friendly_name = self.get_user_name()

        (tmpFD, outFileName) = tempfile.mkstemp("cumulustests")
        os.close(tmpFD)

        s3id = str(uuid.uuid1())
        s3pw = str(uuid.uuid1())
        rc = nimbus_new_user.main(["--nocert", "-b", "-r", "cert,key,dn", "-O", outFileName, friendly_name])
        needle = "None,None,None" 
        rc = self.find_in_file(outFileName, needle)
        os.unlink(outFileName)
        self.assertTrue(rc)
        
        rc = nimbus_remove_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

    def test_edit_user(self):
        friendly_name = self.get_user_name()

        (tmpFD, outFileName) = tempfile.mkstemp("cumulustests")
        os.close(tmpFD)

        s3id = str(uuid.uuid1())
        s3pw = str(uuid.uuid1())
        rc = nimbus_new_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))
        rc = nimbus_edit_user.main(["-b", "-a", s3id, "-p", s3pw, "-r", "access_id,access_secret", "-O", outFileName, friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))
        needle = "%s,%s" % (s3id, s3pw)
        print "--> %s <--" % (needle)
        rc = self.find_in_file(outFileName, needle)
        os.unlink(outFileName)
        self.assertTrue(rc)
        
        rc = nimbus_remove_user.main([friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

    def test_list_user(self):
        name1 = self.get_user_name()
        rc = nimbus_new_user.main([name1])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

        name2 = self.get_user_name()
        rc = nimbus_new_user.main([name2])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

        name3 = self.get_user_name()
        rc = nimbus_new_user.main([name3])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))

        (tmpFD, outFileName) = tempfile.mkstemp("cumulustests")
        os.close(tmpFD)
        rc = nimbus_list_users.main(["-b", "-r", "display_name", "-O", outFileName, '%'])
        rc = self.find_in_file(outFileName, name1)
        self.assertTrue(rc)

        rc = self.find_in_file(outFileName, name2)
        self.assertTrue(rc)

        rc = self.find_in_file(outFileName, name3)
        self.assertTrue(rc)

        os.unlink(outFileName)

    def test_db_commit_user(self):
        friendly_name = self.get_user_name(friendly_name="test1@nimbus.test")
        rc = nimbus_new_user.main(["-W", friendly_name])
        self.assertEqual(rc, 0, "should be 0 %d" % (rc))
        friendly_name = self.get_user_name(friendly_name="test1@nimbus22.test")
        rc = nimbus_new_user.main(["-W", friendly_name])
        self.assertNotEqual(rc, 0, "we expect this one to fail %d" % (rc))
        friendly_name = self.get_user_name(friendly_name="test1@nimbus22.test")
        rc = nimbus_new_user.main(["-W", "-w", "NewName", friendly_name])
        self.assertEqual(rc, 0, "but then this clarification should succeed %d" % (rc))

