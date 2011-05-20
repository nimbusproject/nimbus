import sqlite3
import sys
import os
from socket import *
import logging
import pylantorrent
from pylantorrent.db import LantorrentDB
from pylantorrent.server import LTServer
from pylantorrent.client import LTClient
try:
    import json
except ImportError:
    import simplejson as json
import traceback
import uuid
import time
import datetime

def getrows(con):
    c = con.cursor()
    tm = datetime.datetime.now() - datetime.timedelta(0, pylantorrent.config.insert_delay)
    s = "select distinct src_filename from requests where state = 0 and attempt_count < 3 and entry_time < ? order by entry_time limit 1"
    data = (tm, )
    c.execute(s, data)
    r = c.fetchone()
    if r == None or len(r) < 1:
        return None
    src_file = r[0]
    # do a commit here.  THe assumption is that jsut one daemon is pulling
    # from the db.  better to grab any that came in since the initial
    # select
    con.commit()
    pylantorrent.log(logging.INFO, "selected rows with fname %s" % (src_file))

    #  need to find a way to determine that nothing new has been added for this
    #  file recently
    #s = 'select max(entry_time) as "e [timestamp]" from requests where src_filename = ? and state = 0 and attempt_count < 3'
    #data = (src_file, )
    #done = False
    #while not done:
    #    c.execute(s, data)
    #    row = c.fetchone()
    #    con.commit()
    #    td = datetime.datetime.now() - datetime.timedelta(0, 2)
    #    if row[0] < td:
    #        done = True
    #    else:
    #        time.sleep(0.1)

    s = "select hostname,port,src_filename,dst_filename,rid from requests where src_filename = ? and state = 0 and attempt_count < 3 order by hostname,port"
    data = (src_file, )
    c.execute(s, data)
    rows = c.fetchall()
    con.commit()
    return rows

def do_it_live(con, rows):

    pylantorrent.log(logging.INFO, "lan torrent daemon setting up to send %d in a group" % (len(rows)))

    c = con.cursor()
    dests = []
    last_host = None
    last_port = None
    json_dest = None
    rids_all = []
    for r in rows:
        new_host = r[0]
        new_port = int(r[1])
        dst_filename = r[3]
        src_filename = r[2]
        rid = r[4]
        rids_all.append(rid)
        sz = os.path.getsize(src_filename)
        # if it is the same host just tack on another dest file
        if new_host == last_host and last_port == new_port:
            reqs = json_dest['requests']
            new_req = {"filename" : dst_filename, "id" : rid, 'rename' : True}
            reqs.append(new_req)
            json_dest['requests'] = reqs
        else:
            if json_dest != None:
                dests.append(json_dest)
            last_host = new_host
            last_port = new_port

            json_dest = {}
            json_dest['host'] = new_host
            json_dest['port'] = new_port
            json_dest['requests'] = [{"filename" : dst_filename, "id" : rid, 'rename' : True}]
            json_dest['block_size'] = 128*1024
            json_dest['degree'] = 1
            json_dest['length'] = sz
    
    if json_dest != None:
        dests.append(json_dest)

    final = {}
    # for the sake of code resuse this will just be piped into an
    # lt daemon processor.  /dev/null is used to supress a local write
    final['requests'] = [{'filename' : "/dev/null", 'id' : str(uuid.uuid1()), 'rename' : False}]
    final['host'] = "localhost"
    final['port'] = 2893
    final['block_size'] = 131072
    final['degree'] = 1
    final['destinations'] = dests

    pylantorrent.log(logging.INFO, "request send %s" % (json.dumps(final, sort_keys=True, indent=4)))
    pylantorrent.log(logging.INFO, "sending em!")

    client = LTClient(src_filename, final)
    v = LTServer(client, client)
    try:
        v.store_and_forward()
    except Exception, ex:
        pylantorrent.log(logging.ERROR, "an error occured on store and forward: %s" % (str(ex)))
    rc = 0
    es = client.get_incomplete()
    bad_rid = []
    for k in es:
        rc = rc + 1
        e = es[k]
        pylantorrent.log(logging.ERROR, "error trying to send %s" % (str(e)))
        rid = e['id']
        bad_rid.append(rid)
        # set to retry
        u = "update requests set state = ?, message = ?, attempt_count = attempt_count + 1 where rid = ?"
        data = (0,str(e),rid,)
        c.execute(u, data)
        rids_all.remove(rid)

    for rid in rids_all:
        # set to compelte
        u = "update requests set state = ?, message = ? where rid = ?"
        data = (1,"Success",rid,)
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

    # use sqlaclh to make sure the db is there
    x = LantorrentDB("sqlite:///%s" % pylantorrent.config.dbfile)
    x.close()
    con_str = pylantorrent.config.dbfile
    #con = sqlite3.connect(con_str, isolation_level="EXCLUSIVE")
    con = sqlite3.connect(con_str, detect_types=sqlite3.PARSE_DECLTYPES|sqlite3.PARSE_COLNAMES)

    done = False
    while not done:
        try:
            rows = getrows(con)
            if rows and len(rows) > 0:
                do_it_live(con, rows)
            else:
                time.sleep(5)
        except Exception, ex:
            pylantorrent.log(logging.ERROR, "top level error %s" % (str(ex)))
            con = sqlite3.connect(con_str, detect_types=sqlite3.PARSE_DECLTYPES|sqlite3.PARSE_COLNAMES)

    return 0

if __name__ == "__main__":
    if 'LANTORRENT_HOME' not in os.environ:
        msg = "The env LANTORRENT_HOME must be set"
        print msg
        raise Exception(msg)
    rc = main()
    sys.exit(rc)
