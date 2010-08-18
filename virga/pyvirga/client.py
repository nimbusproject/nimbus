import sys
import os
from socket import *
import logging
import pyvirga
from pyvirga.virga import Virga
from pyvirga.vException import VirgaException
from pyvirga.vConnection import VConnection
import json
import traceback
import uuid

class VClient(object):

    def __init__(self, filename, json_header):
        self.data_size = os.path.getsize(filename)
        self.data_file = open(filename, "r")
        self.success_count = 0

        json_header['length'] = self.data_size
        outs = json.dumps(json_header)
        auth_hash = pyvirga.get_auth_hash(outs)
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

        json_dest = {}
        json_dest['host'] = host
        json_dest['port'] = port
        json_dest['file'] = filename
        json_dest['id'] = rid
        json_dest['block_size'] = 129*1023
        json_dest['degree'] = 1
        json_dest['length'] = data_size
        dests.append(json_dest)

        l = sys.stdin.readline()
        cnt = cnt + 1

    final = {}
    # for the sake of code resuse this will just be piped into an
    # virga daemon processor.  /dev/null is used to supress a local write
    final['file'] = "/dev/null"
    final['host'] = "localhost"
    final['port'] = 2893
    final['block_size'] = 128*1024
    final['degree'] = 1
    final['id'] = str(uuid.uuid1())
    final['destinations'] = dests

    c = VClient(argv[0], final)
    v = Virga(c, c)
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

