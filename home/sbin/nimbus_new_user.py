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
import traceback
import filecmp
from pynimbusauthz.cmd_opts import cbOpts
from pynimbusauthz.db import DB
from pynimbusauthz.user import *
import logging
import shlex

g_created_cert_files=False
g_report_options = ["cert", "key", "dn", "canonical_id", "accessid", "accesssecret", "url"]


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
        raise IncompatibleEnvironment("NIMBUS_HOME must refer to a valid path")
    return nimbus_home

def get_dn(cert_file):
    nimbus_home = get_nimbus_home()
    webdir = os.path.join(nimbus_home, 'web/')
    if not os.path.exists(webdir):
        raise IncompatibleEnvironment(
                "web dir doesn't exist. is this a valid Nimbus install? (%s)"
                % webdir)
    log = logging.getLogger()
    dn = autoca.getCertDN(cert_file, webdir, log)
    return dn

def generate_cert(o):
    nimbus_home = get_nimbus_home()
    webdir = os.path.join(nimbus_home, 'web/')
    if not os.path.exists(webdir):
        raise IncompatibleEnvironment(
                "web dir doesn't exist. is this a valid Nimbus install? (%s)"
                % webdir)
    configpath = os.path.join(nimbus_home, 'nimbus-setup.conf')
    config = SafeConfigParser()
    if not config.read(configpath):
        raise IncompatibleEnvironment(
                "Failed to read config from '%s'. Has Nimbus been configured?"
                % configpath)
    try:
        cadir = config.get('nimbussetup', 'ca.dir')
    except NoOptionError:
        raise IncompatibleEnvironment("Config file '%s' does not contain ca.dir" %
                configpath)

    dir = o.dest
    keypath = os.path.join(dir, "userkey.pem")
    certpath = os.path.join(dir, "usercert.pem")
    if os.path.exists(keypath):
        raise IncompatibleEnvironment(
                "The destination key path exists: '%s'" % keypath)
    if os.path.exists(certpath):
        raise IncompatibleEnvironment(
                "The destination cert path exists: '%s'" % certpath)
    if not os.access(dir, os.W_OK):
        raise IncompatibleEnvironment(
                "The destination directory is not writable: '%s'" % dir)

    cn = o.cn
    if not cn:
        cn = o.emailaddr
    # XXX
    log = logging.getLogger()
    dn = autoca.createCert(cn, webdir, cadir, certpath, keypath, log)
    global g_created_cert_files
    g_created_cert_files = True

    return (certpath, keypath)


def setup_options(argv):

    u = """[options] <email>
Create a new nimbus user
    """
    (parser, all_opts) = pynimbusauthz.get_default_options(u)

    opt = cbOpts("dn", "s", "This is used when the user already has a cert.  This option will use the given DN instead of generating a new cert", None)
    all_opts.append(opt)
    opt = cbOpts("cert", "c", "Instead of generating a new key pair use this certificate.  This must be used with the --key option", None)
    all_opts.append(opt)
    opt = cbOpts("key", "k", "Instead of generating a new key pair use this key.  This must be used with the --cert option", None)
    all_opts.append(opt)
    opt = cbOpts("cn", "n", "This is used to set the common name when generating a new certificate.  If none is specified the email address is used.  This can be optionally used in conjunction with --key and --cert", None)
    all_opts.append(opt)
    opt = cbOpts("accessid", "a", "Instead of generating a new access id/secret pair, use this one.  This must be used with the --accesssecret option", None)
    all_opts.append(opt)
    opt = cbOpts("accesssecret", "p", "Instead of generating a new access id/secret pair, use this one.  This must be used with the --accessid option", None)
    all_opts.append(opt)
    opt = cbOpts("dest", "d", "The directory to put all of the new files into.", None)
    all_opts.append(opt)
    opt = cbOpts("web", "w", "Set the web user name.  If not set and a web user is desired a username will be created from the email address.", None)
    all_opts.append(opt)
    opt = cbOpts("noweb", "W", "Do not put stuff into webapp sqlite", False)
    all_opts.append(opt)
    opt = cbOpts("nocert", "C", "Do not add a DN", False)
    all_opts.append(opt)
    opt = cbOpts("delim", "D", "Character between columns in the report", ",")
    all_opts.append(opt)
    opt = cbOpts("noaccess", "A", "Do not add access tokens", False)
    all_opts.append(opt)
    opt = cbOpts("report", "r", "Report the selected columns from the following: " + pycb.tools.report_options_to_string(g_report_options), pycb.tools.report_options_to_string(g_report_options))
    all_opts.append(opt)

    (o, args) = pynimbusauthz.parse_args(parser, all_opts, argv)

    # def verify_opts(o, args, parser):
    if len(args) != 1:
        pynimbusauthz.parse_args(parser, [], ["--help"])

    if o.cert == None and o.key != None or o.cert != None and o.key == None:
        print "key and cert must be used together"
        pynimbusauthz.parse_args(parser, all_opts, ["--help"])
    if o.accessid == None and o.accesssecret != None or o.accessid != None and o.accesssecret == None:
        print "secret and accessid must be used together"
        pynimbusauthz.parse_args(parser, all_opts, ["--help"])
    if o.noweb and o.nocert and o.noaccess:
        print "you must want this tool to do something"
        pynimbusauthz.parse_args(parser, all_opts, ["--help"])
    if o.dest == None:
        o.dest = tempfile.mkdtemp()
    else:
        try:
            os.mkdir(o.dest)
        except:
            pass

    # verify the id/secret length
    if o.accessid != None:
        if len(o.accessid) != 21:
            print "secret and accessid must be used together"
            pynimbusauthz.parse_args(parser, all_opts, ["--help"])
        if len(o.accesssecret) != 42:
            print "secret and accessid must be used together"
            pynimbusauthz.parse_args(parser, all_opts, ["--help"])
    if o.cert != None:
        if not os.path.isfile(o.cert):
            print "No such cert file %s" % (o.cert)
            pynimbusauthz.parse_args(parser, all_opts, ["--help"])
        if not os.path.isfile(o.key):
            print "No such cert file %s" % (o.key)
            pynimbusauthz.parse_args(parser, all_opts, ["--help"])
    if o.cert != None and o.nocert:
        print "why specify a cert and use nocert?"
        pynimbusauthz.parse_args(parser, all_opts, ["--help"])
    if o.dn != None and o.nocert:
        print "why specify a dn and use nocert?"
        pynimbusauthz.parse_args(parser, all_opts, ["--help"])

    o.canonical_id = None
    o.url = None

    return (o, args, parser)

def add_gridmap(o):
    nimbus_home = get_nimbus_home()
    configpath = os.path.join(nimbus_home, 'nimbus-setup.conf')
    config = SafeConfigParser()
    if not config.read(configpath):
        raise IncompatibleEnvironment(
                "Failed to read config from '%s'. Has Nimbus been configured?"
                % configpath)
    gmf = config.get('nimbussetup', 'gridmap')

    f = open(gmf, 'r+')
    for l in f.readlines():
        a = shlex.split(l)
        if o.dn == a[0]:
            print "WARNING! This dn is already in the gridmap file"
            f.close()
            return
    f.write("\"%s\" not_a_real_account\n" % (o.dn))
    f.close()

def create_user(o):
    con_str = pycb.config.authzdb
    db = DB(con_str)
    try:
        # create canonical user
        user = User(db, friendly=o.emailaddr)
        o.canonical_id = user.get_id()
        if not o.noaccess and o.accessid == None:
            o.accessid = pynimbusauthz.random_string_gen(21)
            o.accesspw = pynimbusauthz.random_string_gen(42)
            # add to db
            ua1 = user.create_alias(o.accessid, pynimbusauthz.alias_type_s3, o.emailaddr, alias_data=o.accesspw)

        if not o.nocert:
            # if not give a dn we need to get it from the provided cert, or 
            # generate a cet key pair and get it from that
            if o.dn == None:
                if o.cert == None:            
                    # generate a cert
                    (o.cert, o.key) = generate_cert(o)
                # get dn 
                o.dn = get_dn(o.cert)
            ua2 = user.create_alias(o.dn, pynimbusauthz.alias_type_x509, o.emailaddr)
            # add dn to gridmap
            add_gridmap(o)

        if not o.noweb:
            if o.web == None:
                o.web = o.emailaddr.__hash__()
            # call into web api
            pass

        db.commit()
    except Exception, ex1:
        global g_created_cert_files
        if g_created_cert_files:
            os.remove(o.cert)
            os.remove(o.key)
        db.rollback()
        raise ex1

def report_results(o):
    pycb.tools.print_report(o, o.report, o)

def main(argv=sys.argv[1:]):

    (o, args, p) = setup_options(argv)
    o.emailaddr = args[0]
    create_user(o)
    report_results(o)

if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

