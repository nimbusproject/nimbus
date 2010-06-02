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

g_report_options = ["ID", "password", "quota", "canonical_id"]

def setup_options(argv):

    u = """[options] <display name>
Create a new cumulus users
    """
    (parser, all_opts) = pynimbusauthz.get_default_options(u)

    opt = cbOpts("password", "p", "Set the secret key associated with this cumulus account.  If not specified one will be generated.", None)
    all_opts.append(opt)
    opt = cbOpts("exist", "e", "Update an existing user", False, flag=True)
    all_opts.append(opt)
    opt = cbOpts("canonical_id", "c", "Use this canonical user ID.  If not specified a new one will be generated.  If you are not trying to tie the cumulus account with some other Nimbus account, then you probably do not need to specify this option.", None)
    all_opts.append(opt)
    opt = cbOpts("report", "r", "Report the selected columns from the following: " + pycb.tools.report_options_to_string(g_report_options), pycb.tools.report_options_to_string(g_report_options))
    all_opts.append(opt)
    opt = cbOpts("delim", "d", "The column separater for the report.", ",")
    all_opts.append(opt)

    (o, args) = pynimbusauthz.parse_args(parser, all_opts, argv)

    return (o, args)


def add_user_generate_pw(len):
    chars = string.letters + string.digits
    newpasswd = ""
    for i in range(len):
        newpasswd = newpasswd + random.choice(chars)
    return newpasswd

def main_trap(argv=sys.argv[1:]):

    auth = pycb.config.auth

    (opts, args) = setup_options(argv)
    if len(args) == 0:
        raise cbToolsException('CMDLINE', ("You must provide a display name"))

    display_name = args[0]

    if opts.password == None:
        opts.password = add_user_generate_pw(42)
    if not opts.exist:
        # make new user
        if opts.canonical_id == None:
            opts.canonical_id = add_user_generate_pw(21)

        auth.create_user(display_name, opts.canonical_id, opts.password, opts)

    try:
        user_id = auth.get_user_id_by_display(display_name)
    except Exception, ex:
        raise cbToolsException('UNKNOWN_USER', [display_name], ex)

    try:
        u = auth.get_user(user_id)
    except:
        raise cbToolsException('UNKNOWN_USER', [user_id], ex)

    opts.ID = user_id
    opts.canonical_id = u.get_canonical_id()
    opts.quota = u.get_quota()
    opts.id = u.set_user_pw(opts.password)

    pycb.tools.print_report(opts, opts.report, opts)

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
