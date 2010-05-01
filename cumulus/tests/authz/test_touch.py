import sqlite3
import os
import sys
import pynimbusauthz
from pynimbusauthz.db import DB
from pynimbusauthz.user import User
from pynimbusauthz.user import UserAlias
from pynimbusauthz.objects import File
from pynimbusauthz.objects import UserFile
import pynimbusauthz.touch
import unittest
import uuid
import tempfile


class TestTouchCli(unittest.TestCase):

    def setUp(self):
        (osf, self.fname) = tempfile.mkstemp()
        os.close(osf)
#        os.environ['CUMULUS_AUTHZ_DDL'] = "/home/bresnaha/Dev/Nimbus/nimbus/cumulus/authz/etc/acl.sql"
        os.environ['NIMBUS_AUTHZ_DB'] = self.fname
        pynimbusauthz.db.make_test_database(self.fname)
        self.db = DB(con_str=self.fname)
        self.user1 = User(self.db)
        self.db.commit()

    def tearDown(self):
        self.db.close()
        os.remove(self.fname)

    def test_basic_touch(self):
        fname = str(uuid.uuid1())
        data = str(uuid.uuid1())
        f = File.get_file(self.db, fname, pynimbusauthz.object_type_s3)
        self.assertEqual(f, None)
        rc = pynimbusauthz.touch.main([self.user1.get_id(), fname, data])
        self.assertEqual(rc, 0, "CLI should return success %d" % (rc))
        f = File.get_file(self.db, fname, pynimbusauthz.object_type_s3)
        self.assertNotEqual(f, None)

    def test_bucket_touch(self):
        fname = str(uuid.uuid1())
        data = str(uuid.uuid1())
        rc = pynimbusauthz.touch.main(["-t", pynimbusauthz.object_type_s3, self.user1.get_id(), fname, data])
        self.assertEqual(rc, 0, "CLI should return success %d" % (rc))

        f = File.get_file(self.db, fname, pynimbusauthz.object_type_s3)
        self.assertNotEqual(f, None)

    def test_under_bucket_touch(self):
        bname = str(uuid.uuid1())
        fname = str(uuid.uuid1())
        data = str(uuid.uuid1())
        rc = pynimbusauthz.touch.main(["-t", pynimbusauthz.object_type_s3, self.user1.get_id(), bname, data])
        self.assertEqual(rc, 0, "CLI should return success %d" % (rc))
        rc = pynimbusauthz.touch.main(["-p", bname, self.user1.get_id(), fname, data])
        self.assertEqual(rc, 0, "CLI should return success %d" % (rc))

        b1 = File.get_file(self.db, bname, pynimbusauthz.object_type_s3)
        f1 = File.get_file(self.db, fname, pynimbusauthz.object_type_s3, parent=b1)

        self.assertNotEqual(b1, None)
        self.assertNotEqual(f1, None)


    def test_bad_opts(self):
        bname = str(uuid.uuid1())
        fname = str(uuid.uuid1())
        data = str(uuid.uuid1())
        rc = pynimbusauthz.touch.main([bname, data])
        self.assertEqual(rc, 32, "CLI should return failure %d" % (rc))
        rc = pynimbusauthz.touch.main(["-p", bname, self.user1.get_id(), fname, data])
        self.assertEqual(rc, 33, "CLI should return failure %d" % (rc))

