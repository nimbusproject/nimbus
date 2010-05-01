#!/usr/bin/python

import os.path
import string
import random
import os
import sys
import tempfile
from datetime import datetime, timedelta

import boto
from boto.s3.connection import S3Connection
from boto.s3.connection import OrdinaryCallingFormat
from boto.s3.connection import VHostCallingFormat
from boto.s3.connection import SubdomainCallingFormat
import sys
from ConfigParser import SafeConfigParser

my_char = '.'

def percent_cb(complete, total):
    sys.stdout.write(my_char)
    sys.stdout.flush()

def cb_random_bucketname(len):
    chars = string.letters + string.digits
    newpasswd = ""
    for i in range(len):
        newpasswd = newpasswd + random.choice(chars)
    return newpasswd


def cb_get_conn(s):
    cf = OrdinaryCallingFormat()

    hostname = s.get("cb", "hostname")
    p = int(s.get("cb", "port"))
    use_sec = False
    pw = s.get("tests", "pw")
    id = s.get("tests", "id")
    dbg = 0

    print "attempting to connect to %s:%d" % (hostname, p)
    conn = S3Connection(id, pw, host=hostname, port=p, is_secure=use_sec, debug=dbg, calling_format=cf)

    return conn

def create_bucket(conn):
    done = False
    while done == False:
        bucketname = cb_random_bucketname(10)
        try:
            bucket = conn.create_bucket(bucketname)
            done = True
        except:
            print "bucket name %s exists, trying again" % bucketname
            done = False

    return (bucketname, bucket)

def upload_file(con, filename, mds): 
    (bucketname,bucket) = create_bucket(con)
    k = boto.s3.key.Key(bucket)
    k.key = filename
    k.set_contents_from_filename(filename, md5=mds)
    return (k, bucket)

def time_upload(con, count, size, fname=None, mds=None):

    if fname == None:
        print "making a new file"
        (osf, filename) = tempfile.mkstemp()

        os.close(osf)
        f = open(filename, "w")
        for i in range(0, size):
            s = cb_random_bucketname(1024)
            f.write(s)
        f.close()
    else:
        filename = fname
        size = os.path.getsize(filename)

    try:
        start_tm = datetime.now()
        for i in range(0, count):
            upload_file(con, filename, mds)
        end_tm = datetime.now()

        delt = end_tm - start_tm

        us = float(delt.microseconds) / 1000000.0
        tm = float(delt.seconds) + us
    except:
        if fname == None:
            os.remove(filename)
        raise

    if fname == None:
        os.remove(filename)

    return (tm, size)
    

def main():
    settings=os.path.expanduser('~/.nimbus/cumulus.ini')

    s = SafeConfigParser()
    s.readfp(open(settings, "r"))
    con = cb_get_conn(s)

    its = int(sys.argv[1])
    kbs = int(sys.argv[2])
    fname = None
    if len(sys.argv) > 3:
        fname = sys.argv[3]
        mds = sys.argv[4]
    (tm, size) = time_upload(con, its, kbs, fname, mds)
    print tm / its


if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

