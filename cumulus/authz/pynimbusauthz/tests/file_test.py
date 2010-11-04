import sqlite3
import os
import sys
import pynimbusauthz
from pynimbusauthz.db import DB
from pynimbusauthz.user import User
from pynimbusauthz.user import UserAlias
from pynimbusauthz.objects import File
import unittest

class TestFile(unittest.TestCase):

    def setUp(self):
#        os.environ['CUMULUS_AUTHZ_DDL'] = "/home/bresnaha/Dev/Nimbus/nimbus/cumulus/authz/etc/acl.sql"
        con = pynimbusauthz.db.make_test_database()
        self.db = DB(con=con)

    def tearDown(self):
        self.db.close()

    def test_basic_file(self):
        user1 = User(self.db)
        name = "/file/name"
        data = "/etc/group"
        file1 = File.create_file(self.db, name, user1, data, pynimbusauthz.object_type_s3)
        self.db.commit()
        x = file1.get_all_children()
        self.assertEqual(len(list(x)), 0, "The file should have no children")
        n2 = file1.get_name()
        self.assertEqual(name, n2, "Names not equal")
        d2 = file1.get_data_key()
        self.assertEqual(data, d2, "Data not equal")
        o2 = file1.get_owner()
        self.assertEqual(user1, o2, "Owner not equal")
        p2 = file1.get_parent()
        self.assertEqual(None, p2, "There should be no parent")
        b2 = file1.get_object_type()
        self.assertEqual(pynimbusauthz.object_type_s3, b2, "Type wrong")

    def test_file_children(self):
        user1 = User(self.db)
        name = "/file/name"
        data = "/etc/group"
        file1 = File.create_file(self.db, name, user1, data, pynimbusauthz.object_type_s3)
        self.db.commit()
                
        child1 = File.create_file(self.db, "kid", user1, data, pynimbusauthz.object_type_s3, parent=file1)
        self.db.commit()

        p2 = child1.get_parent()
        self.assertEqual(p2, file1, "parent not set properly")

        x = child1.get_all_children()
        self.assertEqual(len(list(x)), 0, "The file should have no children")

        x = file1.get_all_children()
        found = False
        for f in x:
            if f == child1:
                found = True

        self.assertTrue(found, "We should have found that kid!")

    def test_find_no_file(self):
        f = File.get_file_from_db_id(self.db, 1000)
        self.assertEqual(f, None, "We should not have found that file")
        f = File.get_file(self.db, "nofile", pynimbusauthz.object_type_s3)
        self.assertEqual(f, None, "We should not have found that file")

    def test_file_and_bucket(self):
        user1 = User(self.db)
        fname = "NAME"
        data = "data"
        b1 = File.create_file(self.db, "bucket", user1, data, pynimbusauthz.object_type_s3)
        f1 = File.create_file(self.db, fname, user1, data, pynimbusauthz.object_type_s3, parent=b1)
        f2 = File.create_file(self.db, fname, user1, data, pynimbusauthz.object_type_s3)
        self.db.commit()

        self.assertNotEqual(f1.get_id(), f2.get_id())
        
        f3 = File.get_file(self.db, fname, pynimbusauthz.object_type_s3, parent=b1)
        f4 = File.get_file(self.db, fname, pynimbusauthz.object_type_s3)
        self.assertEqual(f1.get_id(), f3.get_id())
        self.assertEqual(f2.get_id(), f4.get_id())
        self.assertNotEqual(f3.get_id(), f4.get_id())
        self.db.commit()

    def test_change_key(self):
        user1 = User(self.db)
        name = "/file/name"
        old_base = "/old/path/base"
        fname = "/etc/group"
        new_base = "/new/base/location/dir"
        f = File.create_file(self.db, name, user1, old_base + fname, pynimbusauthz.object_type_s3)

        self.assertEqual(old_base + fname, f.get_data_key(), "old value not euqal")

        new_key = new_base + fname
        f.set_data_key(new_key)
        self.db.commit()

        tst_new_key = f.get_data_key()
        self.assertEqual(tst_new_key, new_key, "%s should equal %s" % (tst_new_key, new_key))

        f2 = File.get_file(self.db, name, pynimbusauthz.object_type_s3)

        tst_new_key = f2.get_data_key()
        self.assertEqual(tst_new_key, new_key, "%s should equal %s" % (tst_new_key, new_key))


    def test_find_by_key(self):
        user1 = User(self.db)
        name = "/file/name"
        key = "/old/path/base"
        f = File.create_file(self.db, name, user1, key, pynimbusauthz.object_type_s3)
        self.db.commit()

        f2a = File.find_files_from_data(self.db, key)

        found = False
        for f2 in f2a:
            tst_key = f2.get_data_key()
            if tst_key == key:
                found = True
        self.assertTrue(found, "key not found")

