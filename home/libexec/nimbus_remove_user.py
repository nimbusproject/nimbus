#!/usr/bin/python

"""
Removes nimbus users.  It will remove all unneeded user aliases (cumulus,
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
import shutil

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
Remove a nimbus user
    """
    (parser, all_opts) = pynimbusauthz.get_default_options(u)
    opt = cbOpts("web_id", "w", "Set the web user name to remove.  If not set and the user is to be removed from webapp, a username will be created from the email address.", None)
    all_opts.append(opt)
    opt = cbOpts("web", "W", "Remove user from webapp", False, flag=True)
    all_opts.append(opt)
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
        print "WARNING! user DN not found in gridmap: %s" % (dn)
    os.close(nf)
    f.close()
    shutil.move(new_name, gmf)

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

        if o.web:
            if o.web_id == None:
                o.web_id = o.emailaddr.split("@")[0]
            remove_web(o)

    user.destroy_brutally()
    db.commit()

def remove_web(o):
    # import this here because otherwise errors will be thrown when
    # the settings.py is imported (transitively).  Web is disabled by
    # default in a Nimbus install, we should keep the experience cleanest
    # for new admins.
    try:
        import nimbusweb.portal.nimbus.remove_web_user as remove_web_user
    except Exception, e:
        msg = "\nERROR linking with web application (have you ever sets up the web application?)\n"
        msg += "\nSee: http://www.nimbusproject.org/docs/current/admin/reference.html#nimbusweb-config\n"
        msg += "\n%s\n" % e
        raise CLIError('EUSER', "%s" % msg)

    errmsg = remove_web_user.remove_web_user(o.web_id)

    if errmsg:
        raise CLIError('EUSER', "Problem removing user from webapp: %s" % (errmsg))

def main(argv=sys.argv[1:]):

    try:
        (o, args, p) = setup_options(argv)
        o.emailaddr = args[0]
        delete_user(o)
    except CLIError, clie:
        print clie
        return clie.get_rc()
    except SystemExit, se:
        pass
    except:
        traceback.print_exc(file=sys.stdout)

    return 0

if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

