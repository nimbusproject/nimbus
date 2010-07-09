#!/usr/bin/python

"""
Creates a new nimbus users.  It will create all needed user aliases (cumulus,
x509, and web loging id)
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
import traceback

g_created_cert_files=False
g_report_options = ["cert", "key", "dn", "canonical_id", "access_id", "access_secret", "url", "web_id"]


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
        raise CLIError('ENIMBUSHOME', "NIMBUS_HOME must refer to a valid path:  %s" % (nimbus_home))

    return nimbus_home

def setup_options(argv):

    u = """[options] <email>
Create a new nimbus user
    """
    (parser, all_opts) = pynimbusauthz.get_default_options(u)
    (o, args) = pynimbusauthz.parse_args(parser, all_opts, argv)

    # def verify_opts(o, args, parser):
    if len(args) != 1:
        pynimbusauthz.parse_args(parser, [], ["--help"])

    return (o, args, parser)

def remove_gridmap(dn):
    nimbus_home = get_nimbus_home()
    configpath = os.path.join(nimbus_home, 'nimbus-setup.conf')
    config = SafeConfigParser()
    if not config.read(configpath):
        raise CLIError('ENIMBUSHOME', 
                "Failed to read config from '%s'. Has Nimbus been configured?"
                % configpath)
    gmf = config.get('nimbussetup', 'gridmap')
    gmf = os.path.join(nimbus_home, gmf)

    found = False
    f = open(gmf, 'r')
    (nf, new_name) = tempfile.mkstemp(dir=nimbus_home+"/var", prefix="gridmap", text=True)
    for l in f.readlines():
        l = l.strip()
        if l == "":
            continue
        a = shlex.split(l)
        if dn == a[0]:
            found = True
        else:
            os.write(nf, l)
            os.write(nf, os.linesep)

    if not found:
        print "WARNING! user not found in %s" % (dn)
    os.close(nf)
    f.close()
    os.unlink(gmf)
    os.rename(new_name, gmf)

def delete_user(o):
    con_str = pycb.config.authzdb
    db = DB(con_str)
    # create canonical user
    user = User.get_user_by_friendly(db, o.emailaddr)
    if user == None:
        raise CLIError('EUSER', "No such user %s" % (o.emailaddr))

    o.canonical_id = user.get_id()

    dnu = user.get_alias_by_friendly(o.emailaddr, pynimbusauthz.alias_type_x509)
    if dnu == None:
        print "WARNING! there is no x509 alias for user %s" % (o.emailaddr)
    else:
        dn = dnu.get_name()
        remove_gridmap(dn)

        nh = get_nimbus_home()
        groupauthz_dir = os.path.join(nh, "services/etc/nimbus/workspace-service/group-authz/")
        try:
            remove_member(groupauthz_dir, dn)
        except Exception, ex:
            print "WARNING %s" % (ex)

    user.destroy_brutally()
    db.commit()

def main(argv=sys.argv[1:]):

    try:
        (o, args, p) = setup_options(argv)
        o.emailaddr = args[0]
        delete_user(o)
    except CLIError, clie:
        print clie
        return clie.get_rc()
    except:
        traceback.print_exc(file=sys.stdout)

    return 0

if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

