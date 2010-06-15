from ConfigParser import SafeConfigParser
import string
import random
import pycb
from pycb.cumulus import *
from boto.s3.connection import OrdinaryCallingFormat
from boto.s3.connection import S3Connection

user_count = 0

def start_server():
    pycb.config.parse_cmdline(["-p", "0"])
    cumulus = CumulusRunner()
    port = self.cumulus.getPort()
    host = "localhost"
    return (cumulus, host, port)

def stop_server():
    try:
        self.cumulus.stop()
    except:
        pass

# generate random string
def random_string(len):
    chars = string.letters + string.digits
    newpasswd = ""
    for i in range(len):
        newpasswd = newpasswd + random.choice(chars)
    return newpasswd

def cb_get_conn(host, port, id, pw):
    sec = False
    try:
        https = os.environ['CUMULUS_TEST_HTTPS']
        if https != None:
            sec = True
    except:
        pass
    cf = OrdinaryCallingFormat()
    conn = S3Connection(id, pw, host=host, port=port, is_secure=sec, calling_format=cf)
    return conn


def make_user():
    global user_count
    type = pycb.config.sec_type
    display_name = "test%d%s@nosetests.nimbus.org" % (user_count, random_string(10))
    user_count = user_count + 1

    id = random_string(21)
    pw = random_string(42)
    auth = pycb.config.auth
    auth.create_user(display_name, id, pw, None)

    return (id, pw)

def set_user_quota(id, quota):
    auth = pycb.config.auth
    u = auth.get_user(id)
    u.set_quota(quota)

def clean_user(id):
    auth = pycb.config.auth
    u = auth.get_user(id)
    u.remove_user()

def get_contact():
    try:
        host = os.environ['CUMULUS_TEST_HOST']
        port = int(os.environ['CUMULUS_TEST_PORT'])
        return (host, port)
    except:
        print "Must set the envs CUMULUS_TEST_HOST, CUMULUS_TEST_PORT"
        raise

