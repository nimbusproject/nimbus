#!/usr/bin/env python


import sqlite3
import sys
import pynimbusauthz
from pynimbusauthz.cmd_opts import cbOpts
from pynimbusauthz.db import DB
from pynimbusauthz.user import User
from pynimbusauthz.authz_exception import AuthzException
import uuid


def setup_options(argv):
    u = """[options] <canonical username pattern>

List cumulus authz user accounts"""

    (parser, all_opts) = pynimbusauthz.get_default_options(u)
    opt = cbOpts("alias", "a", "list all user alias as well", False, flag=True)
    all_opts.append(opt)
    opt = cbOpts("type", "t", "list alias only of the given type", None, vals=pynimbusauthz.alias_types.keys().append(None))
    all_opts.append(opt)
    opt = cbOpts("bya", "s", "lookup user by alias", False, flag=True)
    all_opts.append(opt)

    (o, args) = pynimbusauthz.parse_args(parser, all_opts, argv)

    return (o, args)


def list_user(opts, user):
    pynimbusauthz.print_msg(opts, 0, "User %s : %s" % (user.get_id(), user.get_friendly()))
    if opts.alias:
        alias_a = user.get_all_alias()

        for a in alias_a:
            pynimbusauthz.print_msg(opts, 0, "\t%s alias: %s" % (a.get_type(), a.get_name()))


def main(argv=sys.argv[1:]):
    
    try:
        con_str = pynimbusauthz.get_db_connection_string()
        db_obj = DB(con_str=con_str)

        (opts,args) = setup_options(argv)

        if len(args) > 0:
            u_pattern = args[0]
        else:
            u_pattern = ""

        if opts.bya:
            usa = User.find_alias(db_obj, u_pattern)
            users = []
            for ua in usa:
                users.append(ua.get_canonical_user())
        else:
            users = User.find_user(db_obj, u_pattern)

        if users == None:
            pynimbusauthz.print_msg(opts, 0, "No users in list")
            return 1

        for u in users:
            list_user(opts, u)

    except AuthzException, ae:
        print ae
        return ae.get_rc()
    return 0



if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

