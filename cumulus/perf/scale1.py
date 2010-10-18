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
g_bucket = "testbucket"
g_times = []
g_hostname = "c1.uc.futuregrid.org"
g_port = 8888
g_id = "y4OHqRh8PM10Khy7QuGmz" 
g_pw = "QuqputK6HldcJxF0LnstOHvYM77qLJUfxYp7vGMbvU"

def cb_get_conn(hostname, port):
    cf = OrdinaryCallingFormat()
    conn = S3Connection(g_id, g_pw, host=hostname, port=port, is_secure=False, calling_format=cf)

    return conn

def upload_file(filename):
    conn = cb_get_conn(g_hostname, g_port)
    try:
        bucket = conn.create_bucket(g_bucket)
    except:
        pass

    k = boto.s3.key.Key(bucket)
    k.key = str(uuid.uuid1()).replace("-", "")
    k.set_contents_from_filename(filename, cb=percent_cb, num_cb=10)

    return k.key

def time_upload(file):
    global g_lock
    global g_times

    con = cb_get_conn(s)
    try:
        my_char = threading.current_thread().getName()
        start_tm = datetime.now()
        (k, b) = upload_file(file)
        end_tm = datetime.now()

        delt = end_tm - start_tm
        us = float(delt.microseconds) / 1000000.0
        tm = float(delt.seconds) + us

        g_lock.acquire()
        try:
            g_times.append(tm)
        finally:
            g_lock.release()
    except:
        raise
    

def main():
    global g_times
    global data
    settings=os.path.expanduser('~/.nimbus/cumulus.ini')

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

    print "cleaning up the keys"
    conn = cb_get_conn(g_hostname, g_port)
    bucket = conn.get_bucket(g_bucket)
    rs = bucket.list()
    for k in rs:
        try:
            k.delete()
        except:
            pass

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

