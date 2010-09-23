import sqlite3
import os
import sys
import pynimbusauthz
from pynimbusauthz.db import DB
from pynimbusauthz.user import User
from pynimbusauthz.user import UserAlias
from pynimbusauthz.objects import File
import unittest

class TestUser(unittest.TestCase):

    def setUp(self):
#        os.environ['CUMULUS_AUTHZ_DDL'] = "/home/bresnaha/Dev/Nimbus/nimbus/cumulus/authz/etc/acl.sql"
        con = pynimbusauthz.db.make_test_database()
        self.db = DB(con=con)
        self.user = User(self.db)

    def tearDown(self):
        self.user.destroy_brutally()
        self.db.close()

    def test_get_no_quota(self):
        q = self.user.get_quota()
        self.assertEqual(q, User.UNLIMITED)

    def test_set_quota(self):
        q = 100
        self.user.set_quota(q)
        qrc = self.user.get_quota()
        self.assertEqual(q, qrc)

    def test_get_no_usage(self):
        u = self.user.get_quota_usage()
        self.assertEqual(u, 0)

    def test_add_file_usage_one_file(self):
        size1 = 100
        name = "/file/name"
        data = "/etc/group"

        file1 = File.create_file(self.db, name, self.user, data, pynimbusauthz.object_type_s3, size=size1)
        self.db.commit()

        u = self.user.get_quota_usage()
        self.assertEqual(u, size1)

    def test_add_file_usage_many_files(self):
        size1 = 100
        name = "/file/name"
        data = "/etc/group"

        total = 0
        for i in range(0, 10):
            file1 = File.create_file(self.db, name+str(i), self.user, data, pynimbusauthz.object_type_s3, size=size1)
            total = total + size1
        self.db.commit()

        u = self.user.get_quota_usage()
        self.assertEqual(u, total)


