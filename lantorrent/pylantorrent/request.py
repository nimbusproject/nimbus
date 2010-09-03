import sqlite3
import sys
import os
from socket import *
import logging
import pylantorrent
from pylantorrent.server import LTServer
from pylantorrent.client import LTClient
import json
import traceback
import uuid
import time
import datetime

def wait_until_sent(con, rid):
    done = False
    while not done:
        s = "select state,messsage from requests where rid = ?"
        data = (rid,)
        c = con.cursor()
        c.execute(s, data)
        rs = c.fetchone()
        con.commit()
        state = int(rs[0])
        message = rs[1]
        if state == 0:
            time.sleep(1)
        else:
            return (state, message)

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

    src_filename = argv[0]
    dst_filename = argv[1]
    # the user provides the rid.  that way we know they have it to look
    # things up later if needed
    rid = argv[2]

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
        port = 5893

    con_str = pylantorrent.config.dbfile
    now = datetime.datetime.now()
    con = sqlite3.connect(con_str)

    i = "insert into requests(src_filename, dst_filename, hostname, port, rid, entry_time) values (?, ?, ?, ?, ?, ?)"
    data = (src_filename, dst_filename, host, port, rid, now,)

    c = con.cursor()
    c.execute(i, data)
    con.commit()

    s = "select count(*) from requests where group_id = ?"
    data = (group_id,)
    c = con.cursor()
    c.execute(s, data)
    rs = c.fetchone()
    con.commit()
    cnt = int(rs[0])

    (rc, message) = wait_until_sent(con, rid)
    if rc == 0:
        print "Success"
    else:
        print "Failure: %s" % (message)

    return rc

if __name__ == "__main__":
    rc = main()
    sys.exit(rc)
