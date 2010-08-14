import string
import random
import os
import sys
import nose.tools
import boto
from boto.exception import BotoServerError
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
import datetime
import re
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

    def test_error(self):
	self.assertRaises(BotoServerError, self.ec2conn.request_spot_instances, '2.0', 'nil', 1, None, None, None, None, None, None, None, None, None, 'm1.large')

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

        instance2.stop()
        
        time.sleep(1)
		
        allReservations = self.ec2conn.get_all_instances()
        assert len(allReservations) == 1, 'incorrect result size'
	reservation = allReservations[0]
        assert len(reservation.instances) == 2, 'incorrect result size'
        instance3 = reservation.instances[0]
        assert instance3.id == instance1.id, 'returned instance is not the same as before'
        instance4 = reservation.instances[1]
        assert instance4.id != instance2.id
        assert instance4.spot_instance_request_id == prev_id, 'returned spot instance id is not the same from the request call'
        assert instance4.instanceLifecycle == "spot", 'instance life cycle is incorrect'
        assert instance4.instance_type == "m1.small", 'returned instance is not the same as submitted'

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

    def test_multiple_requestSI(self):
        bucket_name = "Repo"
        bucket = self.s3conn.get_bucket(bucket_name)
        k = boto.s3.key.Key(bucket)
        image_name = self.cb_random_bucketname(10)
        k.key = "WHATEVER/" + image_name
        k.set_contents_from_filename("/etc/group")
        url = "cumulus://HOST/" + bucket_name + "/" + k.key
        
        ts1 = now = datetime.datetime.now()

        req1 = self.ec2conn.request_spot_instances("1.0",url,count=2,type="persistent")
        req2 = self.ec2conn.request_spot_instances("1.2",url,count=1)
        req3 = self.ec2conn.request_spot_instances("1.5",url,count=1)
        
        req1_res = None
        req2_res = None
        req3_res = None

        allReservations = self.ec2conn.get_all_instances()
        assert len(allReservations) == 3, 'incorrect result size'
        for reservation in allReservations:
           assert len(reservation.instances) > 0, 'reservations have no instances!'
           inst = reservation.instances[0]
           if inst.spot_instance_request_id == req1[0].id:
              req1_res = reservation
           elif inst.spot_instance_request_id == req2[0].id:
              req2_res = reservation
           elif inst.spot_instance_request_id == req3[0].id:
              req3_res = reservation

        assert len(req1_res.instances) == 2
        assert len(req2_res.instances) == 1
        assert len(req3_res.instances) == 1
 
        price_history = self.ec2conn.get_spot_price_history(start_time=ts1.isoformat())
        assert len(price_history) == 0       

        run_res1 = self.ec2conn.run_instances(url,min_count=2)

        time.sleep(1)

        price_history = self.ec2conn.get_spot_price_history(start_time=ts1.isoformat())
        assert len(price_history) == 1
        price1 = price_history[0]
        assert price1.price == 1.0

        #run_res2 = self.ec2conn.run_instances(url)

        req1[0].cancel()
        req1_res.stop_all()
        req2_res.stop_all()
        req3_res.stop_all()
        run_res1.stop_all()
        #run_res2.stop_all()
