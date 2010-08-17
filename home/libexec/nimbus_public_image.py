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
import pynimbusauthz
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


g_report_options = ["cert", "key", "dn", "canonical_id", "access_id", "access_secret", "url", "web_id", "cloud_properties"]

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
    if not os.path.exists(nimbus_home):
        raise CLIError('ENIMBUSHOME', "NIMBUS_HOME must refer to a valid path")
    return nimbus_home

def setup_options(argv):

    u = """[options] <filename> <image name>
Upload an image to the public repository
    """
    (parser, all_opts) = pynimbusauthz.get_default_options(u)
    opt = cbOpts("repo", "r", "The bucket where cloud client images are stored", "Repo")
    all_opts.append(opt)
    opt = cbOpts("prefix", "p", "The prefix used for images in the cloud client bucket", "VMS")
    all_opts.append(opt)

    (o, args) = pynimbusauthz.parse_args(parser, all_opts, argv)

    # def verify_opts(o, args, parser):
    if len(args) != 2:
        pynimbusauthz.parse_args(parser, [], ["--help"])

    return (o, args, parser)

def main(argv=sys.argv[1:]):

    try:
        (o, args, p) = setup_options(argv)
        filename = args[0]
        imagename = args[1]

        bucket = conn.get_bucket(o.repo)

        k = boto.s3.key.Key(bucket)
        key = "%s/common/%s" % (o.prefix, imagename)
        k.key = key
        k.set_contents_from_filename(filename, policy='public-read')

    except CLIError, clie:
        if DEBUG:
            traceback.print_exc(file=sys.stdout)
        
        print clie
        return clie.get_rc()

    return 0

if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

