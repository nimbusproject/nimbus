#!/usr/bin/python

import os.path
import string
import random
import os
import sys
import tempfile
from datetime import datetime, timedelta
import threading
import boto
from boto.s3.connection import S3Connection
from boto.s3.connection import OrdinaryCallingFormat
from boto.s3.connection import VHostCallingFormat
from boto.s3.connection import SubdomainCallingFormat
import sys
from ConfigParser import SafeConfigParser

g_lock = threading.Lock()
g_ctr = 0
g_buckets = []
g_times = []
def percent_cb(complete, total):
    my_char = threading.current_thread().getName()
#    sys.stdout.write(my_char)
    sys.stdout.flush()

def cb_random_bucketname(len): 
    global g_ctr

    g_ctr = g_ctr + 1 
    chars = string.letters + string.digits
    newpasswd = ""
    for i in range(len - 1):
        newpasswd = newpasswd + random.choice(chars)
    bn = "%s%d%s" % (threading.current_thread().getName(), g_ctr, newpasswd)
    return bn

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
        bucketname = cb_random_bucketname(15)
        try:
            bucket = conn.create_bucket(bucketname)
            done = True
        except Exception as ex:
            print ex
            print "bucket name %s exists, trying again" % (bucketname)
            done = False

    return (bucketname, bucket)

def upload_file(con, file):
    (bucketname,bucket) = create_bucket(con)
    k = boto.s3.key.Key(bucket)
    k.key = cb_random_bucketname(5)
    k.set_contents_from_filename(file, cb=percent_cb, num_cb=10)
    return (k, bucket)

def time_upload(s, file):
    global g_buckets
    global g_lock
    global g_times

    con = cb_get_conn(s)
    try:
        my_char = threading.current_thread().getName()

        print "Starting thread %s" % (my_char)
        start_tm = datetime.now()
        (k, b) = upload_file(con, file)
        end_tm = datetime.now()

        delt = end_tm - start_tm
        us = float(delt.microseconds) / 1000000.0
        tm = float(delt.seconds) + us

        g_lock.acquire()
        try:
            g_buckets.append((b,k))
            g_times.append(tm)
        finally:
            g_lock.release()
    except:
        raise

    print tm
    

def main():
    global g_buckets
    global g_times
    global data
    settings=os.path.expanduser('~/.nimbus/cumulus.ini')

    s = SafeConfigParser()
    s.readfp(open(settings, "r"))

    its = int(sys.argv[1])
    file = sys.argv[2]
    size = os.path.getsize(file)
    print "file %s of %d bytes" % (file, size)

    start_tm = datetime.now()

    t_a = []
    char = "abcdefghijklmnopqrstuvvxyz"
    for i in range(0, its):
        t = threading.Thread(target=time_upload, args=(s, file), name=char[i%len(char)])
        t_a.append(t)
        t.start()

    for i in range(0, its):
        print "joining %s" % (t_a[i].getName())
        t_a[i].join()

    end_tm = datetime.now()
    delt = end_tm - start_tm

    us = float(delt.microseconds) / 1000000.0
    tm = float(delt.seconds) + us

    for (b,k) in g_buckets:
        try:
            k.delete()
        except:
            pass
        b.delete()
    max = -1.0
    min = 99999999.0
    for tm in g_times:
        if tm > max:
            max = tm
        if tm < min:
            min = tm

    sizeMB = size * 1024*1024
    total_bytes = size * its 
    total_bw = float(total_bytes) / float(tm)
    avg_tm = float(tm) / float(its)
    avg_bw = float(total_bw) / float(its)
    print "clients,sizeMB,totalMB,totaltime,totalMBps,avgtm,avgbw,max,min" 
    print "%d,%d,%d,%f,%f,%f,%f,%f,%f" %(its, size, total_bytes, tm, total_bw, avg_tm, avg_bw, max, min)
    
    for tm in g_times:
        sys.stdout.write(tm)
        sys.stdout.write(",")
    sys.stdout.write("\n")

if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

