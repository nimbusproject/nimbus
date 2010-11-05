import sqlite3
import os
import sys
import pynimbusauthz
from pynimbusauthz.db import DB
from pynimbusauthz.user import User
from pynimbusauthz.user import UserAlias
from pynimbusauthz.objects import File
from pynimbusauthz.objects import UserFile
import pynimbusauthz.ls
import unittest
import uuid
import tempfile


class TestLsCli(unittest.TestCase):

    def setUp(self):
        (osf, self.fname) = tempfile.mkstemp()
        os.close(osf)
# os.environ['CUMULUS_AUTHZ_DDL'] = "/home/bresnaha/Dev/Nimbus/nimbus/cumulus/authz/etc/acl.sql"
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

    def test_basic_ls(self):
        rc = pynimbusauthz.ls.main([])
        self.assertEqual(rc, 0, "CLI should return success %d" % (rc))
        rc = pynimbusauthz.ls.main(["-t", pynimbusauthz.object_type_s3])
        self.assertEqual(rc, 0, "CLI should return success %d" % (rc))
        rc = pynimbusauthz.ls.main([self.name[0:-1]])
        self.assertEqual(rc, 0, "CLI should return success %d" % (rc))


    def test_fail_ls(self):
        rc = pynimbusauthz.ls.main(["-p", "nobucket", self.name[0:-1]])
        self.assertNotEqual(rc, 0, "CLI should return success %d" % (rc))



