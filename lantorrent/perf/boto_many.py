#!/usr/bin/python


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
import filecmp
import logging
import shlex
from optparse import SUPPRESS_HELP
import boto
from boto.s3.connection import OrdinaryCallingFormat
from boto.s3.connection import VHostCallingFormat
from boto.s3.connection import SubdomainCallingFormat
from boto.s3.connection import S3Connection
from boto.ec2.connection import EC2Connection


def main(argv=sys.argv[1:]):

    host = argv[0]
    imagename = argv[1]
    count = int(argv[2])

    print "setting up ec2 connection"
    ec2conn = EC2Connection(s3id, pw, host=host, port=8444, debug=2)
    ec2conn.host = host

    print "getting a reference to the remote image"
    image = ec2conn.get_image(imagename)
    print "running %d instances" % (count)
    res = image.run(min_count=count, max_count=count)
    print "stoping all instances"
    res.stop_all()

    return 0

if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

