#!/usr/bin/python

import sqlite3
import os
import sys
import pynimbusauthz
from pynimbusauthz.cmd_opts import cbOpts
from pynimbusauthz.db import DB
from pynimbusauthz.user import User
from pynimbusauthz.authz_exception import AuthzException
import uuid


def setup_options(argv):
    u = """[options] <canonical username> 

Manage cumulus authz user accounts
"""

    (parser, all_opts) = pynimbusauthz.get_default_options(u)

    opt = cbOpts("type", "t", "The file types that will have a quota.", "s3", vals=pynimbusauthz.object_types.keys())
    all_opts.append(opt)


    opt = cbOpts("report", "r", "Display current usage for the user", False, flag=True)
    all_opts.append(opt)
    opt = cbOpts("quota", "x", "The limit (an integer of UNLIMITED)", None)
    all_opts.append(opt)

    (o, args) = pynimbusauthz.parse_args(parser, all_opts, argv)

    if len(args) != 1:
        print "You must provide a user name"
        sys.exit(1)

    return (o, args)

def main(argv=sys.argv[1:]):
    
    try:
        con_str = pynimbusauthz.get_db_connection_string()
        db_obj = DB(con_str=con_str)

        (opts,args) = setup_options(argv)

        user = User(db_obj, args[0], create=False)

        if opts.quota != None:
            q = opts.quota
            if opts.quota == "UNLIMITED":
                q = User.UNLIMITED

            user.set_quota(q, object_type=opts.type)
        if opts.report:
            q = user.get_quota(object_type=opts.type)
            u = user.get_quota_usage(object_type=opts.type)

            if q != User.UNLIMITED:
                r = q - u

                rstr = pynimbusauthz.pretty_number(r)
                qstr = pynimbusauthz.pretty_number(q)
                ustr = pynimbusauthz.pretty_number(u)

                pynimbusauthz.print_msg(opts, 0, "%-10s %s" % ("Quota", qstr))
                pynimbusauthz.print_msg(opts, 0, "%-10s %s" % ("Usage", ustr))
                pynimbusauthz.print_msg(opts, 0, "%-10s %s" % ("Remaining", rstr))
                if r < 0:
                    pynimbusauthz.print_msg(opts, 0, "OVER LIMIT!")
                elif r == 0:
                    pynimbusauthz.print_msg(opts, 0, "At Limit")
                else:
                    p = (float(r) / float(q) )* 100.0
                    pynimbusauthz.print_msg(opts, 0, "%-10s %5.1f%%" % ("Available", p))
            else:
                pynimbusauthz.print_msg(opts, 0, "Quota UNLIMITED")

            

        db_obj.commit()
    except AuthzException as ae:
        print ae
        return ae.get_rc()
    return 0



if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

