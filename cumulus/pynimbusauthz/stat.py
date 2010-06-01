#!/usr/bin/env python


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
    u = """[options] <filename>

Get information on a particular file"""

    (parser, all_opts) = pynimbusauthz.get_default_options(u)
    opt = cbOpts("all", "a", "list all users in the acl", False, flag=True)
    all_opts.append(opt)
    opt = cbOpts("type", "t", "The type of file to add.", "s3", vals=pynimbusauthz.object_types.keys())
    all_opts.append(opt)
    opt = cbOpts("parent", "p", "The parent object of the file.", None)
    all_opts.append(opt)

    (o, args) = pynimbusauthz.parse_args(parser, all_opts, argv)

    return (o, args)

def format_perms(p_list):
    outs = ""
    pos = "rwRW"
    for p in pos:
        if p in p_list:
            outs = outs + p
        else:
            outs = outs + "-"
    return outs


def stat_print_uf(opts, uf, n, t):
    f = uf.get_file()
    o = f.get_owner().get_id()
    u = uf.get_user()
    p = format_perms(uf.get_perms(force=True))

    msg = "%10s\t%10s\t%10s\t%10s\t%10s" % (n, t, o, u, p) 
    pynimbusauthz.print_msg(opts, 1, msg)
    #pynimbusauthz.print_msg(opts, 1, "file\ttype\towner\tperms")
 

def main(argv=sys.argv[1:]):
    
    try:
        con_str = pynimbusauthz.get_db_connection_string()
        db_obj = DB(con_str=con_str)

        (opts,args) = setup_options(argv)

        if len(args) == 0:
            raise AuthzException('CLI_PARAMETER', "You must specify a filename")
        parent = None
        if opts.parent != None:
            parent = File.get_file(db_obj, opts.parent, opts.type)
            if parent == None:
                raise AuthzException('FILE_EXISTS', "bucket %s not found" % (opts.parent))


        object_name = args[0]
        file1 = File.get_file(db_obj, object_name, opts.type, parent=parent)
        if file1 == None:
            pynimbusauthz.print_msg(opts, 0, "File not found")
            return

        uf = UserFile(file1)
        msg = "%10s\t%10s\t%10s\t%10s\t%10s" % ("file", "type", "owner", "user", "perms")
        pynimbusauthz.print_msg(opts, 1, msg)
        n = uf.get_file().get_name()
        t = uf.get_file().get_object_type()
        stat_print_uf(opts, uf, n, t)
        if opts.all:
            user_list = uf.get_file().get_all_users()
            for u in user_list:
                uf = UserFile(uf.get_file(), u)
                stat_print_uf(opts, uf, " ", " ")

    except AuthzException as ae:
        print ae
        return ae.get_rc()
    return 0



if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

