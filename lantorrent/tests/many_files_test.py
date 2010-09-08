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



class TestManyFiels(unittest.TestCase):

    def setUp(self):
        self.src_file = "/etc/group"
        self.src_size = os.path.getsize(self.src_file)
        self.ports_a = os.environ['LANTORRENT_TEST_PORTS'].split(",")
        self.files = []

    def tearDown(self):
        while len(self.files) > 0:
            f = self.files.pop(0)
            os.remove(f)

    def _t_new_dest_file(self):
        (osf, fname) = tempfile.mkstemp()
        self.files.append(fname)
        os.close(osf)
        return fname

    def _t_file_compare(self, f):
        rc = filecmp.cmp(self.src_file, f)
        self.assertTrue(rc)

    def _t_build_list(self):
        new_files = []
        dests = []
        for port in self.ports_a:
            fnames_a = []
            for i in range(0, 4):
                fname = self._t_new_dest_file()
                new_files.append(fname)
                fnames_a.append(fname)

            ent = pylantorrent.create_endpoint_entry("localhost", fnames_a, self.src_size, port=int(port))
            dests.append(ent)
            new_files.append(fname)
            print "sending to %s" % (str(fnames_a))

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

