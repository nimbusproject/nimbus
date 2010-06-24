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
import traceback
from boto.exception import S3DataError


class TestBucketsWithBoto(unittest.TestCase):

    def setUp(self):
        (self.host, self.port) = pycb.test_common.get_contact()
        self.type = None
        (self.id, self.pw) = pycb.test_common.make_user()
        self.clean_all()

    def tearDown(self):
        self.clean_all()
        pycb.test_common.clean_user(self.id)

    def clean_all(self):
        conn = pycb.test_common.cb_get_conn(self.host, self.port, self.id, self.pw)
        nbs = conn.get_all_buckets()
        for b in nbs:
            rs = b.list()
            for key in rs:
                key.delete()
            b.delete()
#        pass

    def cb_random_bucketname(self, len):
        chars = string.letters + string.digits
        newpasswd = ""
        for i in range(len):
            newpasswd = newpasswd + random.choice(chars)
        return newpasswd

    def bucket_exists(self, conn, name):
        buckets = conn.get_all_buckets()
        for b in buckets:
            if b.name == name:
                return True
        return False

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

    def test_list_simple(self): 
        conn = pycb.test_common.cb_get_conn(self.host, self.port, self.id, self.pw)
        buckets = conn.get_all_buckets()

    def test_list_many(self):
        conn = pycb.test_common.cb_get_conn(self.host, self.port, self.id, self.pw)
        (bucketname,bucket) = self.create_bucket(conn)

        key_list = []
        for i in range(0, 10):
            key = self.cb_random_bucketname(10)
            k = boto.s3.key.Key(bucket)
            k.key = key
            try:
                k.set_contents_from_filename("/etc/group")
            except S3DataError, s3e:
                traceback.print_exc(file=sys.stdout)

                print s3e
                print s3e.reason
                print dir(s3e)
                raise s3e
            key_list.append(key)

        for k in key_list:
            key = bucket.get_key(k)

        buckets = conn.get_all_buckets()

    def test_simple_bucket(self):
        conn = pycb.test_common.cb_get_conn(self.host, self.port, self.id, self.pw)
        (bucketname,bucket) = self.create_bucket(conn)
        # verify the bucket exists
        rc = self.bucket_exists(conn, bucketname)
        self.assertTrue(rc, bucketname)
        bucket.delete()
        rc = self.bucket_exists(conn, bucketname)
        self.assertFalse(rc, bucketname)

