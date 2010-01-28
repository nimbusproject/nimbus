#!/usr/bin/env python

import logging
import optparse
import os
import socket
import sys
import traceback
import ConfigParser
from StringIO import StringIO
import readline

from nimbusweb.setup import pathutil, javautil, checkssl, gtcontainer
from nimbusweb.setup.setuperrors import *

CONFIGSECTION = 'nimbussetup'
DEFAULTCONFIG = """

[nimbussetup]

# relative to base directory
hostcert: var/hostcert.pem
hostkey: var/hostkey.pem

gridmap: var/gridmap

debug: off

"""

def getlog(override=None):
    """Allow developer to replace logging mechanism, e.g. if this
    module is incorporated into another program as an API.

    Keyword arguments:

    * override -- Custom logger (default None, uses global variable)

    """
    global _log
    if override:
        _log = override
    try:
        _log
    except:
        _log = logging.getLogger("nimbussetup")
        _log.setLevel(logging.DEBUG)
    return _log
    
def configureLogging(level, formatstring=None, logger=None):
    """Configure the logging format and mechanism.  Sets global 'log' variable.
    
    Required parameter:
        
    * level -- log level

    Keyword arguments:

    * formatstring -- Custom logging format (default None, uses time+level+msg)

    * logger -- Custom logger (default None)
    """
    
    global log
    
    logger = getlog(override=logger)
    
    if not formatstring:
        formatstring = "%(asctime)s (%(filename)s:%(lineno)d): %(message)s"
    
    formatter = logging.Formatter(formatstring)
    ch = logging.StreamHandler()
    ch.setLevel(level)
    ch.setFormatter(formatter)
    logger.addHandler(ch)
    
    # set global variable
    log = logger
    
    log.debug("debug enabled")

def getconfig(filepath=None):
    config = ConfigParser.SafeConfigParser()
    
    fh = StringIO(DEFAULTCONFIG)
    config.readfp(fh)
    if filepath:
        config.read(filepath)
    return config
    
class ARGS:

    """Class for command-line argument constants"""

    BASEDIR_LONG = "--basedir"
    BASEDIR = "-b"
    BASEDIR_HELP = "Path to base Nimbus directory"

    CONFIGPATH_LONG="--conf"
    CONFIGPATH="-c"
    CONFIGPATH_HELP="Path to configuration file"
    
    DEBUG_LONG="--debug"
    DEBUG="-d"
    DEBUG_HELP="Log debug messages"
    
    
def validateargs(opts):
    
    seeh = "see help (-h)"

    if not opts.basedir:
        raise InvalidInput("%s required, %s." % (ARGS.BASEDIR_LONG, seeh))

def parsersetup():
    """Return configured command-line parser."""

    ver="Nimbus setup"
    usage="see help (-h)."
    parser = optparse.OptionParser(version=ver, usage=usage)

    # ----

    group = optparse.OptionGroup(parser,  "Misc options", "-------------")

    group.add_option(ARGS.DEBUG, ARGS.DEBUG_LONG,
                      action="store_true", dest="debug", default=False, 
                      help=ARGS.DEBUG_HELP)
    
    group.add_option(ARGS.CONFIGPATH, ARGS.CONFIGPATH_LONG,
                    dest="configpath", metavar="PATH",
                    help=ARGS.CONFIGPATH_HELP)
    
    group.add_option(ARGS.BASEDIR, ARGS.BASEDIR_LONG,
                    dest="basedir", metavar="PATH",
                    help=ARGS.BASEDIR_HELP)
    
    parser.add_option_group(group)

    return parser

def validate_environment(basedir):
    if not pathutil.is_absolute_path(basedir):
        raise IncompatibleEnvironment("Base directory setting is not absolute")

    pathutil.ensure_dir_exists(basedir, "base")

    webdir = get_webdir_path(basedir)
    pathutil.ensure_dir_exists(webdir, "web")

    gtdir = get_gtdir_path(basedir)
    pathutil.ensure_dir_exists(webdir, "GT container")

    # check that we have some java
    javautil.check(webdir, log)

def get_webdir_path(basedir):
    return pathutil.pathjoin(basedir, 'web/')

def get_gtdir_path(basedir):
    return pathutil.pathjoin(basedir, 'services/')

def get_cadir_path(basedir):
    return pathutil.pathjoin(basedir, 'var/ca/')

def get_hostname():

    hostname = socket.getfqdn()

    print "\nWhat is the fully qualified hostname of this machine? If you \
don't know or care right now, hit return to use the detected hostname: \
%s\n" % hostname

    input = raw_input("Hostname: ")
    if input:
        hostname = input
    
    return hostname

def perform_setup(basedir, config):

    # first, set up CA and host cert/key

    webdir = get_webdir_path(basedir)
    cadir = get_cadir_path(basedir)
    gtdir = get_gtdir_path(basedir)
    hostcert = pathutil.pathjoin(basedir, config.get(CONFIGSECTION, 'hostcert'))
    hostkey = pathutil.pathjoin(basedir, config.get(CONFIGSECTION, 'hostkey'))
    gridmap = pathutil.pathjoin(basedir, config.get(CONFIGSECTION, 'gridmap'))
    hostname = get_hostname()

    checkssl.run(webdir, hostcert, hostkey, log, cadir=cadir, hostname=hostname)

    # then adjust the web config to point to these keys

    webconfpath = pathutil.pathjoin(webdir, 'nimbusweb.conf')
    webconf = ConfigParser.SafeConfigParser()
    if not webconf.read(webconfpath):
        raise IncompatibleEnvironment("nimbus web config does not exist: %s" %
                webconfpath)
    webconf.set('nimbusweb', 'ssl.cert', hostcert)
    webconf.set('nimbusweb', 'ssl.key', hostkey)

    with open(webconfpath, 'wb') as webconffile:
        webconf.write(webconffile)

    # then setup GT container

    gtcontainer.adjust_hostname(hostname, webdir, gtdir, log)
    gtcontainer.adjust_secdesc_path(webdir, gtdir, log)
    gtcontainer.adjust_host_cert(hostcert, hostkey, webdir, gtdir, log)
    gtcontainer.adjust_gridmap_file(gridmap, webdir, gtdir, log)


def main(argv=None):
    if os.name != 'posix':
        print >>sys.stderr, "Only runs on POSIX systems."
        return 3
        
    parser = parsersetup()

    if argv:
        (opts, args) = parser.parse_args(argv[1:])
    else:
        (opts, args) = parser.parse_args()
        
    global log
    log = None
    
    printdebugoutput = False
    
    try:
        
        # 1. Intake args and confs
        
        validateargs(opts)
        config = getconfig(filepath=opts.configpath)
        
        # 2. Setup logging
        
        confdebug = config.get("nimbussetup", "debug")
        if confdebug == "on":
            printdebugoutput = True
        elif opts.debug:
            printdebugoutput = True
            
        if printdebugoutput:
            configureLogging(logging.DEBUG)
        else:
            configureLogging(logging.INFO)
            
        # 3. Dump settings
            
        basedir = opts.basedir
        log.debug("base directory: %s" % basedir)
        
        # 4. Validate enviroment
        validate_environment(basedir)

        # 5. Go forth and setup
        
        perform_setup(basedir, config)
        

    except InvalidInput, e:
        msg = "\nProblem with input: %s" % e.msg
        print >>sys.stderr, msg
        return 1

    except InvalidConfig, e:
        msg = "\nProblem with configuration: %s" % e.msg
        print >>sys.stderr, msg
        return 2

    except IncompatibleEnvironment, e:
        msg = "\nCannot validate environment: %s" % e.msg
        print >>sys.stderr, msg
        if printdebugoutput:
            print >>sys.stderr, "\n---------- stacktrace ----------"
            traceback.print_tb(sys.exc_info()[2])
            print >>sys.stderr, "--------------------------------"
        return 3

if __name__ == "__main__":
    
    try:
        sys.exit(main())
    except SystemExit:
        raise
    except KeyboardInterrupt:
        raise
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        name = str(exceptname)
        err = str(sys.exc_value)
        errmsg = "\n==> Uncaught problem, please report all following output:\n  %s: %s" % (name, err)
        print >>sys.stderr, errmsg
        traceback.print_tb(sys.exc_info()[2])
        sys.exit(97)
        
