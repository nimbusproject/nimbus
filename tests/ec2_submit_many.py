#!/usr/bin/python

"""
This program allows you to upload a public image to the nimber repository
"""
import sys
import string
import random
import os
import sys
import sys
import ConfigParser
from ConfigParser import SafeConfigParser
import time
import tempfile
import traceback
import logging
import shlex
from optparse import SUPPRESS_HELP
import boto
from boto.s3.connection import OrdinaryCallingFormat
from boto.s3.connection import VHostCallingFormat
from boto.s3.connection import SubdomainCallingFormat
from boto.s3.connection import S3Connection
from boto.ec2.connection import EC2Connection

DEBUG = False

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
    return nimbus_home

def main(argv=sys.argv[1:]):

    try:
        imagename = argv[0]

        try:
            s = SafeConfigParser()
            s3cfg = os.getenv("HOME") + "/.s3cfg"
            s.readfp(open(s3cfg, "r"))
            s3id = s.get("default", "access_key")
            pw = s.get("default", "secret_key")
            host_base = s.get("default", "host_base")
            use_https = s.getboolean("default", "use_https")

            hba = host_base.split(":", 1)
            if len(hba) == 2:
                port = int(hba[1])
            else:
                port = 8888
            host = hba[0]
        except Exception, ex:
            print "This program uses the s3cmd configuration file ~/.s3cfg"
            print ex
            sys.exit(1)

        print "getting connection"
        ec2port = 8444
        try:
            ec2port = int(os.environ['NIMBUS_TEST_EC2_PORT'])
        except:
            pass

        ec2conn = EC2Connection(s3id, pw, host='locahost', port=ec2port, debug=2)
        ec2conn.host = 'localhost'
        print "getting image"
        image = ec2conn.get_image(imagename)
        print "running"
        res = image.run(min_count=2, max_count=4)
        res.stop_all()

    except:
        raise

    return 0

if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

