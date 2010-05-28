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
import pycb.tools.remove_user
#
class TestAddUsers(unittest.TestCase):

    def setUp(self):
        pass

    def tearDown(self):
        pass

    def test_new_user(self):
        rc = pycb.tools.add_user.main(["test@nosetests.nimbus.org"])
        self.assertEqual(rc, 0, "rc = %d" % (rc))
        rc = pycb.tools.remove_user.main(["test@nosetests.nimbus.org"])
        self.assertEqual(rc, 0, "rc = %d" % (rc))
