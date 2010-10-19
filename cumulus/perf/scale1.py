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
import uuid

g_lock = threading.Lock()
g_ctr = 0
g_bucket = "ScaleTest"
g_key_name = "100MB"
g_times = []
g_hostname = "c1.uc.futuregrid.org"
g_port = 8888
g_id = "5xecKocxGi2DoCppttrnU"
g_pw = "XXm4jIim2rw16QiiqyvB8rHt409pFUBxHm3oMXGDz6"

def cb_get_conn(hostname, port):
    cf = OrdinaryCallingFormat()
    conn = S3Connection(g_id, g_pw, host=hostname, port=port, is_secure=False, calling_format=cf, debug=15)

    return conn

def upload_file(filename):
    global g_bucket
    global g_key_name
    conn = cb_get_conn(g_hostname, g_port)
    try:
        bucket = conn.get_bucket(g_bucket)
    except Exception, ex:
        bucket = conn.create_bucket(g_bucket)

    k = boto.s3.key.Key(bucket)
    k.key = g_key_name
    k.get_contents_to_filename(filename)

    return k.key

def time_upload(file):
    global g_lock
    global g_times

    try:
        my_char = threading.current_thread().getName()
        start_tm = datetime.now()
        #key = upload_file(file+"."+my_char)
        key = upload_file(file)
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

    start_tm = datetime.now()

    t_a = []
    char = "abcdefghijklmnopqrstuvvxyz1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    for i in range(0, its):
        t = threading.Thread(target=time_upload, args=(file,), name=char[i%len(char)])
        t_a.append(t)
        t.start()

    for i in range(0, its):
        t_a[i].join()

    end_tm = datetime.now()
    delt = end_tm - start_tm

    us = float(delt.microseconds) / 1000000.0
    tm = float(delt.seconds) + us

#    conn = cb_get_conn(g_hostname, g_port)
#    bucket = conn.get_bucket(g_bucket)
#    rs = bucket.list()
#    for k in rs:
#        try:
#            k.delete()
#        except:
#            pass

    max = -1.0
    min = 99999999.0
    for tm in g_times:
        if tm > max:
            max = tm
        if tm < min:
            min = tm

    #size = os.path.getsize(file)
    size = 0
    sizeMB = size * 1024*1024
    total_bytes = size * its 
    total_bw = float(total_bytes) / float(tm)
    avg_tm = float(tm) / float(its)
    avg_bw = float(total_bw) / float(its)
    print "clients,sizeMB,totalMB,totaltime,totalMBps,avgtm,avgbw,max,min" 
    print "%d,%d,%d,%f,%f,%f,%f,%f,%f" %(its, size, total_bytes, tm, total_bw, avg_tm, avg_bw, max, min)
    
    for tm in g_times:
        sys.stdout.write(str(tm))
        sys.stdout.write(",")
    sys.stdout.write("\n")

if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

