import sqlite3
import os
import sys
import pynimbusauthz
from pynimbusauthz.db import DB
from pynimbusauthz.user import User
from pynimbusauthz.user import UserAlias
import unittest

class TestUser(unittest.TestCase):

    def setUp(self):
        #os.environ['CUMULUS_AUTHZ_DDL'] = "/home/bresnaha/Dev/Nimbus/nimbus/cumulus/authz/etc/acl.sql"
        con = pynimbusauthz.db.make_test_database()
        self.db = DB(con=con)

    def tearDown(self):
        self.db.close()

    def test_basic_user(self):
        user1 = User(self.db)
        user2 = User(self.db, user1.get_id())

        self.assertEqual(user1.get_id(), user2.get_id(), "User IDs should be equal")

    def test_user_to_string(self):
        user1 = User(self.db)
        uu = user1.get_id()
        self.assertEqual(str(user1), uu, "toString function not working for user")

    def test_destroy_user(self):
        user1 = User(self.db)
        uu = user1.get_id()
        user1.detroy_brutaly()

        try:
            user1 = User(self.db, uu)
            self.fail("The uuid should have been deleted %s" % (uu))
        except:
            pass

    def test_null_alias(self):
        user1 = User(self.db)
        all_alias = user1.get_all_alias()
        self.assertEqual(len(list(all_alias)), 0, "No alias should be in DB for new user")
        all_alias = user1.get_alias_by_type(pynimbusauthz.alias_type_s3)
        self.assertEqual(len(list(all_alias)), 0, "No alias should be in DB for new user")


    def test_create_alias_simple(self):
        user1 = User(self.db)
        alias1 = user1.create_alias("/name/", pynimbusauthz.alias_type_s3)

        user2 = alias1.get_canonical_user()
        self.assertEqual(user1.get_id(), user2.get_id(), "User IDs should be equal")
        alias1 = user1.create_alias("/name2", pynimbusauthz.alias_type_s3, "pooP")

        user2 = alias1.get_canonical_user()
        self.assertEqual(user1.get_id(), user2.get_id(), "User IDs should be equal")
        print alias1
        
    def test_alias_lookup_simple(self):
        user1 = User(self.db)
        alias1 = user1.create_alias("/name/", pynimbusauthz.alias_type_s3)
        alias2 = user1.get_alias("/name/", pynimbusauthz.alias_type_s3)

        self.assertEqual(alias1.get_name(), alias2.get_name(), "Alias names should be the same")
        self.assertEqual(alias1.get_type(), alias2.get_type(), "Alias types should be the same")
        self.assertEqual(alias1.get_data(), alias2.get_data(), "Alias data should be the same")

    def test_alias_lookup(self):
        user1 = User(self.db)
        alias1 = user1.create_alias("/name/", pynimbusauthz.alias_type_s3)
        aliasX = user1.create_alias("/name2", pynimbusauthz.alias_type_s3)
        aliasX = user1.create_alias("/name3", pynimbusauthz.alias_type_s3)
        aliasX = user1.create_alias("/name4", pynimbusauthz.alias_type_s3)
        all_alias = user1.get_all_alias()
        found = False
        for a in all_alias:
            if a.get_name() == alias1.get_name() and a.get_type() == alias1.get_type():
                found = True
        self.assertTrue(found, "We should have found the alias")

        all_alias = user1.get_alias_by_type(pynimbusauthz.alias_type_s3)
        found = False
        for a in all_alias:
            if a.get_name() == alias1.get_name() and a.get_type() == alias1.get_type():
                found = True
        self.assertTrue(found, "We should have found the alias")

    def test_delete_alias(self):
        user1 = User(self.db)
        alias1 = user1.create_alias("/name/", pynimbusauthz.alias_type_s3)
        alias1.remove()
        alias2 = user1.get_alias("/name/", pynimbusauthz.alias_type_s3)
        self.assertEqual(alias2, None, "The alias should be gone")

    def test_create_same_alias(self):
        user1 = User(self.db)
        alias1 = user1.create_alias("/name/", pynimbusauthz.alias_type_s3)

        try:
            alias2 = user1.create_alias("/name/", pynimbusauthz.alias_type_s3)
            self.fail("This should have caused an conflict on insert")
        except:
            pass

    def find_user_id(self, u, list):
        for l in list:
            if u == l:
                return True
        return False

    def test_find_user(self):
        user1 = User(self.db)
        self.db.commit()
        id = user1.get_id()
        fid = id[1:]
        lid = id[:-1]
        mid = id[1:-1]

        # find by exact id
        u_all = User.find_user(self.db, id)
        self.assertNotEqual(u_all, None, "we should have found somethings")
        self.assertTrue(self.find_user_id(user1, u_all))
        # find by exact partial 1
        u_all = User.find_user(self.db, fid)
        self.assertTrue(self.find_user_id(user1, u_all))
        # find by exact partial 1
        u_all = User.find_user(self.db, lid)
        self.assertTrue(self.find_user_id(user1, u_all))
        # find by exact partial 1
        u_all = User.find_user(self.db, mid)
        self.assertNotEqual(u_all, None, "we should have found somethings")
        self.assertTrue(self.find_user_id(user1, u_all))

    def test_find_user_alias(self):
        user1 = User(self.db)
        self.db.commit()
        alias_name = "helloname"
        alias1 = user1.create_alias(alias_name, pynimbusauthz.alias_type_s3)
        self.db.commit()
        fid = "%%" + alias_name[1:]
        lid = alias_name[:-1] + "%%"
        mid = "%%" + alias_name[1:-1] + "%%"

        # find by exact id
        u_all = UserAlias.find_alias(self.db, alias_name)
        self.assertNotEqual(u_all, None, "we should have found somethings")
        self.assertTrue(self.find_user_id(alias1, u_all))
        # find by exact partial 1
        u_all = UserAlias.find_alias(self.db, fid)
        self.assertTrue(self.find_user_id(alias1, u_all))
        # find by exact partial 1
        u_all = UserAlias.find_alias(self.db, lid)
        self.assertTrue(self.find_user_id(alias1, u_all))
        # find by exact partial 1
        u_all = UserAlias.find_alias(self.db, mid)
        self.assertNotEqual(u_all, None, "we should have found somethings")
        self.assertTrue(self.find_user_id(alias1, u_all))


    def test_set_alias_data(self):
        user1 = User(self.db)
        self.db.commit()
        alias_name = "helloname"
        alias1 = user1.create_alias(alias_name, pynimbusauthz.alias_type_s3)
        key = "helloworld"
        alias1.set_data(key)
        self.db.commit()

        alias2 = user1.get_alias(alias_name, pynimbusauthz.alias_type_s3)
        self.assertEqual(alias1.get_data(), alias2.get_data(), "alias not equal")
        self.assertEqual(key, alias2.get_data(), "alias not equal")

