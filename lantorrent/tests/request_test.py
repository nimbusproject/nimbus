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
from pylantorrent.request import *



class TestRequestXfer(unittest.TestCase):

    def setUp(self):
        self.src_file = "/etc/group"
        self.src_size = os.path.getsize(self.src_file)
        self.ports_a = os.environ['LANTORRENT_TEST_PORTS'].split(",")
        self.files = []

    def tearDown(self):
        while len(self.files) > 0:
            f = self.files.pop(0)
            os.remove(f)

    def _get_temp_file(self):
        (osf, fname) = tempfile.mkstemp()
        os.close(osf)
        self.files.append(fname)
        return fname

    def _t_file_compare(self, f):
        rc = filecmp.cmp(self.src_file, f)
        self.assertTrue(rc)

    def test_request_one(self):
        port = int(self.ports_a[0])
        host = "localhost:%d" % (port)
        fname = self._get_temp_file()

        rc = pylantorrent.request.main([self.src_file, fname, str(uuid.uuid1()), host])
        self.assertEqual(rc, 0, "rc should be 0 but is %d" % (rc))
        self._t_file_compare(fname)

    def test_request_many_block(self):
        port = int(self.ports_a[0])
        host = "localhost:%d" % (port)

        for i in range(0, 7):
            fname = self._get_temp_file()
            rc = pylantorrent.request.main([self.src_file, fname, str(uuid.uuid1()), host])
            self.assertEqual(rc, 0, "rc should be 0 but is %d" % (rc))
            self._t_file_compare(fname)

    def test_request_same_host_nonblock(self):
        port = int(self.ports_a[0])
        host = "localhost:%d" % (port)

        print "requesting all the files"
        rids = []
        for i in range(0, 10):
            fname = self._get_temp_file()
            rid = str(uuid.uuid1())
            rc = pylantorrent.request.main(["-n", self.src_file, fname, rid, host])
            self.assertEqual(rc, 0, "rc should be 0 but is %d" % (rc))
            rids.append(rid)

        print "waiting on all the files"
        for rid in rids:
            print "waiting on %s" % (rid)
            rc = pylantorrent.request.main(["-a", rid])
            self.assertEqual(rc, 0, "rc should be 0 but is %d" % (rc))

        print "checking all the files"
        for f in self.files:
            self._t_file_compare(f)

    def test_request_many_host_nonblock(self):

        print "requesting all the files"
        rids = []
        for i in range(0, len(self.ports_a) * 2):
            port = int(self.ports_a[i%len(self.ports_a)])
            host = "localhost:%d" % (port)
            fname = self._get_temp_file()
            rid = str(uuid.uuid1())
            rc = pylantorrent.request.main(["-n", self.src_file, fname, rid, host])
            self.assertEqual(rc, 0, "rc should be 0 but is %d" % (rc))
            rids.append(rid)

        print "waiting on all the files"
        for rid in rids:
            print "waiting on %s" % (rid)
            rc = pylantorrent.request.main(["-a", rid])
            self.assertEqual(rc, 0, "rc should be 0 but is %d" % (rc))

        print "checking all the files"
        for f in self.files:
            self._t_file_compare(f)





