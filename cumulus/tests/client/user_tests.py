import string
import random
import os
import sys
import nose.tools
import boto
from boto.s3.connection import OrdinaryCallingFormat
from boto.s3.connection import VHostCallingFormat
from boto.s3.connection import SubdomainCallingFormat
import sys
from ConfigParser import SafeConfigParser
from pycb.cumulus import *
import time
import pycb.test_common
import unittest
import tempfile
import filecmp
import pycb.tools.add_user
import pycb.tools.list_users
import pycb.tools.remove_user
#
class TestAddUsers(unittest.TestCase):

    def setUp(self):
        pass

    def tearDown(self):
        pass

    def find_in_file(self, fname, needle):
        found = False
        f = open(fname)
        l = f.readline()
        while l:
            x = l.find(needle)
            if x >= 0:
                found = True
            l = f.readline()
        f.close()
        os.unlink(fname)
        return found


    def test_new_user(self):
        rc = pycb.tools.add_user.main(["test@nosetests.nimbus.org"])
        self.assertEqual(rc, 0, "rc = %d" % (rc))
        rc = pycb.tools.remove_user.main(["test@nosetests.nimbus.org"])
        self.assertEqual(rc, 0, "rc = %d" % (rc))

    def test_list_user(self):
        display_name = str(uuid.uuid1())
        rc = pycb.tools.add_user.main([display_name])
        self.assertEqual(rc, 0, "rc = %d" % (rc))

        (tmpFD, outFileName) = tempfile.mkstemp("cumulustests")
        os.close(tmpFD)

        rc = pycb.tools.list_users.main(["-O", outFileName, display_name])
        self.assertEqual(rc, 0, "rc = %d" % (rc))
        rc = self.find_in_file(outFileName, display_name)
        self.assertTrue(rc, "display name not found in list")

        rc = pycb.tools.remove_user.main([display_name])
        self.assertEqual(rc, 0, "rc = %d" % (rc))

    def test_column_report(self):
        display_name = str(uuid.uuid1())
        rc = pycb.tools.add_user.main([display_name])
        self.assertEqual(rc, 0, "rc = %d" % (rc))

        (tmpFD, outFileName) = tempfile.mkstemp("cumulustests")
        os.close(tmpFD)

        rc = pycb.tools.list_users.main(["-O", outFileName, "-b", "-r", "friendly,quota", display_name])
        self.assertEqual(rc, 0, "rc = %d" % (rc))

        n = "%s,None" % (display_name)
        rc = self.find_in_file(outFileName, display_name)
        self.assertTrue(rc, "display name not found in list")

        rc = pycb.tools.remove_user.main([display_name])
        self.assertEqual(rc, 0, "rc = %d" % (rc))



