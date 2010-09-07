import sqlite3
import os
import sys
import pynimbusauthz
from pynimbusauthz.db import DB
from pynimbusauthz.user import User
from pynimbusauthz.user import UserAlias
import unittest
import uuid

class TestCommit(unittest.TestCase):

    def setUp(self):
        con = pynimbusauthz.db.make_test_database()
        self.db = DB(con=con)

    def tearDown(self):
        self.db.close()

    def test_basic(self):
        id = uuid.uuid1()
        user1 = User(self.db, id, create=True)
        self.db.rollback()
        user1 = User(self.db, id, create=True)

