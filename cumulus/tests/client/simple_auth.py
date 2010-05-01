import string
import random
import os
import sys
import nose.tools
import boto
from boto.s3.connection import S3Connection
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
        conn5 = self.cb_get_conn(id, pw)
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
        chars = string.letters + string.digits
        newpasswd = ""
        for i in range(len):
            newpasswd = newpasswd + random.choice(chars)
        return newpasswd

    def cb_get_conn(self, id, pw):
        cf = OrdinaryCallingFormat()
        self.type = type
        conn = S3Connection(id, pw, host=self.host, port=self.port, is_secure=False, calling_format=cf)
        return conn

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

    def test_bucket_perm_simple(self):
        (id, pw) = self.make_user()
        (id2, pw2) = self.make_user()
        conn = self.cb_get_conn(id, pw)
        nbs = conn.get_all_buckets()
        conn2 = self.cb_get_conn(id, pw)
        nbs2 = conn.get_all_buckets()
