import sqlite3
import os
import sys
import pynimbusauthz
from pynimbusauthz.db import DB
from pynimbusauthz.user import User
from pynimbusauthz.user import UserAlias
from pynimbusauthz.objects import File
from pynimbusauthz.objects import UserFile
import pynimbusauthz.chmod
import unittest
import uuid
import tempfile


class TestChmodCli(unittest.TestCase):

    def setUp(self):
        (osf, self.fname) = tempfile.mkstemp()
        os.close(osf)
#        os.environ['CUMULUS_AUTHZ_DDL'] = "/home/bresnaha/Dev/Nimbus/nimbus/cumulus/authz/etc/acl.sql"
        os.environ['NIMBUS_AUTHZ_DB'] = self.fname
        pynimbusauthz.db.make_test_database(self.fname)
        self.db = DB(con_str=self.fname)
        self.user1 = User(self.db)
        self.name = "/file/name"
        self.data = "/etc/group"
        self.file1 = File.create_file(self.db, self.name, self.user1, self.data, pynimbusauthz.object_type_s3)
        self.uf = UserFile(self.file1)
        self.db.commit()

    def tearDown(self):
        self.db.close()
        os.remove(self.fname)

    def validate_perms(self, new):
        f = File.get_file_from_db_id(self.db, self.file1.get_id())
        uf = UserFile(f, self.user1)
        perms = uf.get_perms(force=True)
        for p in new:
            self.assertTrue(p in perms, "bad perms set %s != %s" % (new, perms))
        self.assertEqual(len(perms), len(new), "perms dont match %s != %s" % (new, perms))

    def test_basic_chmod(self):
        uu = str(uuid.uuid1())
        new_perms = "WR"
        rc = pynimbusauthz.chmod.main([self.user1.get_id(), self.file1.get_name(), new_perms])
        self.assertEqual(rc, 0, "CLI should return success %d" % (rc))
        self.validate_perms(new_perms)

    def test_type_chmod(self):
        uu = str(uuid.uuid1())
        new_perms = "WRr"
        rc = pynimbusauthz.chmod.main(["-t", self.file1.get_object_type(), self.user1.get_id(), self.file1.get_name(), new_perms])
        self.assertEqual(rc, 0, "CLI should return success %d" % (rc))
        self.validate_perms(new_perms)

    def test_badopts(self):
        new_perms = "WR"

        rc = pynimbusauthz.chmod.main(["-t", self.file1.get_object_type(), self.user1.get_id()])
        self.assertEqual(rc, 32, "CLI should return success %d" % (rc))

        rc = pynimbusauthz.chmod.main([self.user1.get_id(), "notafile", new_perms])
        self.assertEqual(rc, 33, "CLI should return success %d" % (rc))
        rc = pynimbusauthz.chmod.main(["-t", self.file1.get_object_type(), "-p", "nobucket", self.user1.get_id(), self.file1.get_name(), new_perms])
        self.assertEqual(rc, 33, "CLI should return success %d" % (rc))

    def test_bucket(self):
        # create a file and a bucket
        b1 = File.create_file(self.db, "bucket", self.user1, self.data, pynimbusauthz.object_type_s3)
        f2 = File.create_file(self.db, self.name, self.user1, self.data, pynimbusauthz.object_type_s3, parent=b1)
        self.db.commit()

        new_perms = "WR"
        rc = pynimbusauthz.chmod.main(["-t", f2.get_object_type(), "-p", b1.get_name(), self.user1.get_id(), f2.get_name(), new_perms])
        self.assertEqual(rc, 0, "CLI should return success %d" % (rc))

