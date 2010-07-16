#!/usr/bin/python

"""
Creates new Nimbus users.  It will create all needed user aliases (Cumulus,
x509, and web login id)
"""
from nimbusweb.setup import autoca
import string
import random
import os
import sys
import sys
import ConfigParser
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
from nimbusweb.setup.setuperrors import *
from nimbusweb.setup.groupauthz import *
from optparse import SUPPRESS_HELP

g_report_options = ["cert", "key", "dn", "canonical_id", "access_id", "access_secret", "url", "web_id", "cloud_properties"]

DEBUG = False

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

def generate_cert(o):
    nimbus_home = get_nimbus_home()
    webdir = os.path.join(nimbus_home, 'web/')
    if not os.path.exists(webdir):
        raise CLIError('ENIMBUSHOME', 
                "web dir doesn't exist. is this a valid Nimbus install? (%s)"
                % webdir)
    configpath = os.path.join(nimbus_home, 'nimbus-setup.conf')
    config = SafeConfigParser()
    if not config.read(configpath):
        raise CLIError('ENIMBUSHOME', 
                "Failed to read config from '%s'. Has Nimbus been configured?"
                % configpath)
    try:
        cadir = config.get('nimbussetup', 'ca.dir')
        cadir = os.path.join(nimbus_home, cadir)
    except ConfigParser.NoOptionError:
        raise CLIError('ENIMBUSHOME', 
                "Config file '%s' does not contain ca.dir" %
                configpath)

    dir = o.dest
    keypath = os.path.join(dir, "userkey.pem")
    certpath = os.path.join(dir, "usercert.pem")
    if os.path.exists(keypath):
        raise CLIError('EPATH', 
                "The destination key path exists: '%s'" % keypath)
    if os.path.exists(certpath):
        raise CLIError('EPATH', 
                "The destination cert path exists: '%s'" % certpath)
    if not os.access(dir, os.W_OK):
        raise CLIError('EPATH', 
                "The destination directory is not writable: '%s'" % dir)

    cn = o.cn
    if not cn:
        cn = o.emailaddr
    # XXX
    log = logging.getLogger()
    dn = autoca.createCert(cn, webdir, cadir, certpath, keypath, log)

    return (certpath, keypath)

def cloud_props(o):

    if o.nocloud_properties:
        return

    nimbus_home = get_nimbus_home()
    configpath = os.path.join(nimbus_home, 'nimbus-setup.conf')
    config = SafeConfigParser()
    if not config.read(configpath):
        raise CLIError('ENIMBUSHOME',
                "Failed to read config from '%s'. Has Nimbus been configured?"
                % configpath)
    try:
        cert_file = config.get('nimbussetup', 'hostcert')
        cert_file = os.path.join(nimbus_home, cert_file)
        factory_id = get_dn(cert_file)
        hostname = config.get('nimbussetup', 'hostname')
        template_file = os.path.join(nimbus_home, "var/cloud.properties.in")
    except ConfigParser.NoOptionError:
        raise CLIError('ENIMBUSHOME',
                "Config file '%s' does not contain the needed values" %
                configpath)

    string_subs = {}
    string_subs['@FACTORY_DN@'] = factory_id
    string_subs['@HOST@'] = hostname
    string_subs['@CUMULUS_ID@'] = o.access_id
    string_subs['@CUMULUS_SECRET@'] = o.access_secret
    string_subs['@CANONICAL_ID@'] = o.canonical_id

    # start the sed
    o.cloud_properties = o.dest + "/cloud.properties"
    fin = open(template_file, "r")
    fout = open(o.cloud_properties, "w")
    for l in fin.readlines():
        for k in string_subs:
            v = string_subs[k]
            if v == None:
                v = ""
            l = l.replace(k, v)
        fout.write(l)
    fin.close()
    fout.close()

def setup_options(argv):

    u = """[options] <email>
Create/edit a nimbus user
    """
    (parser, all_opts) = pynimbusauthz.get_default_options(u)

    opt = cbOpts("dn", "s", "This is used when the user already has a cert.  This option will use the given DN instead of generating a new cert", None)
    all_opts.append(opt)
    opt = cbOpts("canonical_id", "i", "Specify the canonical ID string to user for this new user.  If the ID already exists an error will be returned.", None)
    all_opts.append(opt)
    opt = cbOpts("cert", "c", "Instead of generating a new key pair use this certificate.  This must be used with the --key option", None)
    all_opts.append(opt)
    opt = cbOpts("key", "k", "Instead of generating a new key pair use this key.  This must be used with the --cert option", None)
    all_opts.append(opt)
    opt = cbOpts("cn", "n", "This is used to set the common name when generating a new certificate.  If none is specified the email address is used.  This can be optionally used in conjunction with --key and --cert", None)
    all_opts.append(opt)
    opt = cbOpts("access_id", "a", "Instead of generating a new access id/secret pair, use this one.  This must be used with the --access-secret option", None)
    all_opts.append(opt)
    opt = cbOpts("access_secret", "p", "Instead of generating a new access id/secret pair, use this one.  This must be used with the --access-id option", None)
    all_opts.append(opt)
    opt = cbOpts("dest", "d", "The directory to put all of the new files into.", None)
    all_opts.append(opt)
    opt = cbOpts("group", "g", "Put this user in the given group", "01", vals=("01", "02", "03", "04"))
    all_opts.append(opt)
    opt = cbOpts("web_id", "w", "Set the web user name.  If not set and a web user is desired a username will be created from the email address.", None)
    all_opts.append(opt)
    opt = cbOpts("web", "W", "Insert user into webapp for key(s) pickup", False, flag=True)
    all_opts.append(opt)
    opt = cbOpts("nocloud_properties", "P", "Do not make the cloud.properties file", False, flag=False)
    all_opts.append(opt)
    opt = cbOpts("nocert", "C", "Do not add a DN", False, flag=True)
    all_opts.append(opt)
    opt = cbOpts("delim", "D", "Character between columns in the report", ",")
    all_opts.append(opt)
    opt = cbOpts("noaccess", "A", "Do not add access tokens", False, flag=True)
    all_opts.append(opt)
    opt = cbOpts("report", "r", "Report the selected columns from the following: " + pycb.tools.report_options_to_string(g_report_options), pycb.tools.report_options_to_string(g_report_options))
    all_opts.append(opt)

    (o, args) = pynimbusauthz.parse_args(parser, all_opts, argv)

    # def verify_opts(o, args, parser):
    if len(args) != 1:
        pynimbusauthz.parse_args(parser, [], ["--help"])

    if o.cert == None and o.key != None or o.cert != None and o.key == None:
        raise CLIError('ECMDLINE', "key and cert must be used together %s %s" % (str(o.cert), str(o.key)))
    if o.access_id == None and o.access_secret != None or o.access_id != None and o.access_secret == None:
        raise CLIError('ECMDLINE', "secret and access-id must be used together")
    if not o.web and o.nocert and o.noaccess:
        raise CLIError('ECMDLINE', "you must want this tool to do something")
    if o.dest == None:
        nh = get_nimbus_home() + "/var/ca/"
        o.dest = tempfile.mkdtemp(suffix='cert', prefix='tmp', dir=nh)
    else:
        try:
            os.mkdir(o.dest)
        except:
            pass

    if o.cert != None:
        if not os.path.isfile(o.cert):
            raise CLIError('ECMDLINE', "No such cert file %s" % (o.cert))
        if not os.path.isfile(o.key):
            raise CLIError('ECMDLINE', "No such cert file %s" % (o.key))
    if o.cert != None and o.nocert:
        raise CLIError('ECMDLINE', "why specify a cert and use nocert?")
    if o.dn != None and o.nocert:
        raise CLIError('ECMDLINE', "why specify a dn and use nocert?")

    o.url = None
    o.cloud_properties = None

    return (o, args, parser)

def add_gridmap(o):
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
        if o.dn == a[0]:
            print "WARNING! This dn is already in the gridmap file"
            f.close()
            return
    f.write("\"%s\" not_a_real_account\n" % (o.dn))
    f.close()

def create_user(o, db):
    added_gridmap = False
    try:
        # create canonical user
        user = User.get_user_by_friendly(db, o.emailaddr)
        if user != None:
            raise CLIError('EUSER', "The user already exists: %s" % (o.emailaddr))

        if o.canonical_id != None:
            user = User.get_user(db, o.canonical_id)
            if user != None:
                raise CLIError('EUSER', "The canonical user already exists: %s" % (o.canonical_id))

            user = User(db, friendly=o.emailaddr, uu=o.canonical_id, create=True)
        else:
            user = User(db, friendly=o.emailaddr, create=True)

        o.canonical_id = user.get_id()
        if not o.noaccess:
            if o.access_id == None:
                o.access_id = pynimbusauthz.random_string_gen(21)
                o.access_secret = pynimbusauthz.random_string_gen(42)

            # add to db
            ua1 = user.create_alias(o.access_id, pynimbusauthz.alias_type_s3, o.emailaddr, alias_data=o.access_secret)

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
            added_gridmap = True

        cloud_props(o)
        if o.web:
            if o.web_id == None:
                o.web_id = o.emailaddr.split("@")[0]
            o.url = do_web_bidnes(o)

        do_group_bidnes(o)

        db.commit()
    except Exception, ex1:
        if added_gridmap:
            remove_gridmap(o.dn)
        db.rollback()
        if DEBUG:
            traceback.print_exc(file=sys.stdout)
        raise ex1

def do_web_bidnes(o):
    
    # import this here because otherwise errors will be thrown when
    # the settings.py is imported (transitively).  Web is disabled by 
    # default in a Nimbus install, we should keep the experience cleanest
    # for new admins.
    try:
        import nimbusweb.portal.nimbus.create_web_user as create_web_user
    except Exception, e:
        msg = "\nERROR linking with web application (have you ever sets up the web application?)\n"
        msg += "\nSee: http://www.nimbusproject.org/docs/current/admin/reference.html#nimbusweb-config\n"
        msg += "\n%s\n" % e
        raise CLIError('EUSER', "%s" % msg)
    
    (errmsg, url) = create_web_user.create_web_user(o.web_id, o.emailaddr, o.cert, o.key, o.canonical_id, o.access_id, o.access_secret, o.cloud_properties)
    
    if errmsg:
        raise CLIError('EUSER', "Problem adding user to webapp: %s" % (errmsg))
    elif url:
        return url
    else:
        raise CLIError('EUSER', "Problem adding user to webapp, nothing returned?")

def do_group_bidnes(o):
    if o.dn == None:
        return
    
    nh = get_nimbus_home()
    groupauthz_dir = os.path.join(nh, "services/etc/nimbus/workspace-service/group-authz/")
    if o.group:
        add_member(groupauthz_dir, o.dn, int(o.group))
    else:
        add_member(groupauthz_dir, o.dn)

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

def main(argv=sys.argv[1:]):

    try:
        (o, args, p) = setup_options(argv)
        con_str = pycb.config.authzdb
        db = DB(con_str)

        o.emailaddr = args[0]
        create_user(o, db)
        report_results(o, db)
        db.close()
    except CLIError, clie:
        if DEBUG:
            traceback.print_exc(file=sys.stdout)
        
        print clie
        return clie.get_rc()

    return 0

if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

