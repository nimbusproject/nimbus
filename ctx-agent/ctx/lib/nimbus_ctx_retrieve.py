#!/usr/bin/env python

# Copyright 1999-2010 University of Chicago

# Project home: http://www.nimbusproject.org

import os
import sys
import logging
import optparse
import traceback

try:
    #import elementtree.ElementTree as ET
    from xml.etree import ElementTree as ET #XXX what Python version support this?
except ImportError:
    print "elementtree not installed on the system, trying our backup copy"
    import embeddedET.ElementTree as ET #FIXME this is now broken
    
#local imports
from ctx_exceptions import InvalidInput, InvalidConfig, IncompatibleEnvironment
from ctx_exceptions import UnexpectedError, ProgrammingError
from ctx_logging import addFileLogging, configureLogging, getlog
from actions import RegularInstantiation, AmazonInstantiation
from actions import DefaultRetrieveAction , DefaultConsumeRetrieveResult
from actions import get_broker_okaction, get_broker_erraction
from utils import runexe, starttimer, terminateok, setterminateok
from utils import getlogfilepath, setlogfilepath, write_repl_file
from conf import NS_CTX, NS_CTXTYPES, NS_CTXDESC, VERSION, DEFAULTCONFIG
from conf import getconfig, getCommonConf, getReginstConf, getAmazonConf

# ############################################################
# Commandline arguments
# #########################################################{{{

class ARGS:

    """Class for command-line argument constants"""

    AMAZON="--amazon"
    REGULAR="--regular"
    TRYALL="--tryall"
    
    BOOTSTRAP_PATH="--bootstrap-path"
    POLLTIME="--polltime"
    TRACE="--trace"
    CONFIGPATH="--configpath"
    POWEROFF="--poweroff"

def parsersetup():
    """Return configured command-line parser."""

    ver="Nimbus VM context agent %s, http://www.nimbusproject.org" % VERSION
    usage="see help (-h)."
    parser = optparse.OptionParser(version=ver,usage=usage)

    # ----

    group = optparse.OptionGroup(parser,  "Output options", "-------------")

    group.add_option("-q", "--quiet",
                      action="store_true", dest="quiet", default=False,
                      help="don't log any messages (unless error occurs).")

    group.add_option("-v", "--verbose",
                      action="store_true", dest="verbose",
                      default=False, help="log debug messages")

    group.add_option("-t", ARGS.TRACE,
                      action="store_true", dest="trace", default=False,
                      help="log all debug messages")

    parser.add_option_group(group)

    # ----

    group = optparse.OptionGroup(parser,
                                 "Required action, one is required",
                                 "-------------------------------")

    group.add_option("-z", ARGS.TRYALL,
                     action="store_true", dest="tryall",
                     default=False,
                     help="Try all environments.  Currently there are two. "
                          "The order is to operate in normal cluster " "environment first, then sense Amazon")

    group.add_option("-r", ARGS.REGULAR,
                     action="store_true", dest="regular", default=False,
                     help="Operate in normal cluster environment")

    group.add_option("-a", ARGS.AMAZON,
                     action="store_true", dest="amazon", default=False,
                     help="Operate in Amazon EC2 environment")
                     
    parser.add_option_group(group)

    # ----

    group = optparse.OptionGroup(parser,
                                 "Optional arguments",
                                 "------------------")
    
    group.add_option("-b", ARGS.BOOTSTRAP_PATH, 
                     metavar="PATH", 
                     dest="bootstrap_path", 
                     help="Used with normal cluster environment, this will "
                          "override the default path to local metadata URL "
                          "file")

    group.add_option("-s", ARGS.POLLTIME,
                     dest="polltime", metavar="SECS",
                     help="The number of seconds between polls.")
                     
    group.add_option("-p", ARGS.POWEROFF,
                     action="store_true", dest="poweroff", default=False,
                     help="If there is any problem, the 'problem' script "
                          "in the task directory will be called -- "
                          "this will usually be set to poweroff the VM.")
                            
    group.add_option("-c", ARGS.CONFIGPATH,
                    dest="configpath", metavar="PATH",
                    help="Path to configuration file that overrides the "
                    "default.  If there is a problem with the supplied "
                    "configs, the program will NOT fall back to "
                    "the defaults.")
                          
    parser.add_option_group(group)
    
    return parser

def validateargs(opts):
    """Validate command-line argument combination.

    Required arguments:

    * opts -- Parsed optparse opts object

    Raise InvalidInput if there is a problem.

    """

    actions = [opts.regular, opts.amazon, opts.tryall]

    count = 0
    for action in actions:
        if action:
            count += 1

    seeh = "see help (-h)"

    if not count:
        raise InvalidInput("You must supply an action, %s." % seeh)

    if count != 1:
        raise InvalidInput("You may only supply one action, %s." % seeh)

# }}} END: Commandline arguments

# ############################################################
# Standalone entry and exit
# #########################################################{{{

def mainrun(argv=None):
    """Consume inputs, configure logging, and launch the action.

    Keyword arguments:

    * argv -- Executable's arguments (default None)

    Return exit code:

    * 0 -- Success

    * 1 -- Input problem

    * 2 -- Configuration problem

    * 3 -- Problem with the local or remote system environment.

    * 4 -- Failure to carry out action.

    * 42 -- Programming error, please report if this is a non-modified release

    """

    if os.name != 'posix':
        print >>sys.stderr, "Only runs on POSIX systems."
        return 3

    starttimer()

    parser = parsersetup()

    if argv:
        (opts, args) = parser.parse_args(argv[1:])
    else:
        (opts, args) = parser.parse_args()

    loglevel = None
    if opts.verbose or opts.trace:
        loglevel = logging.DEBUG
    elif opts.quiet:
        loglevel = logging.ERROR
    else:
        loglevel = logging.INFO
    log = configureLogging(loglevel, trace=opts.trace)

    try:
        validateargs(opts)

        #if opts.evaluate:
        #    log.info("EVALUATE MODE ENABLED")

        if opts.configpath:
            config = getconfig(filepath=opts.configpath)
        else:
            config = getconfig(string=DEFAULTCONFIG)
            
        commonconf = getCommonConf(opts, config)
        
        if commonconf.logfilepath:
            addFileLogging(log, commonconf.logfilepath, None, loglevel, trace=opts.trace)
            setlogfilepath(commonconf.logfilepath)
            log.debug("[file logging enabled @ '%s'] " % commonconf.logfilepath)

        if opts.tryall:
            log.debug("action %s" % ARGS.TRYALL)
        elif opts.amazon:
            log.debug("action %s" % ARGS.AMAZON)
        elif opts.regular:
            log.debug("action %s" % ARGS.REGULAR)
            
        if opts.poweroff:
            log.info("OK to poweroff")
            try:
                problemscript = config.get("taskpaths", "problemscript")
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                msg = "%s: %s" % (str(exceptname), str(sys.exc_value))
                raise InvalidConfig(msg)
            
            setterminateok(problemscript)
        
        #######################################
        ##  I. Run one Instantiation action  ##
        #######################################
        
        # try-all could be replaced by supporting multiple action flags and
        # having an order (the order itself could be set via conf)
        iactionresult = None
        if not opts.tryall:
        
            if opts.regular:
                regconf = getReginstConf(opts, config)
                iaction = RegularInstantiation(commonconf, regconf)
            elif opts.amazon:
                ec2conf = getAmazonConf(opts, config)
                iaction = AmazonInstantiation(commonconf, ec2conf)
                
            log.info("Running instantiation action")
            iaction.run()
            iactionresult = iaction.result
        
        else:
            
            # embedded run order right now
            try:
                log.info("First running regular instantiation action")
                regconf = getReginstConf(opts, config)
                reg_iaction = RegularInstantiation(commonconf, regconf)
                reg_iaction.run()
                iactionresult = reg_iaction.result
            except:
                msg = "Problem with regular instantiation action: "
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                msg += "%s: %s" % (str(exceptname), str(sys.exc_value))
                log.error(msg)
                
                log.info("Second, running Amazon instantiation action")
                
                ec2conf = getAmazonConf(opts, config)
                ec2_iaction = AmazonInstantiation(commonconf, ec2conf)
                ec2_iaction.run()
                iactionresult = ec2_iaction.result
            
        # If there was an issue, exception should have been thrown:
        if iactionresult == None:
            raise ProgrammingError("Instantiation action(s) ran to completion but no result?")
            
            
        #############################################################
        ## II. Run one Retrieval action                            ##
        ##     The Instantiation action throws an exception or     ##
        ##     places InstantiationResult in its "result" field.   ##
        #############################################################
        
        ractionresult = None
        
        # only one impl right now:
        raction = DefaultRetrieveAction(commonconf, iactionresult)
        log.info("Running retrieval action")
        raction.run()
        ractionresult = raction.result
        
        if ractionresult == None:
            raise ProgrammingError("Retrieve Action ran to completion but no result?")
            
            
        ###############################################################
        ## III. Run one Consumption action                           ##
        ##      The Retrieval action either throws an exception or   ##
        ##      places RetrieveResult object in its "result" field.  ##
        ###############################################################
            
        # only one impl right now:
        caction = DefaultConsumeRetrieveResult(commonconf, ractionresult, iactionresult)
        log.info("Running consume action")
        caction.run()
        
        return 0

    except InvalidInput, e:
        msg = "Problem with input: %s" % e.msg
        if log:
            log.critical(msg)
        else:
            print >>sys.stderr, msg
        return 1

    except InvalidConfig, e:
        msg = "Problem with configuration: %s" % e.msg
        if log:
            log.critical(msg)
        else:
            print >>sys.stderr, msg
        return 2

    except IncompatibleEnvironment, e:
        msg = "Cannot validate environment: %s" % e.msg
        if log:
            log.critical(msg)
        else:
            print >>sys.stderr, msg
        return 3

    except UnexpectedError, e:
        msg = "Problem executing: %s" % e.msg
        if log:
            log.critical(msg)
        else:
            print >>sys.stderr, msg
        return 4

    except ProgrammingError,e:
        msg = "Programming error.\n"
        msg += "   If this is a non-modified release, please report all"
        msg += "   following output:"
        msg += "   MESSAGE: %s" % e.msg
        if log:
            log.critical(msg)
        else:
            print >>sys.stderr, msg
        traceback.print_tb(sys.exc_info()[2])
        return 42
        
def attempt_ok_broker():
    try:
        ok = get_broker_okaction()
        if not ok:
            # shouldn't be possible, since all would not be OK...
            print >>sys.stderr, "OK action was not configured?"
            return
            
        ok.run()
        
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        name = str(exceptname)
        err = str(sys.exc_value)
        errmsg = "==> Problem sending success report to context broker: %s: %s\n" % (name, err)
        print >>sys.stderr, errmsg

def attempt_error_broker(errcode):
    try:
        filepath = getlogfilepath()
        if not filepath or not os.path.exists(filepath):
            print >>sys.stderr, "==> Problem and no log was available to send to context broker as error report."
            return
            
        erraction = get_broker_erraction()
        if not erraction:
            print >>sys.stderr, "No error reporting action was configured, cannot inform context broker of this problem."
            return
            
        text = ""
        f = open(filepath)
        try:
            for line in f:
                text += line
        finally:
            f.close()
            
        erraction.run(str(errcode), text)
            
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        name = str(exceptname)
        err = str(sys.exc_value)
        errmsg = "==> Problem running broker error report: %s: %s\n" % (name, err)
        print >>sys.stderr, errmsg
    
def attempt_problem_script():
    try:
        problemscript = terminateok()
        if not problemscript:
            # (If the problem was before options were parsed, there is no
            # way to locate the poweroff script)
            return
            
        print >>sys.stderr, "==> Problem and problem script is configured. Running it:\n"
        runexe(problemscript, killtime=0)
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        name = str(exceptname)
        err = str(sys.exc_value)
        errmsg = "==> Problem running terminate script: %s: %s\n" % (name, err)
        print >>sys.stderr, errmsg

def main(argv=None):
    exitcode = mainrun(argv) # run
    if not exitcode:
        attempt_ok_broker() # notify ctx broker of success
    else:
        attempt_error_broker(exitcode) # notify ctx broker of failure
        attempt_problem_script() # do something in VM after failure
    return exitcode
    
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
