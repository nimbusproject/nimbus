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
import filecmp
import pycb
import pynimbusauthz
from  pynimbusauthz.db import * 
from  pynimbusauthz.user import * 
import pycb.test_common
from boto.s3.connection import OrdinaryCallingFormat
from boto.s3.connection import S3Connection
import random
from nimbusweb.setup.groupauthz import *

def get_nimbus_home():
    """Determines home directory of Nimbus install we are using.
    
    First looks for a NIMBUS_HOME enviroment variable, else assumes that
    the home directory is the parent directory of the directory with this
    script.
    """
    nimbus_home = os.getenv("NIMBUS_HOME")
    if not nimbus_home:
        script_dir = os.path.dirname(__file__)
        nimbus_home = os.path.dirname(script_dir)
    if not os.path.exists(nimbus_home):
        raise CLIError('ENIMBUSHOME', "NIMBUS_HOME must refer to a valid path")
    return nimbus_home


class TestEC2Submit(unittest.TestCase):

    def killall_running(self):
	instances = self.ec2conn.get_all_instances()
        print instances
        for reserv in instances:
            for inst in reserv.instances:
                if inst.state == u'running':
                    print "Terminating instance %s" % inst
                    inst.stop()


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
        self.db = DB(pycb.config.authzdb)
        self.friendly = os.environ['NIMBUS_TEST_USER']
        self.can_user = User.get_user_by_friendly(self.db, self.friendly)
        s3a = self.can_user.get_alias_by_friendly(self.friendly, pynimbusauthz.alias_type_s3)
        x509a = self.can_user.get_alias_by_friendly(self.friendly, pynimbusauthz.alias_type_x509)

        self.subject = x509a.get_name()
        self.s3id = s3a.get_name()
        self.s3pw = s3a.get_data()
        self.s3user = s3a
        self.dnuser = x509a

        self.ec2conn = EC2Connection(self.s3id, self.s3pw, host=host, port=ec2port, debug=2)
        self.ec2conn.host = host

        cf = OrdinaryCallingFormat()
        self.s3conn = S3Connection(self.s3id, self.s3pw, host=host, port=cumport, is_secure=False, calling_format=cf)
        self.db.commit()
        self.killall_running()


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
        self.killall_running()


    def test_ec2_submit_name_format(self):
        bucket = self.s3conn.get_bucket("Repo")
        k = boto.s3.key.Key(bucket)
        image_name = self.cb_random_bucketname(10)
        k.key = "VMS/" + self.can_user.get_id() + "/" + image_name
        k.set_contents_from_filename("/etc/group")
        image = self.ec2conn.get_image(image_name)
        res = image.run() 
        res.stop_all()

    def test_ec2_submit_url(self):
        bucket_name = "Repo"
        bucket = self.s3conn.get_bucket(bucket_name)
        k = boto.s3.key.Key(bucket)
        image_name = self.cb_random_bucketname(10)
        k.key = "WHATEVER/" + image_name
        k.set_contents_from_filename("/etc/group")
        url = "cumulus://HOST/" + bucket_name + "/" + k.key
        print url
        res = self.ec2conn.run_instances(url)
        res.stop_all()

