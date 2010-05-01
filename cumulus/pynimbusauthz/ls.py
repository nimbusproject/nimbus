#!/usr/bin/python


import sqlite3
import sys
import pynimbusauthz
from pynimbusauthz.cmd_opts import cbOpts
from pynimbusauthz.db import DB
from pynimbusauthz.user import User
from pynimbusauthz.objects import File
from pynimbusauthz.objects import UserFile
from pynimbusauthz.authz_exception import AuthzException
import uuid


def setup_options(argv):
    u = """[options] [<pattern>]

list files known to the system.  If no pattern is used all files are 
returned."""

    (parser, all_opts) = pynimbusauthz.get_default_options(u)
    opt = cbOpts("type", "t", "list only files of the given type", "s3", vals=pynimbusauthz.object_types.keys())
    all_opts.append(opt)
    opt = cbOpts("parent", "p", "The parent object of the file.", None)
    all_opts.append(opt)

    (o, args) = pynimbusauthz.parse_args(parser, all_opts, argv)

    return (o, args)

def print_file(opts, f):
    msg = "%s:%s\t%s\t%s\t%s" % (f.get_object_type(), f.get_name(), f.get_owner(), f.get_data_key(), str(f.get_parent()))
    pynimbusauthz.print_msg(opts, 0, msg)


def main(argv=sys.argv[1:]):
    
    try:
        con_str = pynimbusauthz.get_db_connection_string()
        db_obj = DB(con_str=con_str)

        (opts,args) = setup_options(argv)

        if len(args) > 0:
            u_pattern = args[0]
        else:
            u_pattern = ""
        parent = None
        if opts.parent != None:
            parent = File.get_file(db_obj, opts.parent, opts.type)
            if parent == None:
                raise AuthzException(['FILE_EXISTS'], "parent %s not found" % (opts.parent))

        if opts.type == "all":
            types = pynimbusauthz.object_types.keys()
        else:
            types = [opts.type]

        for t in types:
            files = File.find_files(db_obj, u_pattern, t, parent)

            for f in files:
                print_file(opts, f)

    except AuthzException as ae:
        print ae
        return ae.get_rc()
    return 0



if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

