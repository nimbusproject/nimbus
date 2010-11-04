import sqlite3
import os
import sys
import pynimbusauthz
from pynimbusauthz.db import DB
from pynimbusauthz.user import User
from pynimbusauthz.user import UserAlias
from pynimbusauthz.objects import File
from pynimbusauthz.objects import UserFile
import pynimbusauthz.rebase
import unittest
import uuid
import tempfile


class TestRebaseCli(unittest.TestCase):

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

    def test_single_change(self): 
        name = "/file/name"
        old_base = "/OLD"
        new_base = "/NEW"
        data = "/etc/group"

        key = old_base + data
        file1 = File.create_file(self.db, name, self.user1, key, pynimbusauthz.object_type_s3)
        self.db.commit()

        rc = pynimbusauthz.rebase.main([old_base, new_base])
        self.assertEqual(rc, 0, "rc should be 0, is %d" % (rc))

        f2a = File.find_files_from_data(self.db, key)
        f2a = list(f2a)
        self.assertEqual(len(f2a), 0, "should be no values with key %s len is %d" % (old_base, len(f2a)))
        key = new_base + data
        f2a = File.find_files_from_data(self.db, key)
        f2a = list(f2a)
        self.assertNotEqual(len(f2a), 0, "length should be greater than 0 is %d" % (len(f2a)))

        found = False
        for f2 in f2a:
            tst_key = f2.get_data_key()
            if tst_key == key:
                found = True
        self.assertTrue(found, "key not found")
       

    def test_many_change(self):
        name = "/file/name"
        old_base = "/OLD"
        new_base = "/NEW"
        count = 10

        for i in range(0, count): 
            keyname = str(uuid.uuid1())
            oldkey = old_base + "/" + keyname
            File.create_file(self.db, name+oldkey, self.user1, oldkey, pynimbusauthz.object_type_s3)
        self.db.commit()

        rc = pynimbusauthz.rebase.main([old_base, new_base])
        self.assertEqual(rc, 0, "rc should be 0, is %d" % (rc))

        f2a = File.find_files_from_data(self.db, new_base + "%")
        f2a = list(f2a)
        self.assertEqual(len(f2a), count, "length of the new items should be %d is %s" % (count, len(f2a)))


    def test_many_change_but_not_all(self):
        name = "/file/name"
        old_base = "/OLD"
        new_base = "/NEW"
        other_base = "/NOTHERE"
        count = 10

        for i in range(0, count):
            keyname = str(uuid.uuid1())
            oldkey = old_base + "/" + keyname
            File.create_file(self.db, name+oldkey, self.user1, oldkey, pynimbusauthz.object_type_s3)
        for i in range(0, count*2):
            keyname = str(uuid.uuid1())
            oldkey = other_base + "/" + keyname
            File.create_file(self.db, name+oldkey, self.user1, oldkey, pynimbusauthz.object_type_s3)
        self.db.commit()

        rc = pynimbusauthz.rebase.main([old_base, new_base])
        self.assertEqual(rc, 0, "rc should be 0, is %d" % (rc))

        f2a = File.find_files_from_data(self.db, new_base + "%")
        f2a = list(f2a)
        self.assertEqual(len(f2a), count, "length of the new items should be %d is %s" % (count, len(f2a)))




