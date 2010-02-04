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

from nimbusweb.setup import pathutil,javautil,checkssl,gtcontainer
from nimbusweb.setup.setuperrors import *

CONFIGSECTION = 'nimbussetup'
DEFAULTCONFIG = """

[nimbussetup]

# relative to base directory
hostcert: var/hostcert.pem
hostkey: var/hostkey.pem

CA.dir: var/ca

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

    CONFIGPATH_LONG = "--conf"
    CONFIGPATH = "-c"
    CONFIGPATH_HELP = "Path to configuration file"
    
    DEBUG_LONG = "--debug"
    DEBUG = "-d"
    DEBUG_HELP = "Log debug messages"

    HOSTNAME_LONG = "--hostname"
    HOSTNAME = "-H"
    HOSTNAME_HELP = "Fully qualified hostname of machine"

    CANAME_LONG= "--caname"
    CANAME = "-C"
    CANAME_HELP = "Unique name to give CA"
    
def validateargs(opts):
    
    seeh = "see help (-h)"

    if not opts.basedir:
        raise InvalidInput("%s required, %s." % (ARGS.BASEDIR_LONG, seeh))

def parsersetup():
    """Return configured command-line parser."""

    ver = "Nimbus setup"
    usage = "see help (-h)."
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

    group = optparse.OptionGroup(parser, "Configuration options", 
            "-------------")
    
    group.add_option(ARGS.HOSTNAME, ARGS.HOSTNAME_LONG,
            dest="hostname", metavar="HOST", help=ARGS.HOSTNAME_HELP)

    group.add_option(ARGS.CANAME, ARGS.CANAME_LONG,
            dest="ca_name", metavar="NAME", help=ARGS.CANAME_HELP)

    return parser

def fold_opts_to_config(opts, config):
    if opts.hostname:
        config.set(CONFIGSECTION, 'hostname', opts.hostname)
    if opts.ca_name:
        config.set(CONFIGSECTION, 'ca.name', opts.ca_name)

def ask_question(question, valuename, default=None):

    answer = None
    while not answer:
        print "\n%s\n" % question
        if default:
            print "Press ENTER to use the default (%s)\n"%default

        value = raw_input(valuename+": ")
        if value:
            answer = value.strip()
        elif default:
            answer = default
        if not answer:
            print "Invalid input. You must specify a value. Or hit Ctrl-C to give up."
    return answer

class NimbusSetup(object):
    def __init__(self, basedir, config, interactive=True):
        self.basedir = basedir
        self.config = config
        self.interactive = interactive

    def get_config(self, key):
        try:
            return self.config.get(CONFIGSECTION, key)
        except ConfigParser.NoOptionError:
            return None

    def set_config(self, key, value):
        return self.config.set(CONFIGSECTION, key, value)

    def validate_environment(self):
        if not pathutil.is_absolute_path(self.basedir):
            raise IncompatibleEnvironment(
                    "Base directory setting is not absolute")
        pathutil.ensure_dir_exists(self.basedir, "base")
        pathutil.ensure_dir_exists(self.webdir_path(), "web")
        pathutil.ensure_dir_exists(self.gtdir_path(), "GT container")
        
        # check that we have some java
        javautil.check(self.webdir_path(), log)

    def resolve_path(self, path):
        """
        Resolves a path relative to base directory. 
        If absolute, returns as-is. If relative, 
        joins with self.basedir and returns.
        """
        
        if os.path.isabs(path):
            return path
        return os.path.join(self.basedir, path)

    def webdir_path(self):
        return self.resolve_path('web/')

    def gtdir_path(self):
        return self.resolve_path('services/')

    def cadir_path(self):
        path = self.get_config('CA.dir')
        return self.resolve_path(path or 'var/ca/')
    
    def hostcert_path(self):
        path = self.get_config('hostcert')
        return self.resolve_path(path)

    def hostkey_path(self):
        path = self.get_config('hostkey')
        return self.resolve_path(path)

    def gridmap_path(self):
        path = self.get_config('gridmap')
        return self.resolve_path(path)

    def hostname(self):
        hostguess = self.get_config('hostname')
        if not hostguess:
            hostguess = socket.getfqdn()

        if self.interactive:
            question = "What is the fully qualified hostname of this machine?"
            hostname = ask_question(question, "Hostname", hostguess)
        else:
            print "Using hostname: '%s'" % hostguess
            hostname = hostguess
        
        self.set_config('hostname', hostname)
        return hostname

    def perform_setup(self):

        # first, set up CA and host cert/key

        webdir = self.webdir_path()
        cadir = self.cadir_path()
        gtdir = self.gtdir_path()
        hostcert = self.hostcert_path()
        hostkey = self.hostkey_path()
        gridmap = self.gridmap_path()
        
        # some potentially interactive queries
        hostname = self.hostname()
        
        #TODO this may require interaction
        checkssl.run(webdir, hostcert, hostkey, log, cadir=cadir, 
                hostname=hostname)
        
        # then adjust the web config to point to these keys
        
        webconfpath = pathutil.pathjoin(webdir, 'nimbusweb.conf')
        webconf = ConfigParser.SafeConfigParser()
        if not webconf.read(webconfpath):
            raise IncompatibleEnvironment(
                    "nimbus web config does not exist: %s" % webconfpath)
        webconf.set('nimbusweb', 'ssl.cert', hostcert)
        webconf.set('nimbusweb', 'ssl.key', hostkey)

        with open(webconfpath, 'wb') as webconffile:
            webconf.write(webconffile)

        if not os.path.exists(gridmap):
            example_gridmap = self.resolve_path('var/gridmap.example')
            import shutil
            shutil.copyfile(example_gridmap, gridmap)

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
        
        validateargs(opts)
        config = getconfig(filepath=opts.configpath)
        
        #Some command line options are folded into the config object
        fold_opts_to_config(opts, config)
        
        confdebug = config.get(CONFIGSECTION, "debug")
        if confdebug == "on":
            printdebugoutput = True
        elif opts.debug:
            printdebugoutput = True
            
        if printdebugoutput:
            configureLogging(logging.DEBUG)
        else:
            configureLogging(logging.INFO)
            
        basedir = opts.basedir
        log.debug("base directory: %s" % basedir)

        setup = NimbusSetup(basedir, config)
        setup.validate_environment()

        setup.perform_setup()

        if opts.configpath:
            log.debug("saving settings to %s" % opts.configpath)
            try:
                with open(opts.configpath, 'wb') as f:
                    config.write(f)
            except:
                log.info("Failed to save settings to %s!" % opts.configpath)

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
        
