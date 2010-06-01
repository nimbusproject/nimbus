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
    u = """<canonical username> <filename> <r | w | R | W>

Change the permissions of any user on a file
r : The user can read the file.
w : The user can write the file.
R : The user can read the ACL.
W : The user can alter the ACL.
"""

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
            raise AuthzException('CLI_PARAMETER', "You must specify a username filename permssions")
        user_name = args[0]
        object_name = args[1]
        requested_perms = args[2]

        parent = None
        if opts.parent != None:
            parent = File.get_file(db_obj, opts.parent, opts.type)
            if parent == None:
                raise AuthzException('FILE_EXISTS', "parent %s not found" % (opts.parent))

        file1 = File.get_file(db_obj, object_name, opts.type, parent=parent)
        if file1 == None:
            raise AuthzException('FILE_EXISTS', "file %s:%s not found" % (opts.type, object_name))
        user = User(db_obj, uu=user_name)
        uf = UserFile(file1) # create a uesrfile with owner so we can chmod
        uf.chmod(requested_perms, user=user)
        pynimbusauthz.print_msg(opts, 0, "changed %s to %s for %s" % (str(file1), requested_perms, str(user)))
        db_obj.commit()

    except AuthzException as ae:
        print ae
        return ae.get_rc()
    return 0



if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

