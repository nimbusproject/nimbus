import string
import random
import os
import sys
import nose.tools
import boto
from boto.s3.connection import OrdinaryCallingFormat
from boto.s3.connection import VHostCallingFormat
from boto.s3.connection import SubdomainCallingFormat
import sys
from ConfigParser import SafeConfigParser
from pycb.cumulus import *
import time
import pycb.test_common
import unittest
import tempfile
import filecmp
#
class TestCopyObjectAuthWithBoto(unittest.TestCase):

    def setUp(self):
        (self.host, self.port) = pycb.test_common.get_contact()
        self.user_id_list = []

    def tearDown(self):
        for (id, pw) in self.user_id_list:
            self.clean_all(id, pw)
            pycb.test_common.clean_user(id)
        pass

    def make_user(self):
        # note this only works if access to the same FS
        (id, pw) = pycb.test_common.make_user()
        self.user_id_list.append((id, pw))
        return (id, pw)

    def clean_all(self, id, pw):
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        nbs = conn.get_all_buckets()
        for b in nbs:
            rs = b.list()
            for key in rs:
                try:
                    key.delete()
                except:
                    pass
            try:
                b.delete()
            except:
                pass

    def cb_random_bucketname(self, len):
        chars = string.letters.lower() + string.digits
        newpasswd = ""
        for i in range(len):
            newpasswd = newpasswd + random.choice(chars)
        return newpasswd

    def test_get_and_compare_copy(self):
        (id, pw) = self.make_user()
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucketname = self.cb_random_bucketname(20)
        bucket = conn.create_bucket(bucketname, policy='public-read')
        key = self.cb_random_bucketname(20)
        k = boto.s3.key.Key(bucket)
        k.key = key
        k.set_contents_from_filename("/etc/group")

        (osf, filename) = tempfile.mkstemp()
        os.close(osf)
        k.get_contents_to_filename(filename)

        rc = filecmp.cmp("/etc/group", filename)
        self.assertTrue(rc)

    def cp_object_perm_read(self, type, delete=False):
        (id, pw) = self.make_user()
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucketname = self.cb_random_bucketname(20)
        bucket = conn.create_bucket(bucketname, policy='public-read')
        key = self.cb_random_bucketname(20)
        k = boto.s3.key.Key(bucket)
        k.key = key
        k.set_contents_from_filename("/etc/group", policy='private')

        new_key = self.cb_random_bucketname(20)
        new_k = boto.s3.key.Key(bucket)
        new_k.key = new_key

        k.copy(bucket, new_key)

        (id, pw) = self.make_user()
        for t in type:
            new_k.add_user_grant(t, id)

        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn.get_bucket(bucketname)
        k = bucket2.get_key(new_key)
        k.get_contents_to_filename("/dev/null")

        if delete:
            k.delete()


    def test_cp_object_perm_none(self):
        passed = True
        try:
            self.cp_object_perm_read([])
            passed = False
        except:
            pass
        self.assertTrue(passed)

    def test_cp_object_perm_read(self):
        self.cp_object_perm_read(["READ",])

    def test_cp_object_perm_read_write(self):
        self.cp_object_perm_read(["READ","WRITE",])

    def test_cp_object_perm_write(self):
        passed = True
        try:
            self.cp_object_perm_read(["WRITE",])
            passed = False
        except:
            pass
        self.assertTrue(passed)

    def test_cp_object_perm_full(self):
        self.cp_object_perm_read(["FULL_CONTROL",])

    def test_cp_object_perm_read_write_delete(self, delete=True):
        self.cp_object_perm_read(["READ","WRITE",])

    def test_cp_object_perm_full_delete(self, delete=True):
        self.cp_object_perm_read(["FULL_CONTROL",])

    def test_cp_object_perm_read_delete(self, delete=True):
        passed = True
        try:
            self.cp_object_perm_read(["READ",], delete=True)
            passed = False
        except:
            pass
        self.assertTrue(passed)

