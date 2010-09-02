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

def getrows(c, group_id):
    s = "select hostname,port,src_filename,dst_filename,rid from requests where group_id = ? and state = 0"
    data = (group_id, )
    c.execute(s, data)
    rows = c.fetchall()
    return rows

def do_it_live(con, group_id):

    degree = 1
    maxd = 3

    c = con.cursor()
    rows = getrows(c, group_id)
    while len(rows) > 0 and degree <= maxd:
        dests = []
        for r in rows:
            src_filename = r[2]
            dst_filename = r[3]
            sz = os.path.getsize(src_filename)
            json_dest = {}
            json_dest['host'] = r[0]
            json_dest['port'] = int(r[1])
            json_dest['file'] = dst_filename
            json_dest['id'] = r[4]
            json_dest['block_size'] = 128*1024
            json_dest['degree'] = degree
            json_dest['length'] = sz
            dests.append(json_dest)

        final = {}
        # for the sake of code resuse this will just be piped into an
        # lt daemon processor.  /dev/null is used to supress a local write
        final['file'] = "/dev/null"
        final['host'] = "localhost"
        final['port'] = 2893
        final['block_size'] = 131072
        final['degree'] = 1
        final['id'] = str(uuid.uuid1())
        final['destinations'] = dests

        pylantorrent.log(logging.INFO, "request send %s" % (str(final)))
        pylantorrent.log(logging.INFO, "sending em!")

        client = LTClient(src_filename, final)
        v = LTServer(client, client)
        v.store_and_forward()

        u = "update requests set state = ? where group_id = ?"
        data = (1,group_id,)
        c.execute(u, data)
        state = 0
        degree = degree + 1
        if degree == maxd:
            state = 2
        rc = 0
        es = client.get_incomplete()
        bad_rid = []
        for k in es:
            rc = rc + 1
            e = es[k]
            if state != 2:
                pylantorrent.log(logging.WARNING, "error trying to send %s" % (str(e)))
            else:
                pylantorrent.log(logging.ERROR, "error trying to send %s" % (str(e)))
            rid = e['id']
            bad_rid.append(rid)
            u = "update requests set state = ? where rid = ? and group_id = ?"
            data = (state,rid,group_id,)
            c.execute(u, data)
        con.commit()
    return rc


def wait_until_sent(con, host, port, group_id):
    done = False
    while not done:
        s = "select state from requests where group_id = ? and hostname = ? and port = ?"
        data = (group_id,host,port,)
        c = con.cursor()
        c.execute(s, data)
        rs = c.fetchone()
        con.commit()
        state = int(rs[0])
        pylantorrent.log(logging.INFO, "my state %d" % (state))
        if state == 0:
            time.sleep(1)
        else:
            return state

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
    group_id = argv[2]
    group_count = int(argv[3])
    # the user provides the rid.  that way we know they have it to look
    # things up later if needed
    rid = argv[4]

    hostport = argv[5]
    ha = hostport.split(":")
    host = ha[0]
    if host == "":
        hostport = os.environ['SSH_CLIENT']
        ha = hostport.split(" ")
        host = ha[0]
    if len(ha) > 1:
        port = int(ha[1])
    else:
        port = 5893

    con_str = pylantorrent.config.dbfile
    now = datetime.datetime.now()
    con = sqlite3.connect(con_str)

    i = "insert into requests(src_filename, dst_filename, group_id, group_count, hostname, port, rid, entry_time) values (?, ?, ?, ?, ?, ?, ?, ?)"
    data = (src_filename, dst_filename, group_id, group_count, host, port, rid, now,)

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

    if cnt == group_count:
        do_it_live(con, group_id)

    rc = wait_until_sent(con, host, port, group_id)

    return rc

if __name__ == "__main__":
    rc = main()
    sys.exit(rc)
