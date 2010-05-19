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
class TestPubAuthWithBoto(unittest.TestCase):

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
        chars = string.letters + string.digits
        newpasswd = ""
        for i in range(len):
            newpasswd = newpasswd + random.choice(chars)
        return newpasswd

    def bucket_perm_read(self, type):
        (id, pw) = self.make_user()
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucketname = self.cb_random_bucketname(20)
        bucket = conn.create_bucket(bucketname, policy=type)
        (id, pw) = self.make_user()
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket = conn.get_bucket(bucketname)

    def test_bucket_perm_authed(self):
        self.bucket_perm_read('authenticated-read')

    def test_bucket_perm_public(self):
        self.bucket_perm_read('public-read')

    def test_bucket_perm_public_read_write(self):
        self.bucket_perm_read('public-read-write')

    def test_bucket_perm_private(self):
        try:
            self.bucket_perm_read('private')
            passed = False
        except:
            passed = True
        self.assertTrue(passed)

    def bucket_perm_read_after(self, type):
        (id, pw) = self.make_user()
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucketname = self.cb_random_bucketname(20)
        bucket = conn.create_bucket(bucketname)
        bucket.set_acl(type)
        (id, pw) = self.make_user()
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket = conn.get_bucket(bucketname)

    def test_bucket_perm_authed_after(self):
        self.bucket_perm_read_after('authenticated-read')

    def test_bucket_perm_public_after(self):
        self.bucket_perm_read_after('public-read')

    def test_bucket_perm_public_read_write_after(self):
        self.bucket_perm_read_after('public-read-write')

    def test_bucket_perm_private_after(self):
        try:
            self.bucket_perm_read_after('private')
            passed = False
        except:
            passed = True
        self.assertTrue(passed)

    def object_perm_read(self, type):
        (id, pw) = self.make_user()
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucketname = self.cb_random_bucketname(20)
        bucket = conn.create_bucket(bucketname, policy='public-read')
        key = self.cb_random_bucketname(20)
        k = boto.s3.key.Key(bucket)
        k.key = key
        k.set_contents_from_filename("/etc/group", policy=type)

        (id, pw) = self.make_user()
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn.get_bucket(bucketname)
        k = bucket2.get_key(key)
        k.get_contents_to_filename("/dev/null")


    def test_object_perm_authed(self):
        self.object_perm_read('authenticated-read')

    def test_object_perm_public(self):
        self.object_perm_read('public-read')

    def test_object_perm_public_read_write(self):
        self.object_perm_read('public-read-write')

    def test_object_perm_private(self):
        try:
            self.object_perm_read('private')
            passed = False
        except:
            passed = True
        self.assertTrue(passed)

    def object_perm_read_after(self, type):
        (id, pw) = self.make_user()
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucketname = self.cb_random_bucketname(20)
        bucket = conn.create_bucket(bucketname, policy='public-read')
        key = self.cb_random_bucketname(20)
        k = boto.s3.key.Key(bucket)
        k.key = key
        k.set_contents_from_filename("/etc/group")
        bucket.set_acl(type, key)

        (id, pw) = self.make_user()
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn.get_bucket(bucketname)
        k = bucket2.get_key(key)
        k.get_contents_to_filename("/dev/null")


    def test_object_perm_authed_after(self):
        self.object_perm_read_after('authenticated-read')

    def test_object_perm_public_after(self):
        self.object_perm_read_after('public-read')

    def test_object_perm_public_read_write_after(self):
        self.object_perm_read_after('public-read-write')

    def test_object_perm_private_after(self):
        try:
            self.object_perm_read_after('private')
            passed = False
        except:
            passed = True
        self.assertTrue(passed)

    def test_bucket_read_write(self):
        (id, pw) = self.make_user()
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucketname = self.cb_random_bucketname(20)
        bucket = conn.create_bucket(bucketname, policy='public-read-write')
        (id, pw) = self.make_user()
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket = conn.get_bucket(bucketname)
        key = self.cb_random_bucketname(20)
        k = boto.s3.key.Key(bucket)
        k.key = key
        k.set_contents_from_filename("/etc/group")
        k = bucket.get_key(key)
        k.get_contents_to_filename("/dev/null")

    def test_bucket_owner_read(self):
        (id, pw) = self.make_user()
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucketname = self.cb_random_bucketname(20)
        bucket = conn.create_bucket(bucketname, policy='public-read-write')

        (id, pw) = self.make_user()
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bucketname)
        key = self.cb_random_bucketname(20)
        k = boto.s3.key.Key(bucket2)
        k.key = key
        k.set_contents_from_filename("/etc/group", policy='bucket-owner-read')

        # now verify that the owner can read it
        k = bucket.get_key(key)
        k.get_contents_to_filename("/dev/null")

        # now make sure the owner cannot over write it
        passed = False
        try:
            k = boto.s3.key.Key(bucket)

            k.key = key
            k.set_contents_from_filename("/etc/group")
        except:
            passed = True
        self.assertTrue(passed)

    def test_bucket_owner_write(self):
        (id, pw) = self.make_user()
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucketname = self.cb_random_bucketname(20)
        bucket = conn.create_bucket(bucketname, policy='public-read-write')

        (id, pw) = self.make_user()
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bucketname)
        key = self.cb_random_bucketname(20)
        k = boto.s3.key.Key(bucket2)
        k.key = key
        k.set_contents_from_filename("/etc/group", policy='bucket-owner-full-control')

        # now verify that the owner can read it
        k = bucket.get_key(key)
        k.get_contents_to_filename("/dev/null")

        # now make sure the owner cannot over write it
        k = boto.s3.key.Key(bucket)

        k.key = key
        k.set_contents_from_filename("/etc/group")
