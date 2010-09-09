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
import pynimbusauthz
from pynimbusauthz.cmd_opts import cbOpts

def setup_options(argv):

    u = """[options]
Submit a transfer request
    """
    (parser, all_opts) = pynimbusauthz.get_default_options(u)

    opt = cbOpts("nonblock", "n", "Do not block waiting for completion", False, flag=True)
    all_opts.append(opt)
    opt = cbOpts("reattach", "a", "Reattach", None)
    all_opts.append(opt)

    (o, args) = pynimbusauthz.parse_args(parser, all_opts, argv)
    return (o, args, parser)


def wait_until_sent(con, rid):
    done = False
    while not done:
        s = "select state,message,attempt_count from requests where rid = ?"
        data = (rid,)
        c = con.cursor()
        c.execute(s, data)
        rs = c.fetchone()
        con.commit()
        state = int(rs[0])
        message = rs[1]
        attempt_count = rs[2]
        if attempt_count > 2:
            done = True
            state = 2
            if message == None:
                message = "too many attempts %d" % (attempt_count)
        elif state == 1:
            done = True
        else:
            time.sleep(1)

    # cleanup
    d = "delete from requests where rid = ?"
    data = (rid,)
    c = con.cursor()
    c.execute(d, data)
    con.commit()

    if state == 1:
        rc = 0
    else:
        rc = 1
    return (rc, message)


def request(argv, con):
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
        port = 2893

    now = datetime.datetime.now()
    i = "insert into requests(src_filename, dst_filename, hostname, port, rid, entry_time) values (?, ?, ?, ?, ?, ?)"
    data = (src_filename, dst_filename, host, port, rid, now,)

    c = con.cursor()
    c.execute(i, data)
    con.commit()

    return rid


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

    (o, args, p) = setup_options(argv)

    con_str = pylantorrent.config.dbfile
    con = sqlite3.connect(con_str, isolation_level="EXCLUSIVE")

    rc = 0
    if o.reattach == None:
        rid = request(args, con)
    else:
        rid = o.reattach

    if not o.nonblock:
        (rc, message) = wait_until_sent(con, rid)
    else:
        msg = "request id: %s" % (rid)
        pynimbusauthz.print_msg(o, 0,  msg)

    if rc == 0:
        pynimbusauthz.print_msg(o, 0,  "Success")
    else:
        msg = "Failure: %s" % (message)
        pynimbusauthz.print_msg(o, 0,  msg)

    return rc


if __name__ == "__main__":
    rc = main()
    sys.exit(rc)
