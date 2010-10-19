import os
import sys
import random
from pycb.cbException import cbException
import pycb
import stat
import urllib
import glob
import errno
import logging
import threading
import tempfile
import hashlib
import traceback
import time
import pycb

class cbRedirectorIface(object):

    # return new host direction or None
    def new_connection(self, request):
        return None

    # called when a connection is closed
    def end_connection(self, request):
        pass


class cbBasicRedirector(object):

    def __init__(self, parser):
        self.connection_count = 0
        self.host_file = parser.get("load_balanced", "hostfile")
        self.max = int(parser.get("load_balanced", "max"))

    def new_connection(self, request):
        h = None
        self.connection_count = self.connection_count + 1
        if self.connection_count > self.max:
            h = self.get_next_host()
        return h

    def end_connection(self, request):
        self.connection_count = self.connection_count - 1
 
    def get_next_host(self):
        try:
            hosts = []
            f = open(self.host_file, "r")
            for l in f.readlines():
                hosts.append(l.strip())
            f.close()

            my_host = "%s:%d" % (pycb.config.hostname, pycb.config.port)
            for i in range(0, 5):
                ndx = random.randint(0, len(hosts)-1)
                h = hosts[ndx]
                pycb.log(logging.INFO, "redirector found %s, my host %s" % (h, my_host))
                if h != my_host:
                    return h
            return None
        except Exception, ex:
            pycb.log(logging.ERROR, "get next host error %s" % (str(ex)))
            return None
 
