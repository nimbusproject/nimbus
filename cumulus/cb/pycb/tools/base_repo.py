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
import pynimbusauthz
import unittest
import tempfile
import filecmp
from pycb.cumulus import *
from boto.s3.connection import OrdinaryCallingFormat
from boto.s3.connection import S3Connection


def setup_options(argv):

    u = """[options] <admin name> <repo dir>
Create a base repository directory with public write permissions
    """
    (parser, all_opts) = pynimbusauthz.get_default_options(u)

    (o, args) = pynimbusauthz.parse_args(parser, all_opts, argv)

    if len(args) < 2:
        pynimbusauthz.parse_args(parser, all_opts, ["--help"])

    return (o, args)


def main(argv=sys.argv[1:]):

    (o, args) = setup_options(argv)
    # make the user that will create the dir
    id = pycb.test_common.random_string(21)
    pw = pycb.test_common.random_string(42)
    auth = pycb.config.auth

    display_name = args[0]
    repo_dir = args[1]
    auth.create_user(display_name, id, pw, None)

    print "admin s3 id is: %s" % (id)
    print "admin s3 pw:    %s" % (pw)

    cf = OrdinaryCallingFormat()
    conn = S3Connection(id, pw, host=pycb.config.hostname, port=pycb.config.port, is_secure=pycb.config.use_https, calling_format=cf)

    bucket = conn.create_bucket(repo_dir, policy='public-read-write')
    



if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

