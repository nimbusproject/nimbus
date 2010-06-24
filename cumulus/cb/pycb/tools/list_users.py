#!/usr/bin/env python

import getpass
import traceback
import string
import random
import os
import sys
from ConfigParser import SafeConfigParser
import pycb
import pynimbusauthz
import shutil
import pycb.cbPosixSecurity
from pycb.tools.cbToolsException import cbToolsException
from pynimbusauthz.cmd_opts import cbOpts

g_report_options = ["friendly", "ID", "password", "quota", "canonical_id"]

class printer_obj(object):
    def __init__(self):
        pass

def setup_options(argv):

    u = """[options] [<display name pattern>]
Get a listing of cumulus users.
"""
    (parser, all_opts) = pynimbusauthz.get_default_options(u)

    opt = cbOpts("report", "r", "Report the selected columns from the following: " + pycb.tools.report_options_to_string(g_report_options), pycb.tools.report_options_to_string(g_report_options))
    all_opts.append(opt)
    opt = cbOpts("delim", "d", "The column separater for the report.", ",")
    all_opts.append(opt)

    (o, args) = pynimbusauthz.parse_args(parser, all_opts, argv)

    return (o, args)

def main_trap(argv=sys.argv[1:]):

    auth = pycb.config.auth

    (opts, args) = setup_options(argv)
    if len(args) == 0:
        raise cbToolsException('CMDLINE', ("You must provide a display name"))

    search_name = args[0].replace("*", "%%")

    all_matches = auth.find_user_id_by_display(search_name)

    for id in all_matches:
        u = auth.get_user(id)
        p = printer_obj() 
        p.ID = id
        p.canonical_id = u.get_canonical_id()
        p.quota = u.get_quota()
        p.password = u.get_password()
        p.friendly = u.get_display_name()

        pycb.tools.print_report(p, opts.report, opts)

    return 0

def main(argv=sys.argv[1:]):
    try:
        rc = main_trap(argv)
    except cbToolsException, tex:
        print tex
        rc = tex.get_rc()
    except SystemExit:
        rc = 0
    except:
        traceback.print_exc(file=sys.stdout)
        print 'An unknown error occurred'
        rc = 128

    return rc

if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

