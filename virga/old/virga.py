#!/usr/bin/env python

import sys
import os
from socket import *


def read_header(f):
    urls = []
    l = f.readline().strip()
    while l and l != "EOH":
        print l
        urls.append(l)
        l = f.readline().strip()
    return urls

def store_and_forward(fin, forward_url, filename, urls):
    print gethostname()

    if forward_url != None:
        fa = forward_url.split(":", 1)
        host = fa[0]
        pa = fa[1].split("/", 1)
        port = int(pa[0])
        forward_file = "/" + pa[1]
        print "host %s port %s" % (host, port)
        s = socket(AF_INET, SOCK_STREAM)
        s.connect((host, port))

        s.send(forward_file)
        s.send("\r\n")
        for u in urls:
            s.send(u)
            s.send("\r\n")
        s.send("EOH\r\n")
    else:
        s = None

    f = open(filename, "w")
    block_size = 1024*128

    data = fin.read(block_size)
    while data:
        if s != None:
            s.send(data)
        f.write(data)
        data = fin.read(block_size)

#    data = s.recv(block_size)
#    while data:
#        print data
#        data = s.recv(block_size)

    if s != None:
        s.close()
    f.close()


def main(argv=sys.argv[1:]):

    urls = read_header(sys.stdin)
    file_name = urls.pop(0)
    if len(urls) == 0:
        forward_url = None
    else:
        forward_url = urls.pop(0)
    print "filename %s forward %s" % (file_name, forward_url)
    store_and_forward(sys.stdin, forward_url, file_name, urls)

    return 0

if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

