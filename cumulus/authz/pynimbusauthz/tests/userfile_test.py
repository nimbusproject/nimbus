import sqlite3
import os
import sys
import pynimbusauthz
from pynimbusauthz.db import DB
from pynimbusauthz.user import User
from pynimbusauthz.user import UserAlias
from pynimbusauthz.objects import File
from pynimbusauthz.objects import UserFile
import unittest

class TestUser(unittest.TestCase):

    def setUp(self):
#        os.environ['CUMULUS_AUTHZ_DDL'] = "/home/bresnaha/Dev/Nimbus/nimbus/cumulus/authz/etc/acl.sql"
        con = pynimbusauthz.db.make_test_database()
        self.db = DB(con=con)
        self.user1 = User(self.db)
        self.name = "/file/name"
        self.data = "/etc/group"
        self.file1 = File.create_file(self.db, self.name, self.user1, self.data, pynimbusauthz.object_type_s3)
        self.uf = UserFile(self.file1)

    def tearDown(self):
        self.db.close()

    def test_basic_userfile(self):
        perms = self.uf.get_perms()
        self.assertEqual(len(perms), 4, "Default perms should be none")
        self.assertTrue("r" in perms, "read not set")
        self.assertTrue("w" in perms, "write not set")
        self.assertTrue("R" in perms, "read acl not set")
        self.assertTrue("W" in perms, "write acl not set")

        f2 = self.uf.get_file()
        self.assertEqual(f2, self.file1, "should return the same file")

        o2 = self.uf.get_owner()
        self.assertEqual(o2, self.user1, "should return the same user")

        self.assertTrue(self.uf.can_access("rwRW"))

        a = self.uf.get_all_children()
        self.assertEqual(len(list(a)), 0, "should be no children")

    def test_bad_chmod(self):
        try:
            self.uf.chmod("KSA")
            self.fail("should be a bad parameter exception")
        except:
            pass

    def test_bad_chmod(self):
        self.uf.chmod("r")
        try:
            perms = self.uf.get_perms()
            self.fail("should not be able to read acl")
        except:
            pass
        self.uf.chmod("Rr")
        perms = self.uf.get_perms()
        rc = self.uf.can_access("w")
        self.assertFalse(rc, "should not be able to write %d"  % (rc))
        rc = self.uf.can_access("r")
        self.assertTrue(rc, "should be able to read")
        self.uf.chmod("RW")


    def test_grant(self):
        user2 = User(self.db)
        self.uf.chmod("R", user=user2)

        uf2 = UserFile(self.file1, user2)

        p = uf2.get_perms()
        self.assertEqual(p, "R", "perms should only be read here")
       
    def test_children(self):
        child1 = File.create_file(self.db, "kid", self.user1, self.data, pynimbusauthz.object_type_s3, parent=self.file1)
        self.db.commit()

        x = child1.get_all_children()
        self.assertEqual(len(list(x)), 0, "The file should have no children")

        x = self.uf.get_all_children()
        found = False
        for f in x:
            if f.get_file() == child1:
                found = True
        self.assertTrue(found, "We should have found that kid!")

