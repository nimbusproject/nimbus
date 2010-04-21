#!/usr/bin/env python

"""
Creates a new user certificate and key using the embedded Nimbus CA.

In general you should avoid using this facility for a production environment.
You should use a real Certificate Authority. This script is provided to help
you get up and running on Nimbus, with test credentials.
"""

USAGE_EPILOG = """
If you do not specify a name, you will be prompted for one.
If you do not specify a destination directory, the new certificate and key
files will be placed in the ~/.globus/ directory.
"""

import os
import sys
import traceback
import ConfigParser
import optparse
import readline
import logging

from nimbusweb.setup import autoca
from nimbusweb.setup.setuperrors import *

def get_opt_parser():
    """Prepares an option parser and returns it."""
    parser = optparse.OptionParser(description=__doc__, epilog=USAGE_EPILOG)
    parser.add_option("--common-name", "--cn", "-c", dest="cn",
            help="Name for new certificate", metavar="NAME")
    parser.add_option("--dir", "-d", dest="dir",
            help="Destination directory for new cert and key", metavar="DIR")
    return parser

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

def _main():
    nimbus_home = get_nimbus_home()
    webdir = os.path.join(nimbus_home, 'web/')
    if not os.path.exists(webdir):
        raise IncompatibleEnvironment(
                "web dir doesn't exist. is this a valid Nimbus install? (%s)"
                % webdir)
    configpath = os.path.join(nimbus_home, 'nimbus-setup.conf')
    config = ConfigParser.SafeConfigParser()
    if not config.read(configpath):
        raise IncompatibleEnvironment(
                "Failed to read config from '%s'. Has Nimbus been configured?"
                % configpath)
    try:
        cadir = config.get('nimbussetup', 'ca.dir')
    except NoOptionError:
        raise IncompatibleEnvironment("Config file '%s' does not contain ca.dir" %
                configpath)

    parser = get_opt_parser()
    (opts, args) = parser.parse_args()

    if opts.dir:
        dir = os.path.abspath(opts.dir)
        if not os.path.isdir(dir):
            raise InvalidInput("The specified directory does not exist (%s)" %
                    dir)
    else:
        dir = os.path.expanduser("~/.globus/")
        if not os.path.exists(dir):
            try:
                os.mkdir(dir)
            except:
                raise IncompatibleEnvironment("Destination directory was not "+
                        "specified. Creating the default ~/.globus directory "+
                        "failed: %s" % dir)
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
    
    print "\nThe new certificate and key will be placed in: %s" % dir

    cn = opts.cn
    if not cn:
        print "\nPlease enter the Common Name for the new certificate."
        print "This could be the user's full name or username."
        cn = raw_input("Name: ")
    cn = cn.strip()
    if not cn:
        raise InvalidInput("You must specify a valid Common Name")

    log = logging.getLogger()
    dn = autoca.createCert(cn, webdir, cadir, certpath, keypath, log)

    print "Success! The DN of the new certificate is:\n\n    \"%s\"\n"%dn

def main():
    try:
        _main()
    except InvalidInput, e:
        msg = "\nProblem with input: %s" % e.msg
        print >>sys.stderr, msg
        return 1

    except InvalidConfig, e:
        msg = "\nProblem with configuration: %s" % e.msg
        print >>sys.stderr, msg
        return 2

    except IncompatibleEnvironment, e:
        msg = "\nProblem with environment: %s" % e.msg
        print >>sys.stderr, msg

    return 0

if __name__ == "__main__":
    try:
        sys.exit(main())
    except SystemExit, KeyboardInterrupt:
        raise
    except:
        info = sys.exc_info()
        try:
            name = info[0].__name__
        except AttributeError:
            name = info[0]
        err = "\nUnexpected error! Please report all of the following info:\n\n"
        err += "%s: %s" % (name, info[1])
        print >>sys.stderr, err
        traceback.print_tb(info[2])
        sys.exit(97)
