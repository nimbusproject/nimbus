import sqlite3
import os
import sys
import pynimbusauthz
from pynimbusauthz.db import DB
from pynimbusauthz.user import User
from pynimbusauthz.user import UserAlias
import pynimbusauthz.add_user 
import pynimbusauthz.list_user
import unittest
import uuid
import tempfile


class TestListUserCli(unittest.TestCase):

    def setUp(self):
        (osf, self.fname) = tempfile.mkstemp()
        os.close(osf)
#        os.environ['CUMULUS_AUTHZ_DDL'] = "/home/bresnaha/Dev/Nimbus/nimbus/cumulus/authz/etc/acl.sql"
        os.environ['NIMBUS_AUTHZ_DB'] = self.fname
        pynimbusauthz.db.make_test_database(self.fname)
        self.db = DB(con_str=self.fname)
        (tmpFD, self.outFileName) = tempfile.mkstemp("koatests")
        os.close(tmpFD)

    def tearDown(self):
        self.db.close()
        os.remove(self.fname)

    def find_out_string(self, s):
        f = open(self.outFileName)
        lines = f.readlines()
        f.close()

        for l in lines:
            ndx = l.find(s)
            if ndx >= 0:
                return True
        return False

    def test_basic_list_user(self):
        uu = str(uuid.uuid1())
        user = User(self.db, uu, create=True)
        self.db.commit()
        rc = pynimbusauthz.list_user.main(["-O", self.outFileName])
        self.assertEqual(rc, 0, "CLI should return success")
        rc = self.find_out_string(uu)
        self.assertTrue(rc, "Username not found")

    def test_list_alias(self):
        uu = str(uuid.uuid1())
        user = User(self.db, uu, create=True)
        #  add a few alias
        alias_a = []
        for i in range(0, 10):
            aname = str(uuid.uuid1())
            alias_a.append(aname)
            rc = pynimbusauthz.add_user.main(["-a", aname, uu])
            self.assertEqual(rc, 0, "CLI should return success")

        self.db.commit()
        rc = pynimbusauthz.list_user.main(["-a", "-O", self.outFileName])
        self.assertEqual(rc, 0, "CLI should return success")
        rc = self.find_out_string(uu)
        self.assertTrue(rc, "Username not found")
        for a in alias_a:
            rc = self.find_out_string(a)
            self.assertTrue(rc, "Username not found")

    def test_find_by_alias(self):
        uu = str(uuid.uuid1())
        user = User(self.db, uu, create=True)
        #  add a few alias
        alias_a = []
        for i in range(0, 10):
            aname = str(uuid.uuid1())
            alias_a.append(aname)
            rc = pynimbusauthz.add_user.main(["-a", aname, uu])
            self.assertEqual(rc, 0, "CLI should return success")

        self.db.commit()
        rc = pynimbusauthz.list_user.main(["-s", "-O", self.outFileName, alias_a[0]])
        self.assertEqual(rc, 0, "CLI should return success")
        rc = self.find_out_string(uu)
        self.assertTrue(rc, "Username not found")
