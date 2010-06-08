#!/usr/bin/python

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
from pycb.cumulus import *


def main(argv=sys.argv[1:]):

    # make the user that will create the dir
    id = pycb.test_common.random_string(21)
    pw = pycb.test_common.random_string(42)
    auth = pycb.config.auth

    display_name = "repobucketcreator@nimbusproject.org"
    auth.create_user(display_name, id, pw, None)

    print "id: %s" % (id)
    print "pw: %s" % (pw)

    conn = pycb.test_common.cb_get_conn(pycb.config.hostname, pycb.config.port, id, pw)

    bucket = conn.create_bucket("Repo", policy='public-read-write')
    



if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

