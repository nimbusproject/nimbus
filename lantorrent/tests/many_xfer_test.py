import string
import random
import os
import sys
import nose.tools
import sys
import time
import unittest
import tempfile
import filecmp
import uuid
import traceback
from pylantorrent.client import *
from pylantorrent.server import *



class TestManyXfer(unittest.TestCase):

    def setUp(self):
        self.src_file = "/etc/group"
        self.src_size = os.path.getsize(self.src_file)
        self.ports_a = os.environ['LANTORRENT_TEST_PORTS'].split(",")
        self.files = []

    def tearDown(self):
        while len(self.files) > 0:
            f = self.files.pop(0)
            os.remove(f)

    def _t_new_dest(self, host, port, sz=128*1024): 
        (osf, fname) = tempfile.mkstemp()
        self.files.append(fname)

        ent = pylantorrent.create_endpoint_entry(host, [fname], self.src_size, port=int(port), block_size=sz)

        os.close(osf)
        return (fname, ent)

    def _t_file_compare(self, f):
        rc = filecmp.cmp(self.src_file, f)
        self.assertTrue(rc, "%s not the same as %s" % (self.src_file, f))


    def _t_build_list(self):
        new_files = []
        dests = []
        for port in self.ports_a:
            (fname, ent) = self._t_new_dest("localhost", port)
            dests.append(ent)
            new_files.append(fname)
            print "sending to %s" % (fname)

        top = dests.pop(0)
        top['destinations'] = dests
        print top

        return (top, new_files)


    def send_to_all_test(self):
        (top, new_files) = self._t_build_list()

        c = LTClient(self.src_file, top)
        v = LTServer(c, c)
        v.store_and_forward()

        for f in new_files:
            self._t_file_compare(f)
            print f

    def fail_end_test(self):
        (top, new_files) = self._t_build_list()
        dest = top['destinations']

        (fname, bad_ent) = self._t_new_dest("notahost", 5150)
        dest.append(bad_ent)
        top['destinations'] = dest

        c = LTClient(self.src_file, top)
        v = LTServer(c, c)
        v.store_and_forward()

        # even tho there is a bad file in the mix, all the good ones should
        # work
        for f in new_files:
            self._t_file_compare(f)
            print f

    def fail_top_test(self):
        (top, new_files) = self._t_build_list()
        dest = top['destinations']

        (fname, bad_ent) = self._t_new_dest("notahost", 5150)
        dest.insert(0, bad_ent)
        top['destinations'] = dest

        c = LTClient(self.src_file, top)
        v = LTServer(c, c)
        v.store_and_forward()

        # even tho there is a bad file in the mix, all the good ones should
        # work
        for f in new_files:
            self._t_file_compare(f)
            print f
        
    def fail_mid_test(self):
        (top, new_files) = self._t_build_list()
        dest = top['destinations']

        (fname, bad_ent) = self._t_new_dest("notahost", 5150)
        ndx = len(dest) / 2
        dest.insert(ndx, bad_ent)
        top['destinations'] = dest

        c = LTClient(self.src_file, top)
        v = LTServer(c, c)
        v.store_and_forward()

        # even tho there is a bad file in the mix, all the good ones should
        # work
        for f in new_files:
            self._t_file_compare(f)
            print f

