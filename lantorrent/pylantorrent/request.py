import sqlite3
from socket import *
import logging
import pylantorrent
from pylantorrent.db import LantorrentDB
from pylantorrent.server import LTServer
from pylantorrent.client import LTClient
import os
try:
    import json
except ImportError:
    import simplejson as json
import time
import random
import datetime
from pylantorrent import cbOpts
import sys

def setup_options(argv):

    u = """[options]
Submit a transfer request
    """
    (parser, all_opts) = pylantorrent.get_default_options(u)

    opt = cbOpts("nonblock", "n", "Do not block waiting for completion", False, flag=True)
    all_opts.append(opt)
    opt = cbOpts("reattach", "a", "Reattach", None)
    all_opts.append(opt)

    (o, args) = pylantorrent.parse_args(parser, all_opts, argv)
    return (o, args, parser)


def wait_until_sent(con, rid):
    done = False
    while not done:
        (done, rc, message) = is_done(con, rid)
        if not done:
            time.sleep(5)
    return (rc, message)

#
def is_done(con, rid):
    error_cnt = 0
    while True:
        try:
            pylantorrent.log(logging.INFO, "checking for done on  %s" % (rid))
            done = False
            rc = 0
            s = "select state,message,attempt_count from requests where rid = ?"
            data = (rid,)
            c = con.cursor()
            c.execute(s, data)
            rs = c.fetchone()
            con.commit()
            state = int(rs[0])
            message = rs[1]
            attempt_count = rs[2]
            if state == 1:
                done = True
            elif attempt_count > 2:
                done = True
                rc = 1
                if message == None:
                    message = "too many attempts %d" % (attempt_count)
            con.commit()
            return (done, rc, message)
        except sqlite3.OperationalError, sqlex:
            error_cnt = error_cnt + 1
            if error_cnt >= pylantorrent.config.db_error_max:
                raise sqlex
            time.sleep(random.random() * 2.0)

def delete_rid(con, rid):
    error_cnt = 0
    while True:
        try:
            # cleanup
            c = con.cursor()
            d = "delete from requests where rid = ?"
            data = (rid,)
            c = con.cursor()
            c.execute(d, data)
            con.commit()
            return 
        except sqlite3.OperationalError, sqlex:
            error_cnt = error_cnt + 1
            if error_cnt >= pylantorrent.config.db_error_max:
                raise sqlex
            time.sleep(random.random() * 2.0)

def request(argv, con):
    if len(argv) < 4:
        raise Exception("You must provide 4 arguments: <src file> <dst file> <a uuid for this request> <the contanct string of the receiving nodes lt server>")
    src_filename = argv[0]
    dst_filename = argv[1]
    # the user provides the rid.  that way we know they have it to look
    # things up later if needed
    rid = argv[2]

    # get the size of the file and verify that it exists
    sz = os.path.getsize(src_filename)

    hostport = argv[3]
    ha = hostport.split(":")
    host = ha[0]
    if host == "":
        hostport = os.environ['SSH_CLIENT']
        ha2 = hostport.split(" ")
        host = ha2[0]
    if len(ha) > 1:
        port = int(ha[1])
    else:
        port = 2893

    now = datetime.datetime.now()
    i = "insert into requests(src_filename, dst_filename, hostname, port, rid, entry_time, state, attempt_count) values (?, ?, ?, ?, ?, ?, ?, ?)"
    data = (src_filename, dst_filename, host, port, rid, now, 0, 0, )

    error_ctr = 0
    while True:
        try:
            c = con.cursor()
            c.execute(i, data)
            con.commit()
            pylantorrent.log(logging.INFO, "new request %s %d" % (rid, sz))
            return (rid, sz)
        except Exception, ex:
            pylantorrent.log(logging.ERROR, "an error occured on request %s" % str(ex))
            error_ctr = error_ctr + 1
            if error_ctr >= pylantorrent.config.db_error_max:
                raise ex
            time.sleep(random.random() * 2.0)

    # should never get here


def main(argv=sys.argv[1:]):
    """
    This program allows a file to be requested from the lantorrent system.  The
    file will be sent out of band.  When the file has been delived the 
    database entry for this request will be updated.  This program will
    block until that entry is update.

    As options, the program takes the source file, the
    target file location, the group_id and the group count.

    The lantorrent config file must have the ip and port that the requester
    is using for lantorrent delivery.
    """

    pylantorrent.log(logging.INFO, "enter")
    random.seed()

    (o, args, p) = setup_options(argv)

    # use sqlaclh to make sure the db is there
    x = LantorrentDB("sqlite:///%s" % pylantorrent.config.dbfile)
    x.close()
    
    con_str = pylantorrent.config.dbfile
    con = sqlite3.connect(con_str, isolation_level="EXCLUSIVE")

    rc = 0
    sz = -1
    done = False
    message = ""
    if o.reattach == None:
        (rid, sz) = request(args, con)
        try:
            (done, rc, message) = is_done(con, rid)
        except:
            done = False
            rc = 0
            message = "Check on status later, db not ready for polling"
    else:
        rid = o.reattach
        (done, rc, message) = is_done(con, rid)

    if not o.nonblock and not done:
        (rc, message) = wait_until_sent(con, rid)
        done = True

    if done:
        delete_rid(con, rid)

    msg = "%d,%s,%s" % (rc, str(done), message)
    print msg

    return rc


if __name__ == "__main__":
    if 'LANTORRENT_HOME' not in os.environ:
        msg = "The env LANTORRENT_HOME must be set"
        print msg
        raise Exception(msg)

    rc = main()
    # always return 0.  an non 0 return code will mean an ssh error
    sys.exit(0)
