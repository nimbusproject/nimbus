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
from optparse import SUPPRESS_HELP

g_report_options = ["dn", "canonical_id", "access_id", "access_secret"]


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

def get_dn(cert_file):
    nimbus_home = get_nimbus_home()
    webdir = os.path.join(nimbus_home, 'web/')
    if not os.path.exists(webdir):
        raise CLIError('ENIMBUSHOME', 
                "web dir doesn't exist. is this a valid Nimbus install? (%s)"
                % webdir)
    log = logging.getLogger()
    dn = autoca.getCertDN(cert_file, webdir, log)
    return dn

def setup_options(argv):

    u = """[options] <email>
Create/edit a nimbus user
    """
    (parser, all_opts) = pynimbusauthz.get_default_options(u)

    opt = cbOpts("dn", "s", "Change the users dn", None)
    all_opts.append(opt)
    opt = cbOpts("access_id", "a", "Instead of generating a new access id/secret pair, use this one.  This must be used with the --access-secret option", None)
    all_opts.append(opt)
    opt = cbOpts("access_secret", "p", "Instead of generating a new access id/secret pair, use this one.  This must be used with the --access-id option", None)
    all_opts.append(opt)
    opt = cbOpts("delim", "D", "Character between columns in the report", ",")
    all_opts.append(opt)
    opt = cbOpts("group", "g", "Change the users group", None, vals=(None, "01", "02", "03", "04"))
    all_opts.append(opt)

    opt = cbOpts("report", "r", "Report the selected columns from the following: " + pycb.tools.report_options_to_string(g_report_options), pycb.tools.report_options_to_string(g_report_options))
    all_opts.append(opt)

    (o, args) = pynimbusauthz.parse_args(parser, all_opts, argv)

    # def verify_opts(o, args, parser):
    if len(args) != 1:
        pynimbusauthz.parse_args(parser, [], ["--help"])

    o.canonical_id = None
    o.url = None

    return (o, args, parser)

def add_gridmap(dn):
    nimbus_home = get_nimbus_home()
    configpath = os.path.join(nimbus_home, 'nimbus-setup.conf')
    config = SafeConfigParser()
    if not config.read(configpath):
        raise CLIError('ENIMBUSHOME', 
                "Failed to read config from '%s'. Has Nimbus been configured?"
                % configpath)
    gmf = config.get('nimbussetup', 'gridmap')
    gmf = os.path.join(nimbus_home, gmf)

    f = open(gmf, 'r+')
    for l in f.readlines():
        l = l.strip()
        if l == "":
            continue
        a = shlex.split(l)
        if dn == a[0]:
            print "WARNING! This dn is already in the gridmap file"
            f.close()
            return
    f.write("\"%s\" not_a_real_account\n" % (dn))
    f.close()

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


def report_results(o, db):
    user = User.get_user_by_friendly(db, o.emailaddr)
    if user == None:
        raise CLIError('EUSER', "The user should not be in db but is not: %s" % (o.emailaddr))

    dnu = user.get_alias_by_friendly(o.emailaddr, pynimbusauthz.alias_type_x509)
    if dnu != None:
        o.dn = dnu.get_name()
    o.canonical_id = user.get_id()

    s3u = user.get_alias_by_friendly(o.emailaddr, pynimbusauthz.alias_type_s3)
    if s3u != None:
        o.access_id = s3u.get_name()
        o.access_secret = s3u.get_data()

    pycb.tools.print_report(o, o.report, o)

def edit_user(o, db):
    # create canonical user
    user = User.get_user_by_friendly(db, o.emailaddr)
    if user == None:
        raise CLIError('EUSER', "The user does not exists: %s" % (o.emailaddr))
    dnu = user.get_alias_by_friendly(o.emailaddr, pynimbusauthz.alias_type_x509)

    s3u = user.get_alias_by_friendly(o.emailaddr, pynimbusauthz.alias_type_s3)
    # if there is a dn set it
    if o.access_id != None:
        if s3u == None:
            raise CLIError('EUSER', "There is no s3 user for: %s" % (o.emailaddr))
        s3u.set_name(o.access_id.strip())

    if o.access_secret != None:
        if s3u == None:
            raise CLIError('EUSER', "There is no s3 user for: %s" % (o.emailaddr))
        s3u.set_data(o.access_secret.strip())

    nh = get_nimbus_home()
    groupauthz_dir = os.path.join(nh, "services/etc/nimbus/workspace-service/group-authz/")
    if o.group != None:
        if dnu == None:
            raise CLIError('EUSER', "There is x509 entry for: %s" % (o.emailaddr))
        dn = dnu.get_name()
        group = find_member(groupauthz_dir, dn)
        if group != None:
            remove_member(groupauthz_dir, dn)
        add_member(groupauthz_dir, dn, int(o.group))

    if o.dn != None:
        if dnu == None:
            raise CLIError('EUSER', "There is x509 entry for: %s" % (o.emailaddr))
        old_dn = dnu.get_name()


        group = find_member(groupauthz_dir, old_dn)
        if group == None:
            raise CLIError('EUSER', "There is no authz group for user: %s" % (old_dn))
        group_id = group.group_id

        dnu.set_name(o.dn.strip())

        remove_gridmap(old_dn)
        add_gridmap(o.dn)

        try:        
            remove_member(groupauthz_dir, old_dn)
            add_member(groupauthz_dir, o.dn, group_id)
        except:
            remove_gridmap(o.dn)
            add_gridmap(old_dn)
            
    db.commit()

    # todo, reset options structure to report user

def main(argv=sys.argv[1:]):

    try:
        (o, args, p) = setup_options(argv)

        con_str = pycb.config.authzdb
        db = DB(con_str)

        o.emailaddr = args[0]
        edit_user(o, db)
        report_results(o, db)
    except CLIError, clie:
        print clie
        return clie.get_rc()

    return 0

if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

