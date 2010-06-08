import sqlite3
import os
import sys
import pynimbusauthz
from pynimbusauthz.db import DB
from pynimbusauthz.user import User
from pynimbusauthz.user import UserAlias
import pynimbusauthz.add_user 
import unittest
import uuid
import tempfile


class TestAddUserCli(unittest.TestCase):

    def setUp(self):
        (osf, self.fname) = tempfile.mkstemp()
        os.close(osf)
        os.environ['NIMBUS_AUTHZ_DB'] = self.fname
        pynimbusauthz.db.make_test_database(self.fname)
        self.db = DB(con_str=self.fname)

    def tearDown(self):
        self.db.close()
        os.remove(self.fname)

    def test_basic_adduser(self):
        uu = str(uuid.uuid1())
        rc = pynimbusauthz.add_user.main(["-n", uu])
        self.assertEqual(rc, 0, "CLI should return success")
        user = User(self.db, uu)
        rc = pynimbusauthz.add_user.main(["-r", uu])
        self.assertEqual(rc, 0, "CLI should return success")
        try:
            user = User(self.db, uu)
            self.fail("should have had an exception loading user")
        except:
            pass

    def test_basic_alias(self):
        user = User(self.db)
        uu = user.get_id()
        aname = "alias1"
        self.db.commit()
        rc = pynimbusauthz.add_user.main(["-a", aname, uu])
        self.assertEqual(rc, 0, "CLI should return success")
        ua = user.get_alias(aname, pynimbusauthz.alias_type_s3)
        self.assertNotEqual(ua, None, "alias not found")
        rc = pynimbusauthz.add_user.main(["-x", aname, uu])
        self.assertEqual(rc, 0, "CLI should return success")
        ua = user.get_alias(aname, pynimbusauthz.alias_type_s3)
        self.assertEqual(ua, None, "alias should not be found")

    def test_user_alias_remove(self):
        aname = str(uuid.uuid1())
        uu = str(uuid.uuid1())
        rc = pynimbusauthz.add_user.main(["-n", "-a", aname, uu])
        self.assertEqual(rc, 0, "CLI should return success")

        user = User(self.db, uu)
        ua = user.get_alias(aname, pynimbusauthz.alias_type_s3)

        rc = pynimbusauthz.add_user.main(["-x", aname, "-r", uu])
        self.assertEqual(rc, 0, "CLI should return success")

        try:
            user = User(self.db, uu)
            self.fail("should have had an exception loading user")
        except:
            pass
        try:
            ua = user.get_alias(aname, pynimbusauthz.alias_type_s3)
            self.fail("should have had an exception loading user")
        except:
            pass

    def test_set_key(self):
        aname = str(uuid.uuid1())
        uu = str(uuid.uuid1())
        rc = pynimbusauthz.add_user.main(["-n", "-a", aname, uu])
        self.assertEqual(rc, 0, "CLI should return success")
        rc = pynimbusauthz.add_user.main(["-g", "-a", aname, uu])
        self.assertEqual(rc, 0, "CLI should return success")
 
        key = str(uuid.uuid1())
        rc = pynimbusauthz.add_user.main(["-k", key, "-a", aname, uu])
        self.assertEqual(rc, 0, "CLI should return success")

        user = User(self.db, uu)
        ua = user.get_alias(aname, pynimbusauthz.alias_type_s3)
        self.db.commit()
        k2 = ua.get_data()
        self.assertEqual(k2, key)

    def test_default_new_user(self):
        rc = pynimbusauthz.add_user.main(["-n"])
        self.assertEqual(rc, 0, "CLI should return success %d"%(rc))

    def test_bad_args(self):
        uu = str(uuid.uuid1())
        rc = pynimbusauthz.add_user.main([])
        self.assertNotEqual(rc, 0, "CLI should not return success %d"%(rc))
        rc = pynimbusauthz.add_user.main(["-k", uu, uu])
        self.assertNotEqual(rc, 0, "CLI should not return success %d"%(rc))
        rc = pynimbusauthz.add_user.main(["-g", uu])
        self.assertNotEqual(rc, 0, "CLI should not return success %d"%(rc))

    def test_remove_force(self):
        aname = str(uuid.uuid1())
        uu = str(uuid.uuid1())
        rc = pynimbusauthz.add_user.main(["-n", "-a", aname, uu])
        self.assertEqual(rc, 0, "CLI should return success")

        #  add a few alias
        for i in range(0, 10):
            aname = str(uuid.uuid1())
            rc = pynimbusauthz.add_user.main(["-a", aname, uu])
            self.assertEqual(rc, 0, "CLI should return success")

        rc = pynimbusauthz.add_user.main(["-f", "-r", uu])
        self.assertEqual(rc, 0, "CLI should return success")
    
