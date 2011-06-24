#!/usr/bin/env python

import ConfigParser
import logging
import optparse
import os
import sys
import traceback

from pynimbusconfig import checkssl
from pynimbusconfig import forcessl
from pynimbusconfig.web import newconf
from pynimbusconfig import pathutil
from pynimbusconfig.setuperrors import *

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

def getconfig(filepath):
    if not filepath:
        raise InvalidConfig("filepath was not supplied to getconfig()")
    config = ConfigParser.SafeConfigParser()
    config.read(filepath)
    return config
    
def config_from_key(config, key, section="nimbusweb"):
    setting = config.get(section, key)
    log.debug("%s: %s" % (key, setting))
    return setting

class ARGS:

    """Class for command-line argument constants"""

    BASEDIR_LONG="--basedir"
    BASEDIR="-b"
    BASEDIR_HELP="Path to base web directory (where bin, lib, etc. is)"
    
    CHECKSSL_LONG="--checkssl"
    CHECKSSL="-s"
    CHECKSSL_HELP="Check on SSL setup for standalone server system"
    
    CONFIGPATH_LONG="--conf"
    CONFIGPATH="-c"
    CONFIGPATH_HELP="Path to configuration file"
    
    DEBUG_LONG="--debug"
    DEBUG="-d"
    DEBUG_HELP="Log debug messages"
    
    NEWCONF_LONG="--newconf"
    NEWCONF="-n"
    NEWCONF_HELP="Make sure the internal webapp settings are in line with the standalone system's conf file and filesystem location"
    
    FORCESSL_LONG="--forcessl"
    FORCESSL="-f"
    FORCESSL_HELP="Set up a CA and hostcert, useful outside webapp system. \
It is not interactive."

    FORCECAPATH_LONG="--force-ca-path"
    FORCECAPATH="-p"
    FORCECAPATH_HELP="Absolute path to non-existent CA directory for %s" % FORCESSL_LONG
    
    FORCECERTPATH_LONG="--force-hostcert-path"
    FORCECERTPATH="-e"
    FORCECERTPATH_HELP="Absolute path to non-existent hostcert for %s" % FORCESSL_LONG
    
    FORCEKEYPATH_LONG="--force-hostkey-path"
    FORCEKEYPATH="-k"
    FORCEKEYPATH_HELP="Absolute path to non-existent hostkey for %s" % FORCESSL_LONG
    
    FORCEHOSTNAME_LONG="--force-hostname"
    FORCEHOSTNAME="-m"
    FORCEHOSTNAME_HELP="hostname to use for hostcert in %s" % FORCESSL_LONG

    PRINTPORT_LONG="--printport"
    PRINTPORT="-t"
    PRINTPORT_HELP="Print configured port # to stdout"
    
    PRINTHOST_LONG="--printhost"
    PRINTHOST="-l"
    PRINTHOST_HELP="Print configured host interface to stdout"
    
    PRINTCERTPATH_LONG="--printcertpath"
    PRINTCERTPATH="-i"
    PRINTCERTPATH_HELP="Print configured cert path to stdout and exit"
    
    PRINTKEYPATH_LONG="--printkeypath"
    PRINTKEYPATH="-z"
    PRINTKEYPATH_HELP="Print configured key path to stdout and exit"
    
    # only used by "developer-server.sh"
    INSECUREMODE_LONG="--insecuremode"
    INSECUREMODE="-a"
    INSECUREMODE_HELP="Alter configuration to allow for developer's insecure mode"
    
def validateargs(opts):
    
    actions = [opts.checkssl, opts.forcenewssl, opts.newconf, 
               opts.printport, opts.printcertpath, opts.printkeypath,
               opts.printhost]
    
    count = 0
    for action in actions:
        if action:
            count += 1

    seeh = "see help (-h)"

    if not count:
        raise InvalidInput("You must supply an action, %s." % seeh)

    if count != 1:
        raise InvalidInput("You may only supply one action, %s." % seeh)
        
    if not opts.basedir:
        raise InvalidInput("%s required, %s." % (ARGS.BASEDIR_LONG, seeh))

    if opts.forcenewssl:
        if not opts.forcecapath:
            raise InvalidInput("%s required for %s, %s." % (ARGS.FORCECAPATH_LONG, ARGS.FORCESSL_LONG, seeh))
        if not opts.forcecertpath:
            raise InvalidInput("%s required for %s, %s." % (ARGS.FORCECERTPATH_LONG, ARGS.FORCESSL_LONG, seeh))
        if not opts.forcekeypath:
            raise InvalidInput("%s required for %s, %s." % (ARGS.FORCEKEYPATH_LONG, ARGS.FORCESSL_LONG, seeh))
        if not opts.forcehostname:
            raise InvalidInput("%s required for %s, %s." % (ARGS.FORCEHOSTNAME_LONG, ARGS.FORCESSL_LONG, seeh))
    
def parsersetup():
    """Return configured command-line parser."""

    ver="Nimbus webapp setup"
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
    
    group.add_option(ARGS.INSECUREMODE, ARGS.INSECUREMODE_LONG,
                     action="store_true", dest="insecuremode", default=False,
                     help=ARGS.INSECUREMODE_HELP)

    parser.add_option_group(group)

    # ----

    group = optparse.OptionGroup(parser,
                                 "Required action, one is required",
                                 "-------------------------------")

    group.add_option(ARGS.CHECKSSL, ARGS.CHECKSSL_LONG,
                     action="store_true", dest="checkssl", default=False,
                     help=ARGS.CHECKSSL_HELP)
    
    group.add_option(ARGS.PRINTPORT, ARGS.PRINTPORT_LONG,
                     action="store_true", dest="printport", default=False,
                     help=ARGS.PRINTPORT_HELP)
    
    group.add_option(ARGS.PRINTHOST, ARGS.PRINTHOST_LONG,
                     action="store_true", dest="printhost", default=False,
                     help=ARGS.PRINTHOST_HELP)
    
    group.add_option(ARGS.PRINTCERTPATH, ARGS.PRINTCERTPATH_LONG,
                     action="store_true", dest="printcertpath", default=False,
                     help=ARGS.PRINTCERTPATH_HELP)
    
    group.add_option(ARGS.PRINTKEYPATH, ARGS.PRINTKEYPATH_LONG,
                     action="store_true", dest="printkeypath", default=False,
                     help=ARGS.PRINTKEYPATH_HELP)
    
    group.add_option(ARGS.NEWCONF, ARGS.NEWCONF_LONG,
                     action="store_true", dest="newconf", default=False,
                     help=ARGS.NEWCONF_HELP)
    
    group.add_option(ARGS.FORCESSL, ARGS.FORCESSL_LONG,
                     action="store_true", dest="forcenewssl", default=False,
                     help=ARGS.FORCESSL_HELP)

    parser.add_option_group(group)

    # ----

    group = optparse.OptionGroup(parser,
                                 "Required options for forcessl",
                                 "----------------------------")

    group.add_option(ARGS.FORCECAPATH, ARGS.FORCECAPATH_LONG,
                     dest="forcecapath", metavar="DIR",
                     help=ARGS.FORCECAPATH_HELP)
    
    group.add_option(ARGS.FORCECERTPATH, ARGS.FORCECERTPATH_LONG,
                     dest="forcecertpath", metavar="PATH",
                     help=ARGS.FORCECERTPATH_HELP)

    group.add_option(ARGS.FORCEKEYPATH, ARGS.FORCEKEYPATH_LONG,
                     dest="forcekeypath", metavar="PATH",
                     help=ARGS.FORCEKEYPATH_HELP)
    
    group.add_option(ARGS.FORCEHOSTNAME, ARGS.FORCEHOSTNAME_LONG,
                     dest="forcehostname", metavar="HOSTNAME",
                     help=ARGS.FORCEHOSTNAME_HELP)

    parser.add_option_group(group)

    # ----
    
    return parser
    
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
        
        confdebug = config.get("nimbusweb", "debug")
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
        
        insecuremode = opts.insecuremode
        if insecuremode:
            log.debug("**** This is insecure developer mode ****")
        else:
            log.debug("secure mode")
        
        certconf = config_from_key(config, "ssl.cert")
        keyconf = config_from_key(config, "ssl.key")
        cadir = config_from_key(config, "ca.dir")
        timezone = config_from_key(config, "timezone")
        port = config_from_key(config, "webserver.port")
        host = config_from_key(config, "webserver.host")
        printurl = config_from_key(config, "print.url")
        accountprompt = config_from_key(config, "account.prompt")
        expire_hours = config_from_key(config, "token.expire_hours")
        try:
            expire_hours = int(expire_hours)
        except:
            raise InvalidConfig("invalid token.expire_hours setting, not an integer?")
                
        # 4. Validate base directory
        
        if not pathutil.is_absolute_path(basedir):
            raise IncompatibleEnvironment("Base directory setting is not absolute, have you been altering the stanadalone launch code?")
    
        pathutil.ensure_dir_exists(basedir, "base", ": have you been altering the stanadalone launch code?")
            
        # 5. Run one subcommand
        
        if opts.checkssl:
            checkssl.run(basedir, certconf, keyconf, log)
            
        if opts.newconf:
            newconf.run(basedir, timezone, accountprompt, log, 
                    printdebugoutput, insecuremode, printurl, expire_hours, 
                    cadir)
        
        if opts.printport:
            if not port:
                raise IncompatibleEnvironment("There is no 'webserver.port' configuration")
            try:
                port = int(port)
            except:
                raise IncompatibleEnvironment("'webserver.port' configuration is not an integer?")
            print port
        
        if opts.printhost:
            if not host:
                raise IncompatibleEnvironment("There is no 'webserver.host' configuration")
            print host

        if opts.printcertpath:
            if not certconf:
                raise IncompatibleEnvironment("There is no 'ssl.cert' configuration")
            if not pathutil.is_absolute_path(certconf):
                certconf = pathutil.pathjoin(basedir, certconf)
                log.debug("ssl.cert was a relative path, converted to '%s'" % certconf)
            print certconf
            
        if opts.printkeypath:
            if not keyconf:
                raise IncompatibleEnvironment("There is no 'ssl.key' configuration")
            if not pathutil.is_absolute_path(keyconf):
                keyconf = pathutil.pathjoin(basedir, keyconf)
                log.debug("ssl.key was a relative path, converted to '%s'" % keyconf)
            print keyconf

        if opts.forcenewssl:
            forcessl.run(basedir, opts.forcecapath, opts.forcecertpath,
                         opts.forcekeypath, opts.forcehostname, log)

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
        
