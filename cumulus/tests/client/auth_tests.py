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
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        (bucketname,bucket) = self.create_bucket(conn)
        (id, pw) = self.make_user()
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        try:
            bucket = conn.get_bucket(bucketname)
            passes = False
        except:
            passes = True
        self.assertTrue(passes, "second user should not be able to get this bucket")

    def test_adduser(self):
        (id, pw) = self.make_user()
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        (bucketname,bucket) = self.create_bucket(conn)
        (id2, pw2) = self.make_user()
        bucket.add_user_grant("FULL_CONTROL", id2)
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id2, pw2)
        bucket = conn2.get_bucket(bucketname)

    def test_unknown_user(self):
        id = str(uuid.uuid1()).replace("-", "")
        pw = str(uuid.uuid1()).replace("-", "")

        try:
            conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
            conn.get_all_buckets()
            passes = False
        except:
            passes = True
        self.assertTrue(passes, "This user should be rejected")

    def bucket_perms(self, perms):
        (id, pw) = self.make_user()
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        (bucketname,bucket) = self.create_bucket(conn)
        (id2, pw2) = self.make_user()

        for p in perms:
            bucket.add_user_grant(p, id2)

        new_perms = []
        a = bucket.get_acl()
        count = 0
        for g in a.acl.grants:
            if id2 == g.id:
                new_perms.append(g.permission)

        self.assertEqual(len(perms), len(new_perms))
        for np in new_perms:
            self.assertTrue(np in perms)

        return (id2, pw2, bucketname, conn)

    # check a few combos
    def test_write_acp_bucket(self):
        self.bucket_perms(["FULL_CONTROL"])
    def test_Rrw_bucket(self):
        self.bucket_perms(["READ_ACP", "READ", "WRITE"])
    def test_RrW_bucket(self):
        self.bucket_perms(["READ_ACP", "READ", "WRITE_ACP"])
    def test_Rr_bucket(self):
        self.bucket_perms(["READ_ACP", "READ"])
    def test_Rw_bucket(self):
        self.bucket_perms(["READ_ACP", "WRITE"])
    def test_Wr_bucket(self):
        self.bucket_perms(["WRITE_ACP", "READ"])
    def test_R_bucket(self):
        self.bucket_perms(["READ_ACP"])
    def test_W_bucket(self):
        self.bucket_perms(["WRITE_ACP"])
    def test_r_bucket(self):
        self.bucket_perms(["READ"])
    def test_w_bucket(self):
        b = self.bucket_perms(["WRITE"])

    def test_bad_perm_bucket(self):
        try:
            b = self.bucket_perms(["NOTATING"])
            passes = False
        except:
            passes = True
        self.assertTrue(passes)

    # verify user 2 is actually rejected/accepted at the right times
    def test_connect_2nd_full_bucket(self):
        (id,pw,bn,conn) = self.bucket_perms(["FULL_CONTROL"])
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bn)
    def test_connect_2nd_Rr_bucket(self):
        (id,pw,bn,conn) = self.bucket_perms(["READ_ACP", "READ"])
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bn)
    def test_connect_2nd_r_bucket(self):
        (id,pw,bn,conn) = self.bucket_perms(["READ"])
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bn)
    def test_connect_2nd_w_bucket(self):
        (id,pw,bn,conn) = self.bucket_perms(["WRITE"])
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        try:
            bucket2 = conn2.get_bucket(bn)
            passes = False
        except:
            passes = True
        self.assertTrue(passes)

    def upload_etc_group(self, bucket):
        key = self.cb_random_bucketname(10)
        k = boto.s3.key.Key(bucket)
        k.key = key
        k.set_contents_from_filename("/etc/group")
        return k

    # check if we can upload
    def test_can_upload_full(self):
        (id,pw,bn,conn) = self.bucket_perms(["FULL_CONTROL"])
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bn)
        k = self.upload_etc_group(bucket2)
        k.delete()

    def test_can_upload_wr(self):
        (id,pw,bn,conn) = self.bucket_perms(["WRITE", "READ"])
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bn)
        k = self.upload_etc_group(bucket2)
        k.delete()

    def test_can_upload_r(self):
        (id,pw,bn,conn) = self.bucket_perms(["READ"])
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bn)
        try:
            self.upload_etc_group(bucket2)
            passes = False
        except:
            passes = True
        self.assertTrue(passes)

    # check if can delete a bucket
    def test_can_delete_bucket(self):
        (id,pw,bn,conn) = self.bucket_perms(["READ", "WRITE"])
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bn)
        bucket2.delete()
    def test_cannot_delete_bucket(self):
        (id,pw,bn,conn) = self.bucket_perms(["READ"])
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bn)
        try:
            bucket2.delete()
            passes = False
        except:
            passes = True
        self.assertTrue(passes, "should not be able to delete")

    # check acl love
    def test_acl_RWr(self):
        (id,pw,bn,conn) = self.bucket_perms(["READ_ACP", "WRITE_ACP", "READ"])
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bn)
        a = bucket2.get_acl()
    def test_acl_Rr(self):
        (id,pw,bn,conn) = self.bucket_perms(["READ_ACP", "READ"])
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bn)
        a = bucket2.get_acl()
    def test_acl_Wr(self):
        (id,pw,bn,conn) = self.bucket_perms(["WRITE_ACP", "READ"])
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bn)
        try:
            a = bucket2.get_acl()
            passes = False
        except:
            passes = True
        self.assertTrue(passes, "should not be able to list")
    def test_acl_r(self):
        (id,pw,bn,conn) = self.bucket_perms(["READ"])
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bn)
        try:
            a = bucket2.get_acl()
            passes = False
        except:
            passes = True
        self.assertTrue(passes, "should not be able to list")

    def test_acl_keyup(self):
        (id,pw,bn,conn) = self.bucket_perms(["FULL_CONTROL"])
        bucket = conn.get_bucket(bn)
        k = self.upload_etc_group(bucket)
        k.add_user_grant("FULL_CONTROL", id)
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bn)
        k = bucket2.get_key(k.key)
        k.get_contents_to_filename("/dev/null")

    def test_acl_just_read_keyup(self):
        (id,pw,bn,conn) = self.bucket_perms(["FULL_CONTROL"])
        bucket = conn.get_bucket(bn)
        k = self.upload_etc_group(bucket)
        k.add_user_grant("READ", id)
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bn)
        k = bucket2.get_key(k.key)
        k.get_contents_to_filename("/dev/null")

    def test_acl_no_grant_keyup(self):
        (id,pw,bn,conn) = self.bucket_perms(["FULL_CONTROL"])
        bucket = conn.get_bucket(bn)
        k = self.upload_etc_group(bucket)
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bn)
        try:
            k = bucket2.get_key(k.key)
            k.get_contents_to_filename("/dev/null")
            passes = False
        except:
            passes = True
        self.assertTrue(passes)

    def test_acl_key_over_write(self):
        (id,pw,bn,conn) = self.bucket_perms(["FULL_CONTROL"])
        bucket = conn.get_bucket(bn)
        k = self.upload_etc_group(bucket)
        k.add_user_grant("FULL_CONTROL", id)
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bn)
        k = bucket2.get_key(k.key)
        k.set_contents_from_filename("/etc/group")

    def test_acl_key_over_write_wr(self):
        (id,pw,bn,conn) = self.bucket_perms(["FULL_CONTROL"])
        bucket = conn.get_bucket(bn)
        k = self.upload_etc_group(bucket)
        k.add_user_grant("WRITE", id)
        k.add_user_grant("READ", id)
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bn)
        k = bucket2.get_key(k.key)
        k.set_contents_from_filename("/etc/group")

    def test_acl_key_over_write_r(self):
        (id,pw,bn,conn) = self.bucket_perms(["FULL_CONTROL"])
        bucket = conn.get_bucket(bn)
        k = self.upload_etc_group(bucket)
        k.add_user_grant("READ", id)
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bn)
        k = bucket2.get_key(k.key)
        try:
            k.set_contents_from_filename("/etc/group")
            passes = False
        except:
            passes = True
        self.assertTrue(passes)

    def test_get_acl_key_Rr(self):
        (id,pw,bn,conn) = self.bucket_perms(["FULL_CONTROL"])
        bucket = conn.get_bucket(bn)
        k = self.upload_etc_group(bucket)
        k.add_user_grant("READ", id)
        k.add_user_grant("READ_ACP", id)
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bn)
        k = bucket2.get_key(k.key)
        acl = k.get_acl()

    def test_get_acl_key_r(self):
        (id,pw,bn,conn) = self.bucket_perms(["FULL_CONTROL"])
        bucket = conn.get_bucket(bn)
        k = self.upload_etc_group(bucket)
        k.add_user_grant("READ", id)
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bn)
        k = bucket2.get_key(k.key)
        try:
            acl = k.get_acl()
            passes = False
        except:
            passes = True
        self.assertTrue(passes)

    def test_acl_key_get_key_r(self):
        (id,pw,bn,conn) = self.bucket_perms(["FULL_CONTROL"])
        bucket = conn.get_bucket(bn)
        k = self.upload_etc_group(bucket)
        k.add_user_grant("READ", id)
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bn)
        k = bucket2.get_key(k.key)
        k.get_contents_to_filename("/dev/null")

    def test_acl_key_get_key_r(self):
        (id,pw,bn,conn) = self.bucket_perms(["FULL_CONTROL"])
        bucket = conn.get_bucket(bn)
        k = self.upload_etc_group(bucket)
        k.add_user_grant("READ_ACP", id)
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bn)
        try:
            k = bucket2.get_key(k.key)
            k.get_contents_to_filename("/dev/null")
            passes = False
        except:
            passes = True
        self.assertTrue(passes)

    def test_fail_set_new_acl_key(self):
        (id,pw,bn,conn) = self.bucket_perms(["FULL_CONTROL"])
        bucket = conn.get_bucket(bn)
        k = self.upload_etc_group(bucket)
        k.add_user_grant("READ", id)
        conn2 = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
        bucket2 = conn2.get_bucket(bn)
        k2 = bucket2.get_key(k.key)
        try:
            k2.add_user_grant("WRITE_ACP", id)
            passes = False
        except:
            passes = True
        self.assertTrue(passes)

    def test_fail_access(self):
        (id, pw) = self.make_user()
        conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw + "S")
        try:
            nbs = conn.get_all_buckets()
            passes = False
        except:
            passes = True
        self.assertTrue(passes)

    def test_upload_bad_user(self):
        id = str(uuid.uuid1()).replace("-", "")
        pw = str(uuid.uuid1()).replace("-", "")

        try:
            conn = pycb.test_common.cb_get_conn(self.host, self.port, id, pw)
            bucket = conn.create_bucket(bucketname)
            passes = False
        except:
            passes = True
        self.assertTrue(passes, "This user should be rejected")

