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
    u = """<old path> <new path>
        This tool allows you to change the physical location of files
        in your repository.  It changes the base path where files are 
        physically located.  The internal name and location is changed
        leaving the external logical name in tact. 

        All instances of <old path> are substituted with <new path>.
        Paths that do not match <old path> are left intact.
"""

    (parser, all_opts) = pynimbusauthz.get_default_options(u)

    (o, args) = pynimbusauthz.parse_args(parser, all_opts, argv)
    if len(args) != 2:
        raise AuthzException('CLI_PARAMETER', 'You must specify an old path and a new path.  See --help')

    return (o, args)

def main(argv=sys.argv[1:]):
    
    try:
        con_str = pynimbusauthz.get_db_connection_string()
        db_obj = DB(con_str=con_str)

        (opts,args) = setup_options(argv)

        old_path = args[0]
        new_path = args[1]

        pattern = old_path + "%"

        files = list(File.find_files_from_data(db_obj, pattern))
        for f in files:
            old_key = f.get_data_key()
            new_key = old_key.replace(old_path, new_path, 1)
            f.set_data_key(new_key)
        db_obj.commit()
        print "done - %d files rebased" % len(files)

    except AuthzException, ae:
        print ae
        return ae.get_rc()
    return 0



if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

