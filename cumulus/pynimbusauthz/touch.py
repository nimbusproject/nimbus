#!/usr/bin/env python


import sqlite3
import sys
import pynimbusauthz
from pynimbusauthz.cmd_opts import cbOpts
from pynimbusauthz.db import DB
from pynimbusauthz.user import User
from pynimbusauthz.objects import File
from pynimbusauthz.authz_exception import AuthzException
import uuid


def setup_options(argv):
    u = """[options] <owner> <filename> <data>
Create a file reference in the sytem"""

    (parser, all_opts) = pynimbusauthz.get_default_options(u)
    opt = cbOpts("type", "t", "The type of file to add.", "s3", vals=pynimbusauthz.object_types.keys())
    all_opts.append(opt)
    opt = cbOpts("parent", "p", "The parent object of the file.", None)
    all_opts.append(opt)

    (o, args) = pynimbusauthz.parse_args(parser, all_opts, argv)

    return (o, args)

def main(argv=sys.argv[1:]):
    
    try:
        con_str = pynimbusauthz.get_db_connection_string()
        db_obj = DB(con_str=con_str)

        (opts,args) = setup_options(argv)

        if len(args) != 3:
            raise AuthzException('CLI_PARAMETER', "You must specify a username filename and a datakey\nTry --help")
        user_name = args[0]
        object_name = args[1]
        data = args[2]

        user = User(db_obj, uu=user_name)
        parent = None
        if opts.parent != None:
            parent = File.get_file(db_obj, opts.parent, opts.type)
            if parent == None:
                raise AuthzException('FILE_EXISTS', "parent %s not found" % (opts.parent))
        File.create_file(db_obj, object_name, user, data, opts.type, parent=parent)

    except AuthzException as ae:
        print ae
        return ae.get_rc()
    return 0



if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

