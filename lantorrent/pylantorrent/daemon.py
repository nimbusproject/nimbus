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

def getrows(con):
    c = con.cursor()
    tm = datetime.datetime.now() - datetime.timedelta(0, 5)
    s = "select distinct src_filename from requests where state = 0 and entry_time < ? limit 1"
    data = (tm, )
    c.execute(s, data)
    r = c.fetchone()
    if r == None or len(r) < 1:
        return None
    src_file = r[0]

    s = "select hostname,port,src_filename,dst_filename,rid from requests where src_filename = ? and state = 0"
    data = (src_file, )
    c.execute(s, data)
    rows = c.fetchall()
    con.commit()
    return rows

def do_it_live(con, rows):

    degree = 1
    maxd = 3

    while len(rows) > 0 and degree <= maxd:
        c = con.cursor()
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
        if degree > maxd:
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
            u = "update requests set state = ?, message = ? where rid = ? and group_id = ?"
            data = (state,str(e),rid,group_id,)
            c.execute(u, data)
        con.commit()

        if len(bad_rid) > 0:
            # wait for soemthing in the system to change
            # obviously we need something more sophisticated than this
            # eventually
            time.sleep(5)
    return rc


def main(argv=sys.argv[1:]):
    """
    This is the lantorrent daemon program.  it mines the db for transfers
    that it can group together and send.  Only one should be running at 
    one time
    """

    pylantorrent.log(logging.INFO, "enter %s" % (sys.argv[0]))

    con_str = pylantorrent.config.dbfile
    now = datetime.datetime.now()
    con = sqlite3.connect(con_str)

    done = False
    while not done:
        rows = getrows(con)
        if rows and len(rows) > 0:
            do_it_live(con, rows)
        else:
            time.sleep(5)

    return 0

if __name__ == "__main__":
    rc = main()
    sys.exit(rc)
