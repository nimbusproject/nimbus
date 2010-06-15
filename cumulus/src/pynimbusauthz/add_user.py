#!/usr/bin/env python


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
    u = """[options] [<canonical username>]

Manage cumulus authz user accounts
"""

    (parser, all_opts) = pynimbusauthz.get_default_options(u)
    opt = cbOpts("new", "n", "Add a new top level user", False, flag=True)
    all_opts.append(opt)
    opt = cbOpts("friendlyname", "F", "Associate the friendly name with the new user or alias", None)
    all_opts.append(opt)
    opt = cbOpts("alias", "a", "Add a user with the given alias, or use this alias name for other operations", None)
    all_opts.append(opt)
    opt = cbOpts("type", "t", "The type of alias to add.  Only relevant when operating on an alias", "s3", vals=pynimbusauthz.alias_types.keys())
    all_opts.append(opt)
    opt = cbOpts("remove", "r", "Remove a user", False, flag=True)
    all_opts.append(opt)
    opt = cbOpts("genkey", "g", "Generate alias data (a password).  This is only relvant when adding a new alias", False, flag=True)
    all_opts.append(opt)
    opt = cbOpts("setkey", "k", "Set alias data (a password).  This is only relvant when adding a new alias", None)
    all_opts.append(opt)
    opt = cbOpts("remove_alias", "x", "Remove the user alias of the given type", None)
    all_opts.append(opt)
    opt = cbOpts("force", "f", "Force the most extreme case, eg: delete all user information including files that are owned by the user.", False, flag=True)
    all_opts.append(opt)

    (o, args) = pynimbusauthz.parse_args(parser, all_opts, argv)

    if not o.new and len(args) < 1:
        raise AuthzException('CLI_PARAMETER', "You must provide a uuid for the user")
    if o.remove and len(args) < 1:
        raise AuthzException('CLI_PARAMETER', "You must provide tell me what user to remove")
    if o.alias == None and o.genkey:
        raise AuthzException('CLI_PARAMETER', "You must specify the alias that you are changing")
    if o.alias == None and o.setkey != None:
        raise AuthzException('CLI_PARAMETER', "You must specify the alias that you are changing")

    if o.alias != None and o.friendlyname == None:
        o.friendlyname = o.alias

    return (o, args)

def main(argv=sys.argv[1:]):
    
    try:
        con_str = pynimbusauthz.get_db_connection_string()
        db_obj = DB(con_str=con_str)

        (opts,args) = setup_options(argv)

        user_uu = None
        if len(args) == 1:
            user_uu = args[0]
        if opts.new:
            user = User(db_obj, user_uu, friendly=opts.friendlyname, create=True)
            pynimbusauthz.print_msg(opts, 0, "User %s added" % (user.get_id()))
        else:
            user = User(db_obj, user_uu) 
            pynimbusauthz.print_msg(opts, 0, "User %s" % (user.get_id()))

        if opts.alias != None:
            user_alias = user.get_alias(opts.alias, opts.type)
            if user_alias == None:
                user_alias = user.create_alias(opts.alias, opts.type, opts.friendlyname)
                pynimbusauthz.print_msg(opts, 0, "Creating new alias %s:%s" % (opts.type,opts.alias))
            if opts.genkey:
                data = pynimbusauthz.random_string_gen(42)
                pynimbusauthz.print_msg(opts, 0, "Key generated %s" % (data))
                user_alias.set_data(data)
            elif opts.setkey != None:
                data = opts.setkey
                user_alias.set_data(data)
                pynimbusauthz.print_msg(opts, 0, "updated the alias key")

        if opts.remove_alias != None:
            user_alias = user.get_alias(opts.remove_alias, opts.type)
            user_alias.remove()

        if opts.remove:
            pynimbusauthz.print_msg(opts, 1, "Removing user %s" % (user.get_id()))
            if opts.force:
                pynimbusauthz.print_msg(opts, 1, "Removing all references")
                user.destroy_brutally()
            else:
                user.destroy() 
        db_obj.commit()
    except AuthzException, ae:
        print ae
        return ae.get_rc()
    return 0



if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

