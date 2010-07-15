#!/usr/bin/python

"""
List the nimbus users
"""
from nimbusweb.setup import autoca
import string
import random
import os
import sys
import sys
from ConfigParser import SafeConfigParser
import time
import pycb
import pycb.tools
import pynimbusauthz
import tempfile
import filecmp
from pynimbusauthz.cmd_opts import cbOpts
from pynimbusauthz.db import DB
from pynimbusauthz.user import *
import logging
import shlex
from nimbusweb.setup.setuperrors import *
from nimbusweb.setup.groupauthz import *

g_created_cert_files=False
g_report_options = ["dn", "canonical_id", "access_id", "access_secret", "display_name", "group"]

class printer_obj(object):
    def __init__(self):
        pass


def get_nimbus_home():
    """Determines home directory of Nimbus install we are using.
    
    First looks for a NIMBUS_HOME enviroment variable, else assumes that
    the home directory is the parent directory of the directory with this
    script.
    """
    nimbus_home = os.getenv("NIMBUS_HOME")
    if not nimbus_home:
        script_dir = os.path.dirname(__file__)
        nimbus_home = os.path.dirname(script_dir)
    if not os.path.exists(nimbus_home):
        raise CLIError('ENIMBUSHOME', "NIMBUS_HOME must refer to a valid path")
    return nimbus_home

def setup_options(argv):

    u = """[options] <email pattern>
List a Nimbus user

Use % for a wild card
    """
    (parser, all_opts) = pynimbusauthz.get_default_options(u)

    opt = cbOpts("report", "r", "Report the selected columns from the following: " + pycb.tools.report_options_to_string(g_report_options), pycb.tools.report_options_to_string(g_report_options))
    all_opts.append(opt)
    opt = cbOpts("delim", "D", "Character between columns in the report", ",")
    all_opts.append(opt)

    (o, args) = pynimbusauthz.parse_args(parser, all_opts, argv)

    # def verify_opts(o, args, parser):
    if len(args) != 1:
        pynimbusauthz.parse_args(parser, [], ["--help"])

    return (o, args, parser)

def report_results(db, user, opts):
    o = printer_obj()
    o.display_name = user.get_friendly()

    dnu = user.get_alias_by_friendly(o.display_name, pynimbusauthz.alias_type_x509)
    if dnu != None:
        nh = get_nimbus_home()
        groupauthz_dir = os.path.join(nh, "services/etc/nimbus/workspace-service/group-authz/")
        o.dn = dnu.get_name()
        group = find_member(groupauthz_dir, o.dn)
        o.group = None
        if group != None:
            o.group = group.group_id
    else:
        o.dn = None
        o.group = None
    o.canonical_id = user.get_id()

    s3u = user.get_alias_by_friendly(o.display_name, pynimbusauthz.alias_type_s3)
    if s3u != None:
        o.access_id = s3u.get_name()
        o.access_secret = s3u.get_data()
    else:
        o.access_id = None
        o.access_secret = None
 
    pycb.tools.print_report(o, opts.report, opts)

    # todo, reset options structure to report user

def main(argv=sys.argv[1:]):

    try:
        (o, args, p) = setup_options(argv)

        search_pattern = args[0]
        con_str = pycb.config.authzdb
        db = DB(con_str)
        user_list = User.find_user_by_friendly(db, search_pattern)
        for u in user_list:
            report_results(db, u, o)
        db.commit()

    except CLIError, clie:
        print clie
        return clie.get_rc()

    return 0

if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

