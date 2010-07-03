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
from pynimbusauthz.user import User
import pynimbusauthz

def setup_options(argv):

    u = """[options] <display name> <formated integer quota | UNLIMITED> 
Sets a quota for the given cumulus user
"""
    (parser, all_opts) = pynimbusauthz.get_default_options(u)

    (o, args) = pynimbusauthz.parse_args(parser, all_opts, argv)

    return (o, args)

def pretty_parse(num_str):

    end = num_str[-1:len(num_str)]
    if end.isdigit():
        return int(num_str)

    t = {}
    t["K"] = 1024
    t["M"] = 1024*1024
    t["G"] = 1024*1024*1024
    t["T"] = 1024*1024*1024*1024

    b = int(num_str[0:-1])
    rc = b * t[end]
    return rc

def main_trap(argv=sys.argv[1:]):

    auth = pycb.config.auth

    (opts, args) = setup_options(argv)
    if len(args) != 2:
        raise cbToolsException('CMDLINE', ("You must provide a display name and a quota.  See --help"))

    display_name = args[0]
    qstr = args[1]

    if qstr == "UNLIMITED":
        q = User.UNLIMITED
    else:
        try:
            q = pretty_parse(qstr)
            print q
        except:
            traceback.print_exc(file=sys.stdout)
            raise cbToolsException('CMDLINE', ("The quota must be an integer > 0 or the string UNLIMITED"))

        if q < 1:
            raise cbToolsException('CMDLINE', ("The quota must be an integer > 0 or the string UNLIMITED"))

    try:
        user_id = auth.get_user_id_by_display(display_name)
        u = auth.get_user(user_id)
    except Exception, ex:
        raise cbToolsException('UNKNOWN_USER', (display_name), ex)

    if u == None:
        raise cbToolsException('UNKNOWN_USER', (display_name))

    u.set_quota(q)

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

    


