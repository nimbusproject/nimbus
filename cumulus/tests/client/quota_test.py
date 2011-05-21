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
class TestBucketsWithBoto(unittest.TestCase):

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
        self.user_id_list.append([id, pw])
        return (id, pw)

    def clean_all(self, id, pw):
        print "clean %s : %s" % (id, pw)
        conn5 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        print "hi 1"
        nbs = conn5.get_all_buckets()
        print "hi 2"
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

    def create_bucket(self, conn):
        done = False
        count = 0
        while done == False:
            bucketname = self.cb_random_bucketname(10)
            try:
                bucket = conn.create_bucket(bucketname)
                done = True
            except Exception, ex:
                print ex
                print sys.exc_info()[0]
                print "bucket name %s exists, trying again" % (bucketname)
                done = False
                count = count + 1
                if count > 10:
                    done = True
                    print "could not make bucket in many trys"
                    raise

        return (bucketname, bucket)

    def test_basic_pass_quota(self):
        fsize = 100
        data = self.cb_random_bucketname(fsize)
        (id, pw) = self.make_user()
        pycb.test_common.set_user_quota(id, fsize + 1)
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        (bucketname,bucket) = self.create_bucket(conn)
        key = self.cb_random_bucketname(10)
        k = boto.s3.key.Key(bucket)
        k.key = key
        k.set_contents_from_string(data)

    def test_exact_pass_quota(self):
        fsize = 100
        data = self.cb_random_bucketname(fsize)
        (id, pw) = self.make_user()
        pycb.test_common.set_user_quota(id, fsize)
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        (bucketname,bucket) = self.create_bucket(conn)
        key = self.cb_random_bucketname(10)
        k = boto.s3.key.Key(bucket)
        k.key = key
        k.set_contents_from_string(data)

    def test_same_name_reclaim_space_pass_quota(self):
        fsize = 100
        data = self.cb_random_bucketname(fsize)
        (id, pw) = self.make_user()
        pycb.test_common.set_user_quota(id, fsize)
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        (bucketname,bucket) = self.create_bucket(conn)
        key = self.cb_random_bucketname(10)
        k = boto.s3.key.Key(bucket)
        k.key = key
        k.set_contents_from_string(data)

        k = boto.s3.key.Key(bucket)
        k.key = key
        k.set_contents_from_string(data)

    def test_many_key_pass_quota(self):
        fsize = 100
        fcount = 10
        data = self.cb_random_bucketname(fsize)
        (id, pw) = self.make_user()
        pycb.test_common.set_user_quota(id, fsize*fcount + 1)
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        (bucketname,bucket) = self.create_bucket(conn)

        for i in range(0, fcount):
            key = self.cb_random_bucketname(10)
            k = boto.s3.key.Key(bucket)
            k.key = key
            k.set_contents_from_string(data)

    def test_bust_single_quota(self):
        fsize = 100
        data = self.cb_random_bucketname(fsize)
        (id, pw) = self.make_user()
        pycb.test_common.set_user_quota(id, fsize - 1)
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        (bucketname,bucket) = self.create_bucket(conn)
        key = self.cb_random_bucketname(10)
        k = boto.s3.key.Key(bucket)
        k.key = key
        passed = True
        try:
            k.set_contents_from_string(data)
            passed = False
        except:
            pass

    def test_many_key_fail_quota(self):
        fsize = 100
        fcount = 10
        data = self.cb_random_bucketname(fsize)
        (id, pw) = self.make_user()
        pycb.test_common.set_user_quota(id, fsize*fcount + 1)
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        (bucketname,bucket) = self.create_bucket(conn)

        for i in range(0, fcount):
            key = self.cb_random_bucketname(10)
            k = boto.s3.key.Key(bucket)
            k.key = key
            k.set_contents_from_string(data)

        # add the last one that will make it break
        key = self.cb_random_bucketname(10)
        k = boto.s3.key.Key(bucket)
        k.key = key
        passed = True
        try:
            k.set_contents_from_string(data)
            passed = False
        except:
            pass

    def test_delete_quota(self):
        fsize = 100
        data = self.cb_random_bucketname(fsize)
        (id, pw) = self.make_user()
        pycb.test_common.set_user_quota(id, fsize + 1)
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        (bucketname,bucket) = self.create_bucket(conn)

        key_to_delete = self.cb_random_bucketname(10)
        k2d = boto.s3.key.Key(bucket)
        k2d.key = key_to_delete
        k2d.set_contents_from_string(data)

        # add the last one that will make it break
        key = self.cb_random_bucketname(10)
        k = boto.s3.key.Key(bucket)
        k.key = key
        passed = True
        try:
            k.set_contents_from_string(data)
            passed = False
        except:
            pass

        # delete original
        k2d.delete()

        key = self.cb_random_bucketname(10)
        k = boto.s3.key.Key(bucket)
        k.key = key
        passed = True
        k.set_contents_from_string(data)



