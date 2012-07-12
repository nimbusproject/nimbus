import string
import random
import os
import sys
import nose.tools
import boto
from boto.ec2.connection import EC2Connection
import boto.ec2 
import sys
from ConfigParser import SafeConfigParser
import time
import unittest
import tempfile
import pycb
import pynimbusauthz
from  pynimbusauthz.db import * 
from  pynimbusauthz.user import * 
import pycb.test_common
from boto.s3.connection import OrdinaryCallingFormat
from boto.s3.connection import S3Connection
import random


class TestEC2List(unittest.TestCase):

    def cb_random_bucketname(self, len):
        chars = string.letters + string.digits
        newpasswd = ""
        for i in range(len):
            newpasswd = newpasswd + random.choice(chars)
        return newpasswd

    def setUp(self):
        host = 'localhost'
        cumport = 8888
        ec2port = 8444
        try:
            ec2port = int(os.environ['NIMBUS_TEST_EC2_PORT'])
        except:
            pass
        try:
            cumport = int(os.environ['NIMBUS_TEST_S3_PORT'])
        except:
            pass


        self.db = DB(pycb.config.authzdb)
        self.friendly = self.cb_random_bucketname(21)
        self.can_user = User(self.db, friendly=self.friendly, create=True)
        self.subject = self.cb_random_bucketname(21)
        self.s3id = self.cb_random_bucketname(21)
        self.s3pw = self.cb_random_bucketname(42)
        self.s3user = self.can_user.create_alias(self.s3id, pynimbusauthz.alias_type_s3, self.friendly, self.s3pw)
        self.dnuser = self.can_user.create_alias(self.subject, pynimbusauthz.alias_type_x509, self.friendly)

        self.ec2conn = EC2Connection(self.s3id, self.s3pw, host=host, port=ec2port, debug=2)
        self.ec2conn.host = host

        cf = OrdinaryCallingFormat()
        self.s3conn = S3Connection(self.s3id, self.s3pw, host=host, port=cumport, is_secure=False, calling_format=cf)
        self.db.commit()

    def tearDown(self):
        if self.s3conn != None:
            pass
        if self.ec2conn != None:
            pass
        if self.s3user != None:
            self.s3user.remove()
        if self.dnuser != None:
            self.dnuser.remove()
        if self.can_user != None:
            self.can_user.destroy_brutally()
        if self.db != None:
            self.db.close()

    def test_ec2_list_upload(self):
        # obviously this will not work if the default name changes
        bucket = self.s3conn.get_bucket("Repo")
        k = boto.s3.key.Key(bucket)
        image_id = self.cb_random_bucketname(25)
        k.key = "VMS/" + self.can_user.get_id() + "/" + image_id
        k.set_contents_from_filename(os.environ['NIMBUS_SOURCE_TEST_IMAGE'])

        images = self.ec2conn.get_all_images()
        self.assertTrue(len(images) >= 1, "should be 1 image %d" % len(images))
        found = False
        for i in images:
            if i.id == image_id:
                found = True

        for i in images:
            print "+++++++++++++++++++++++++"
            print i
        self.assertTrue(found, "The image should have been found %s" % (image_id))
        
    def test_ec2_signature_v1(self):
        self.ec2conn.SignatureVersion = '1'
        images = self.ec2conn.get_all_images()
        # if there are no exceptions, we can call this success

