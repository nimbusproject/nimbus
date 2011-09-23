import string
import random
import os
import boto
import pexpect
from boto.ec2.connection import EC2Connection
from boto.exception import BotoServerError
import boto.ec2 
import unittest
import pycb
import pynimbusauthz
from  pynimbusauthz.db import * 
from  pynimbusauthz.user import * 
import pycb.test_common
from boto.s3.connection import OrdinaryCallingFormat
from boto.s3.connection import S3Connection
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
        ids = []
        print instances
        for reserv in instances:
            for inst in reserv.instances:
                if inst.state != u'terminated':
                    print "Terminating instance %s" % inst
                    ids.append(inst.id)
        if ids:    
            self.ec2conn.terminate_instances(ids)

    def cb_random_bucketname(self, len):
        chars = string.letters + string.digits
        newpasswd = ""
        for i in range(len):
            newpasswd = newpasswd + random.choice(chars)
        return newpasswd
    
    def store_new_image(self):
        bucket = self.s3conn.get_bucket("Repo")
        k = boto.s3.key.Key(bucket)
        image_name = self.cb_random_bucketname(10)
        k.key = "VMS/" + self.can_user.get_id() + "/" + image_name
        k.set_contents_from_filename(os.environ['NIMBUS_SOURCE_TEST_IMAGE'])
        return image_name

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

        self.ec2conn = EC2Connection(self.s3id, self.s3pw, host=host, port=ec2port)
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
        if self.db != None:
            self.db.close()
        self.killall_running()


    def test_ec2_submit_name_format(self):
        image_name = self.store_new_image()
        image = self.ec2conn.get_image(image_name)
        print "==================================="
        print image.name
        print image.location
        print image_name
        print "==================================="

        res = image.run() 

    def test_ec2_submit_availability_zone(self):
        image_name = self.store_new_image()
        image = self.ec2conn.get_image(image_name)
        print "==================================="
        print image.name
        print image.location
        print image_name
        print "==================================="


        #Ensure that starting a VM with a non-existant zone fails
        zone = "notarealzone"
        try:
            res = image.run(placement=zone) 
        except Exception, e:
            print e.body
            assert e.body.find("Resource pool (Availability Zone) '%s' does not exist" % zone) >= 0

        #Ensure that starting a VM with a no zone defined still works
        res = image.run() 
        
        #Ensure that starting a VM with default zone works
        res = image.run(placement="default") 

        #Add a node in a zone, submit to it
        nimbus_home = get_nimbus_home()
        cmd = "%s/bin/nimbus-nodes -a zonetest -p %s -m 10240" % (nimbus_home, zone)
        (x, rc)=pexpect.run(cmd, withexitstatus=1)

        res = image.run(placement=zone) 

        cmd = "%s/bin/nimbus-nodes -d zonetest" % nimbus_home
        (x, rc)=pexpect.run(cmd, withexitstatus=1)

        # Make sure when we remove the zone, it still fails
        try:
            res = image.run(placement=zone) 
        except Exception, e:
            print e.body
            assert e.body.find("Resource pool (Availability Zone) '%s' does not exist" % zone) >= 0
        

    def test_ec2_submit_url(self):
        bucket_name = "Repo"
        bucket = self.s3conn.get_bucket(bucket_name)
        k = boto.s3.key.Key(bucket)
        image_name = self.cb_random_bucketname(10)
        k.key = "WHATEVER/" + image_name
        k.set_contents_from_filename(os.environ['NIMBUS_SOURCE_TEST_IMAGE'])
        url = "cumulus://HOST/" + bucket_name + "/" + k.key
        print url
        res = self.ec2conn.run_instances(url)
    
    def test_ec2_idempotent_submit(self):
        image_name = self.store_new_image()
        token = str(uuid.uuid4())

        # start an instance with a client token
        res1 = self.ec2conn.run_instances(image_name, client_token=token)
        assert len(res1.instances) == 1, "Expected 1 launched instance"

        # now re-launch with the same token, should get back the same
        # instance and nothing new should be running
        res2 = self.ec2conn.run_instances(image_name, client_token=token)
        assert len(res2.instances) == 1, "Expected 1 launched instance"
        assert res1.id == res2.id, "diff reservation IDs: %s vs %s" % (res1.id, res2.id)
        assert res1.owner_id == res2.owner_id, "diff owner IDs"
        assert res1.instances[0].id == res2.instances[0].id, "diff instance IDs"
        assert res1.instances[0].dns_name == res2.instances[0].dns_name, "diff DNS names"
        assert res1.instances[0].state == res2.instances[0].state, (
                "diff states: %s vs %s" % (res1.instances[0].state, res2.instances[0].state))

        # check that no other instance is running
        assert len(self.ec2conn.get_all_instances()) == 1, "!1 instances running"

        # start another but with a different token-- should get a new instance
        another_token = str(uuid.uuid4())
        res3 = self.ec2conn.run_instances(image_name, client_token=another_token)
        assert len(res3.instances) == 1, "Expected 1 launched instance"
        assert res3.id != res1.id, "same reservation IDs"
        assert res3.instances[0].id != res1.instances[0].id, "same instance IDs"
        
        assert len(self.ec2conn.get_all_instances()) == 2, "!2 instances running"

        # kill the first VM and re-launch with the same token -- should get
        # back a reservation with the same first instance but terminated
        assert len(self.ec2conn.terminate_instances([res1.instances[0].id])) == 1
        
        res4 = self.ec2conn.run_instances(image_name, client_token=token)
        assert len(res4.instances) == 1, "Expected 1 launched instance"
        assert res1.id == res4.id, "diff reservation IDs"
        assert res1.owner_id == res4.owner_id, "diff owner IDs"
        assert res1.instances[0].id == res4.instances[0].id, "diff instance IDs"
        state = res4.instances[0].state 
        assert state == 'terminated', "state was %s" % state
        
        # now attempt to launch instances with the same tokens, but with different
        # parameters. Should get an exception with IdempotentParameterMismatch error
        another_image_name = self.store_new_image()
        self.assertRunInstancesError('IdempotentParameterMismatch', 
                another_image_name, client_token=token)
        
        self.assertRunInstancesError('IdempotentParameterMismatch', 
                image_name, min_count=3, max_count=3, client_token=token)
        
        self.assertRunInstancesError('IdempotentParameterMismatch', 
                another_image_name, client_token=another_token)
        
        self.assertRunInstancesError('IdempotentParameterMismatch', 
                image_name, min_count=3, max_count=3, client_token=another_token)

    def test_ec2_idempotent_group_submit(self):
        image_name = self.store_new_image()
        token = str(uuid.uuid4())

        # start an instance with a client token
        res1 = self.ec2conn.run_instances(image_name, client_token=token, 
                min_count=3, max_count=3)
        assert len(res1.instances) == 3, "Expected 3 launched instances"

        # now re-launch with the same token, should get back the same
        # instance and nothing new should be running
        res2 = self.ec2conn.run_instances(image_name, client_token=token,
                min_count=3, max_count=3)
        assert len(res2.instances) == 3, "Expected 3 launched instances"
        assert res1.id == res2.id, "diff reservation IDs: %s vs %s" % (res1.id, res2.id)
        assert res1.owner_id == res2.owner_id, "diff owner IDs"
        for i1, i2 in zip(res1.instances, res2.instances):
            assert i1.id == i2.id, "diff instance IDs"
            assert i1.dns_name == i2.dns_name, "diff DNS names"
            assert i1.state == i2.state, "diff states: %s vs %s" % (i1.state, i2.state)

    def assertRunInstancesError(self, error_code, *args, **kwargs):
        error = None
        try:
            self.ec2conn.run_instances(*args, **kwargs)
        except BotoServerError,e:
            error = e

        assert error, "Expected %s error but got nothing" % error_code
        assert error.error_code == error_code, "Expected %s error but got %s" % (
                error_code, error.error_code)
