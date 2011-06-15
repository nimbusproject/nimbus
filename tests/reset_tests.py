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
import nimbus_reset_state

class TestUserReset(unittest.TestCase):


    def setUp(self):
        self.users = []
        (osf, self.fname) = tempfile.mkstemp()
        os.close(osf)
        con = pynimbusauthz.db.make_test_database(db_str=self.fname)
        con.close()
        self.dbenv = os.environ['NIMBUS_AUTHZ_DB']
        os.environ['NIMBUS_AUTHZ_DB'] = self.fname

    def tearDown(self):
        for f in self.users:
            nimbus_remove_user.main([f])
        os.remove(self.fname)
        os.environ['NIMBUS_AUTHZ_DB'] = self.dbenv

    def get_user_name(self, friendly_name=None):
        if friendly_name == None:
            friendly_name = str(uuid.uuid1())
        self.users.append(friendly_name)
        return friendly_name

    def test_call_empty(self):
        rc = nimbus_reset_state.main(["-u", "-f"])
        self.assertEqual(rc, 0)

    def test_rest_users(self):
        for i in range(0, 10):
            friendly_name = self.get_user_name()
            rc = nimbus_new_user.main([friendly_name])
            self.assertEqual(rc, 0, "should not be 0 %d" % (rc))

        dbobj = DB(con_str=self.dbenv)
        rc = nimbus_reset_state.main(["-u", "-f"])
        self.assertEqual(rc, 0)

        # make sure that none are in there
        allu = User.find_user_by_friendly(dbobj, '%')
        self.assertEqual(list(allu), 0)


