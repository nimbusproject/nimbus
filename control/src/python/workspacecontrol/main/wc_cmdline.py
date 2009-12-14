import os
import sys
import traceback

import wc_core
import wc_deprecated
import wc_optparse
from workspacecontrol.api.exceptions import *

def main(argv=None):
    if os.name != 'posix':
        print >>sys.stderr, "Only runs on POSIX systems."
        return 3
        
    parser = wc_optparse.parsersetup()

    if argv:
        (opts, args) = parser.parse_args(argv[1:])
    else:
        (opts, args) = parser.parse_args()
        
    try:
        dbgmsgs = wc_deprecated.deprecated_args(opts)
        
        # From here 'down' there is no concept of a commandline program, only
        # 'args' which could be coming from any kind of protocol based request.
        # To make such a thing, construct an opts objects with the expected
        # member names (see the wc_args module) and pass it in.
        
        wc_core.core(opts, dbgmsgs=dbgmsgs)
        
    except InvalidInput, e:
        msg = "Problem with input: %s" % e.msg
        print >>sys.stderr, msg
        return 1

    except InvalidConfig, e:
        msg = "Problem with configuration: %s" % e.msg
        print >>sys.stderr, msg
        return 2

    except IncompatibleEnvironment, e:
        msg = "Problem with environment: %s" % e.msg
        print >>sys.stderr, msg
        return 3

    except UnexpectedError, e:
        msg = "Problem executing: %s" % e.msg
        print >>sys.stderr, msg
        return 4
        
    except ProgrammingError,e:
        msg = "*** Developer error ***\n"
        msg += "   If this is a non-modified release, please report all\n"
        msg += "   the following output:\n"
        msg += "%s" % e.msg
        print >>sys.stderr, msg
        traceback.print_tb(sys.exc_info()[2])
        return 42
        
    except:
        msg = "*** Unexpected error ***\n"
        msg += "   If this is a non-modified release, please report all\n"
        msg += "   the following output:\n"
        print >>sys.stderr, msg
        traceback.print_tb(sys.exc_info()[2])
        return 42
    
    return 0
    
    
if __name__ == "__main__":
    exitcode = main()
    if exitcode != 0:
        sys.stderr.write("\nExiting with error code: %d\n\n" % exitcode)
    sys.stderr.flush()
    sys.exit(exitcode)
    