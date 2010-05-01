import nose.tools
import unittest
import time
import os
import sys
import sqlite3
import os
import sys
import pynimbusauthz
from pynimbusauthz.db import DB
from pynimbusauthz.user import User
from pynimbusauthz.user import UserAlias
import pynimbusauthz.add_user
import pynimbusauthz.list_user
import unittest
import uuid
import tempfile
from pynimbusauthz.cmd_opts import cbOpts




class TestOpts(unittest.TestCase):

    def setUp(self):
        (self.parser,self.allOpts) = pynimbusauthz.get_default_options("options")
        pass

    def tearDown(self):
        pass

    def test_basic(self):
        opt = cbOpts("XX", "x", "message", "value")
        self.allOpts.append(opt)

        d = opt.get_description()
        e = opt.get_error_msg()

        for o in self.allOpts:
            o.add_opt(self.parser)

        (options, args) = self.parser.parse_args(["-x", "value2"])
        for o in self.allOpts:
            o.validate(options)
        self.assertEqual(options.XX, "value2", "incorrect value")


    def test_flag(self):

        opt = cbOpts("XX", "x", "message", False, flag=True)
        self.allOpts.append(opt)
        d = opt.get_description()
        e = opt.get_error_msg()

        for o in self.allOpts:
            o.add_opt(self.parser)

        (options, args) = self.parser.parse_args(["-x"])

        for o in self.allOpts:
            o.validate(options)

        self.assertTrue(options.XX, "flag failed to set to true")

        (options, args) = self.parser.parse_args([])

        for o in self.allOpts:
            o.validate(options)

        self.assertFalse(options.XX, "flag failed to set to default false")

    def test_range(self):

        opt = cbOpts("XX", "x", "message", 3, range=[1,5])
        self.allOpts.append(opt)
        d = opt.get_description()
        e = opt.get_error_msg()

        for o in self.allOpts:
            o.add_opt(self.parser)

        (options, args) = self.parser.parse_args([])
        for o in self.allOpts:
            o.validate(options)
        self.assertEqual(options.XX, 3, "Default did not work")

        (options, args) = self.parser.parse_args(["-x", "2"])
        for o in self.allOpts:
            o.validate(options)
        self.assertEqual(options.XX, "2", "Range value not set")

        (options, args) = self.parser.parse_args(["-x", "11"])
        try:
            for o in self.allOpts:
                o.validate(options)
            self.fail("should have had n error because value too large")
        except:
            pass

        (options, args) = self.parser.parse_args(["-x", "0"])
        try:
            for o in self.allOpts:
                o.validate(options)
            self.fail("should have had n error because value too small")
        except:
            pass

        (options, args) = self.parser.parse_args(["-x", "-1"])
        try:
            for o in self.allOpts:
                o.validate(options)
            self.fail("should have had n error because value too low")
        except:
            pass


    def test_vals(self):

        opt = cbOpts("XX", "x", "message", "hi", vals=["hi","hello","world"])
        self.allOpts.append(opt)
        d = opt.get_description()
        e = opt.get_error_msg()

        for o in self.allOpts:
            o.add_opt(self.parser)

        (options, args) = self.parser.parse_args([])
        for o in self.allOpts:
            o.validate(options)
        self.assertEqual(options.XX, "hi", "Default did not work")

        (options, args) = self.parser.parse_args(["-x","hello"])
        for o in self.allOpts:
            o.validate(options)
        self.assertEqual(options.XX, "hello", "Default did not work")

        (options, args) = self.parser.parse_args(["-x", "11"])
        try:
            for o in self.allOpts:
                o.validate(options)
            self.fail("should have had n error because value not in set")
        except:
            pass

    def test_count(self):

        opt = cbOpts("XX", "x", "message", 0, count=True)
        self.allOpts.append(opt)
        d = opt.get_description()
        e = opt.get_error_msg()

        for o in self.allOpts:
            o.add_opt(self.parser)

        (options, args) = self.parser.parse_args([])
        for o in self.allOpts:
            o.validate(options)
        self.assertEqual(options.XX, 0, "Default hould be 0")

        a = []
        for i in range(0, 3):
            a.append("-x")
        (options, args) = self.parser.parse_args(a)
        for o in self.allOpts:
            o.validate(options)
        self.assertEqual(options.XX, 3, "should be 3")

    def test_range2(self):

        opt = cbOpts("XX", "x", "message", 3, range=[1,-1])
        self.allOpts.append(opt)
        d = opt.get_description()
        e = opt.get_error_msg()

        for o in self.allOpts:
            o.add_opt(self.parser)

        (options, args) = self.parser.parse_args(["-x", "100"])
        for o in self.allOpts:
            o.validate(options)
        self.assertEqual(options.XX, "100", "Range value not set")

        (options, args) = self.parser.parse_args(["-x", "0"])
        try:
            for o in self.allOpts:
                o.validate(options)
            self.fail("should have had n error because value too small")
        except:
            pass

    def test_range3(self):

        opt = cbOpts("XX", "x", "message", 3, range=[-1,10])
        self.allOpts.append(opt)
        d = opt.get_description()
        e = opt.get_error_msg()

        for o in self.allOpts:
            o.add_opt(self.parser)

        (options, args) = self.parser.parse_args(["-x", "-1111"])
        for o in self.allOpts:
            o.validate(options)
        self.assertEqual(options.XX, "-1111", "Range value not set")

        (options, args) = self.parser.parse_args(["-x", "11"])
        try:
            for o in self.allOpts:
                o.validate(options)
            self.fail("should have had n error because value too large")
        except:
            pass
        (options, args) = self.parser.parse_args(["-x", "-1"])
        for o in self.allOpts:
            o.validate(options)
        self.assertEqual(options.XX, "-1", "Range value not set")
