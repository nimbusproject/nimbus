import uuid
import base64
import os
from boto.ec2.connection import EC2Connection
import unittest
import pycb
import pynimbusauthz
from  pynimbusauthz.db import * 
from  pynimbusauthz.user import * 
import pycb.test_common

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

PUBKEY1 = """
ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEA6DrU6ncayShbPTMrWC8PmvnkTlifNyDSr5qwJsxK
hWyO3lDJmEHtpYc0WfKSJTv4LzbGjjeU1SH0mox5UsdaPUu0UCZ8z7O7R9MGpbIkGIEkcv1qdhkO
fGq03u1asoY3GUMkzQY2HfM40YQ93AQJQ96uDhALPAyEV3ZzEtI8z53++cP0pV6qVI5Wze68DmBu
KReG5I6xCWjcxYfx7e/PXXtkOl1WYZIj8eeiS9UkHN4QhdVCMHSALfz8k4D/N8iiYHm9EBKJp8tA
byPLiPAtZAOodlwlsMINwFY+Qcx1z4US/p8t5J6A+1EMZ9npWyxorFk2AfKDi1KIOeuUC72tfw==
""".replace('\n','')


class TestEC2Misc(unittest.TestCase):

    def setUp(self):
        host = 'localhost'
        ec2port = 8444
        try:
            ec2port = int(os.environ['NIMBUS_TEST_EC2_PORT'])
        except:
            pass

        self.db = DB(pycb.config.authzdb)
        self.friendly = os.environ['NIMBUS_TEST_USER']
        self.can_user = User.get_user_by_friendly(self.db, self.friendly)
        s3a = self.can_user.get_alias_by_friendly(self.friendly, pynimbusauthz.alias_type_s3)

        self.s3id = s3a.get_name()
        self.s3pw = s3a.get_data()

        self.ec2conn = EC2Connection(self.s3id, self.s3pw, host=host, port=ec2port)
        self.ec2conn.host = host

    def test_import_keypair(self):
        keyname = str(uuid.uuid4())

        # this is different in trunk boto, key is already encoded. til then..

        keymaterial = base64.b64encode(PUBKEY1)
        self.ec2conn.import_key_pair(keyname, keymaterial)
        pair = self.ec2conn.get_key_pair(keyname)
        self.assertTrue(pair)
        self.assertEqual(pair.name, keyname)

        self.ec2conn.delete_key_pair(keyname)
        pair = self.ec2conn.get_key_pair(keyname)
        self.assertFalse(pair)

    def test_create_keypair(self):
        keyname = str(uuid.uuid4())
        pair = self.ec2conn.create_key_pair(keyname)
        self.assertTrue(pair)
        self.assertTrue(pair.material)
        pair = self.ec2conn.get_key_pair(keyname)
        self.assertTrue(pair)
        self.assertEqual(pair.name, keyname)

        self.ec2conn.delete_key_pair(keyname)
        pair = self.ec2conn.get_key_pair(keyname)
        self.assertFalse(pair)

    def test_availability_zones(self):
        """Test wheter AZs are described correctly
        """

        got_zones = self.ec2conn.get_all_zones()
        print dir(got_zones[0])
        self.assertEqual("default", got_zones[0].name)
