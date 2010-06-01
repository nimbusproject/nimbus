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

def setup_options(argv):

    u = """[options] <display name>
Remove a user from the system.
"""
    (parser, all_opts) = pynimbusauthz.get_default_options(u)

    opt = cbOpts("all", "a", "Destroy all associated user data as well (including files that they own)", False, flag=True)
    all_opts.append(opt)

    (o, args) = pynimbusauthz.parse_args(parser, all_opts, argv)

    return (o, args)

def main(argv=sys.argv[1:]):

    auth = pycb.config.auth

    (opts, args) = setup_options(argv)
    if len(args) == 0:
        raise cbToolsException('CMDLINE', ["You must provide a display name"])

    display_name = args[0]

    try:
        user_id = auth.get_user_id_by_display(display_name)
        u = auth.get_user(user_id)
    except Exception, ex:
        raise cbToolsException('UNKNOWN_USER', (display_name), ex)

    u.remove_user()

    return 0

if __name__ == "__main__":
    try:
        rc = main()
    except cbToolsException, tex:
        print tex
        rc = tex.get_rc()
    except:
        traceback.print_exc(file=sys.stdout)
        print 'An unknown error occurred'
        rc = 128

    sys.exit(rc)

    


