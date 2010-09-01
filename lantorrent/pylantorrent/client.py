import sys
import os
from socket import *
import logging
import pylantorrent
from pylantorrent.server import LTServer
from pylantorrent.ltException import LTException
import json
import traceback
import uuid

class LTClient(object):

    def __init__(self, filename, json_header):
        self.data_size = os.path.getsize(filename)
        self.data_file = open(filename, "r")
        self.success_count = 0

        json_header['length'] = self.data_size
        outs = json.dumps(json_header)
        auth_hash = pylantorrent.get_auth_hash(outs)
        self.header_lines = outs.split("\n")
        self.header_lines.append("EOH : %s" % (auth_hash))
        self.errors = []

        self.dest = {}
        ld = json_header['destinations']
        for d in ld:
            d['emsg'] = None
            self.dest[d['id']] = d

    def readline(self):
        if len(self.header_lines) == 0:
            return None
        l = self.header_lines.pop(0)
        return l

    def read(self, blocksize):
        return self.data_file.read(blocksize)

    def write(self, data):
        try:
            json_outs = json.loads(data)
            rid = json_outs['id']
            if int(json_outs['code']) == 0:
                self.dest.pop(rid)
                self.success_count = self.success_count + 1
            else:
                d = self.dest[rid]
                d['emsg'] = json_outs
        except Exception, ex:
            pass

    def get_incomplete(self):
        return self.dest


def main(argv=sys.argv[1:]):
    
    dests = [] 
    cnt = 1
    l = sys.stdin.readline()
    data_size = os.path.getsize(argv[0])
    while l:
        # each line is a url to be broken down
        a = l.split(":", 1)
        if len(a) != 2:
            raise Exception("url %d not properly formatted: %s" % (cnt, l))
        host = a[0]
        l = a[1]
        a = l.split("/", 1)
        if len(a) != 2:
            raise Exception("url %d not properly formatted: %s" % (cnt, l))
        port = a[0]
        x = int(port)
        filename = "/" + a[1].strip()
        rid = str(uuid.uuid1())

        json_dest = pylantorrent.create_endpoint_entry(host, filename, data_size, port, block_size, degree, rid)
        dests.append(json_dest)

        l = sys.stdin.readline()
        cnt = cnt + 1

    # for the sake of code resuse this will just be piped into an
    # lt daemon processor.  /dev/null is used to supress a local write
    final = pylantorrent.create_endpoint_entry("localhost", "/dev/null")
    final['destinations'] = dests

    c = LTClient(argv[0], final)
    v = LTServer(c, c)
    v.store_and_forward()

    es = c.get_incomplete()
    for k in es:
        e = es[k]
        if e['emsg'] == None:
            e['message'] = "Unknown error.  Please retry"
        else:
            e = e['emsg']
        print "ERROR: %s:%s%s %s" % (e['host'], e['port'], e['file'], e['message'])       
    print "Succesfully sent to %d" % (c.success_count)

    return 0

if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

