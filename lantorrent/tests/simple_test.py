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



class TestSimple(unittest.TestCase):

    def setUp(self):
        self.host = "localhost"
        self.src_file = "/etc/group"
        self.src_size = os.path.getsize(self.src_file)

    def tearDown(self):
        pass

    def test_xfer_one_null_no_check(self): 
        final = pylantorrent.create_endpoint_entry(self.host, ["/dev/null"], self.src_size, rename=False)
        final['destinations'] = []
        c = LTClient(self.src_file, final)
        v = LTServer(c, c)
        v.store_and_forward()

    def _t_xfer_one_file_check(self, sz=128*1024): 
        (osf, fname) = tempfile.mkstemp()

        try:
            final = pylantorrent.create_endpoint_entry(self.host, [fname], self.src_size, block_size=sz)
            final['destinations'] = []
            c = LTClient(self.src_file, final)
            v = LTServer(c, c)
            v.store_and_forward()

            rc = filecmp.cmp(self.src_file, fname)
            self.assertTrue(rc)
        finally:
            os.close(osf)
            os.remove(fname)
        
    def test_xfer_one_file_check(self): 
        self._t_xfer_one_file_check()


    # some times odd block sizes can cause fiel corruption
    def test_block_sizes(self):
        sizes = [11, 23, 53, 997, 5093]
        for sz in sizes:
            self._t_xfer_one_file_check(sz=sz)




