from ConfigParser import SafeConfigParser
import string
import random
import pycb
from pycb.cumulus import *

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


def make_user():
    global user_count
    type = pycb.config.sec_type
    display_name = "test%d@nosetests.nimbus.org" % (user_count)
    user_count = user_count + 1

    id = random_string(21)
    pw = random_string(42)
    auth = pycb.config.auth
    auth.create_user(display_name, id, pw, None)

    return (id, pw)

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

def get_user():
    try:
        user = os.environ['CUMULUS_TEST_USER']
        password = os.environ['CUMULUS_TEST_PASSWORD']
        return (user, password)
    except:
        print "Must set the envs CUMULUS_TEST_USER, CUMULUS_TEST_PASSWORD"
        raise

