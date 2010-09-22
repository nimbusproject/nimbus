import sqlite3
import os
import sys
import pynimbusauthz
from pynimbusauthz.db import DB
from pynimbusauthz.user import User
from pynimbusauthz.user import UserAlias
from pynimbusauthz.objects import File
from pynimbusauthz.objects import UserFile
import pynimbusauthz.stat
import pynimbusauthz.chmod
import unittest
import uuid
import tempfile


class TestStatCli(unittest.TestCase):

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

    def test_basic_stat(self):
        rc = pynimbusauthz.stat.main([self.name])
        self.assertEqual(rc, 0, "CLI should return success %d" % (rc))
        rc = pynimbusauthz.stat.main(["-a", self.name])
        self.assertEqual(rc, 0, "CLI should return success %d" % (rc))

    def test_bad_opts_stat(self):
        rc = pynimbusauthz.stat.main([])
        self.assertEqual(rc, 32, "CLI should return success %d" % (rc))
        rc = pynimbusauthz.stat.main(["nofile"])
        self.assertNotEqual(rc, 0, "CLI should return success %s" % (str(rc)))
        rc = pynimbusauthz.stat.main(["-p", "nobucket", self.name])
        self.assertNotEqual(rc, 0, "CLI should return success %d" % (rc))


    def test_basic_stat(self):
        user2 = User(self.db)
        self.db.commit()

        rc = pynimbusauthz.chmod.main([user2.get_id(), self.name, "Rrw"])
        self.assertEqual(rc, 0, "CLI should return success %d" % (rc))
        rc = pynimbusauthz.stat.main([self.name])
        self.assertEqual(rc, 0, "CLI should return success %d" % (rc))
        rc = pynimbusauthz.stat.main(["-a", self.name])
        self.assertEqual(rc, 0, "CLI should return success %d" % (rc))

