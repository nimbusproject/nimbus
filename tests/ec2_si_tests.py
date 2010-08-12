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

        nh = get_nimbus_home()
        groupauthz_dir = os.path.join(nh, "services/etc/nimbus/workspace-service/group-authz/")
        add_member(groupauthz_dir, self.subject, 4)


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

    def test_simple_requestSI(self):
        bucket_name = "Repo"
        bucket = self.s3conn.get_bucket(bucket_name)
        k = boto.s3.key.Key(bucket)
        image_name = self.cb_random_bucketname(10)
        k.key = "WHATEVER/" + image_name
        k.set_contents_from_filename("/etc/group")
        url = "cumulus://HOST/" + bucket_name + "/" + k.key
        
        result = self.ec2conn.request_spot_instances("1.0",url)
        assert len(result) == 1, 'incorrect result size'
        request = result[0]
        prev_id = request.id
        assert request.launch_specification.instance_type == "m1.small"
        assert request.price == 1.0, 'incorrect price returned'
        assert request.type == "one-time", 'incorrect type returned'        
        
        result = self.ec2conn.get_all_spot_instance_requests()
        assert len(result) == 1, 'incorrect result size'
        request = result[0]
        assert request.id == prev_id, 'returned id is not the same from the request call'
        assert request.price == 1.0, 'incorrect price returned'       
        assert request.type == "one-time", 'incorrect type returned'
        assert request.state == "active", 'request should have been in the active status'

        allReservations = self.ec2conn.get_all_instances()
        assert len(allReservations) == 1, 'incorrect result size'
        reservation = allReservations[0]
        assert len(reservation.instances) == 1, 'incorrect result size'
        instance = reservation.instances[0]
        assert instance.id == request.instance_id, 'returned instance id must be equal to spot instance id' 
        assert instance.spot_instance_request_id == prev_id, 'returned spot instance id is not the same from the request call'
        assert instance.instanceLifecycle == "spot", 'instance life cycle is incorrect'
        assert instance.instance_type == "m1.small", 'returned instance is not the same as submitted'

        request.cancel()

        result = self.ec2conn.get_all_spot_instance_requests()
        assert len(result) == 1, 'incorrect result size'
        request = result[0]
        assert request.id == prev_id, 'returned id is not the same from the request call'
        assert request.price == 1.0, 'incorrect price returned'       
        assert request.type == "one-time", 'incorrect type returned'
        assert request.state == "canceled", 'request should have been in the canceled status'
        assert reservation.instances[0].id == request.instance_id, 'returned instance id must be equal to spot instance id'

        reservation.stop_all()

        result = self.ec2conn.get_all_spot_instance_requests()
        assert len(result) == 1, 'incorrect result size'
        request = result[0]
        assert request.id == prev_id, 'returned id is not the same from the request call'
        assert request.price == 1.0, 'incorrect price returned'
        assert request.type == "one-time", 'incorrect type returned'
        assert request.state == "canceled", 'request should have been in the canceled status'
        assert not request.instance_id, 'there shouldnt be an instance id'

    def test_persistent_requestSI(self):
        bucket_name = "Repo"
        bucket = self.s3conn.get_bucket(bucket_name)
        k = boto.s3.key.Key(bucket)
        image_name = self.cb_random_bucketname(10)
        k.key = "WHATEVER/" + image_name
        k.set_contents_from_filename("/etc/group")
        url = "cumulus://HOST/" + bucket_name + "/" + k.key
        
        result = self.ec2conn.request_spot_instances("1.0",url,count=2,type="persistent")
        assert len(result) == 1, 'incorrect result size'
        request = result[0]
        prev_id = request.id
        assert request.launch_specification.instance_type == "m1.small"
        assert request.price == 1.0, 'incorrect price returned'
        print "REQUEST TYPE: "
        print request.type
        assert request.type == "persistent", 'incorrect type returned'        
        
        result = self.ec2conn.get_all_spot_instance_requests()
        assert len(result) == 1, 'incorrect result size'
        request = result[0]
        assert request.id == prev_id, 'returned id is not the same from the request call'
        assert request.price == 1.0, 'incorrect price returned'       
        assert request.type == "persistent", 'incorrect type returned'
        assert request.state == "active", 'request should have been in the active status'

        allReservations = self.ec2conn.get_all_instances()
        assert len(allReservations) == 1, 'incorrect result size'
        reservation = allReservations[0]
        assert len(reservation.instances) == 2, 'incorrect result size'
        instance1 = reservation.instances[0]
        assert instance1.id == request.instance_id, 'returned instance id must be equal to spot instance id' 
        assert instance1.spot_instance_request_id == prev_id, 'returned spot instance id is not the same from the request call'
        assert instance1.instanceLifecycle == "spot", 'instance life cycle is incorrect'
        assert instance1.instance_type == "m1.small", 'returned instance is not the same as submitted'
        instance2 = reservation.instances[1]
        assert instance2.spot_instance_request_id == prev_id, 'returned spot instance id is not the same from the request call'
        assert instance2.instanceLifecycle == "spot", 'instance life cycle is incorrect'
        assert instance2.instance_type == "m1.small", 'returned instance is not the same as submitted'

        request.cancel()

        result = self.ec2conn.get_all_spot_instance_requests()
        assert len(result) == 1, 'incorrect result size'
        request = result[0]
        assert request.id == prev_id, 'returned id is not the same from the request call'
        assert request.price == 1.0, 'incorrect price returned'       
        assert request.type == "persistent", 'incorrect type returned'
        assert request.state == "canceled", 'request should have been in the canceled status'
        assert reservation.instances[0].id == request.instance_id, 'returned instance id must be equal to spot instance id'

        reservation.stop_all()

        result = self.ec2conn.get_all_spot_instance_requests()
        assert len(result) == 1, 'incorrect result size'
        request = result[0]
        assert request.id == prev_id, 'returned id is not the same from the request call'
        assert request.price == 1.0, 'incorrect price returned'
        assert request.type == "persistent", 'incorrect type returned'
        assert request.state == "canceled", 'request should have been in the canceled status'
        assert not request.instance_id, 'there shouldnt be an instance id'

        allReservations = self.ec2conn.get_all_instances()
        assert len(allReservations) == 0, 'incorrect result size'
