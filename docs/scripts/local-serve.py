#!/usr/bin/python

import BaseHTTPServer
import ConfigParser
import optparse
import os
import SimpleHTTPServer
import sys

# ############################################################
# Exceptions
# #########################################################{{{

class IncompatibleEnvironment(Exception):
    
    """Exception for when something has determined a problem with the
    deployment environment."""
    
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg

# }}} END: Exceptions

# ############################################################
# Run
# #########################################################{{{

def run(port, address):
    
    print "Base directory: %s" % os.getcwd()
    
    handler_class = SimpleHTTPServer.SimpleHTTPRequestHandler
    server_class = BaseHTTPServer.HTTPServer
    
    server_address = (address, port)
    handler_class.protocol_version = "HTTP/1.0"
    httpd = server_class(server_address, handler_class)

    sa = httpd.socket.getsockname()
    #print "Serving HTTP on", sa[0], "port", sa[1], "..."
    print "\nURL: http://%s:%d" % (address, sa[1])
    httpd.serve_forever()

# }}} END: Run

# ############################################################
# Commandline arguments
# #########################################################{{{

class ARGS:

    """Class for command-line argument constants"""

    PORT_LONG="--port"
    PORT="-p"
    PORT_HELP="Override port number (default is 8888)"
    
    
    ADDR_LONG="--address"
    ADDR="-a"
    ADDR_HELP="Address to bind to (default is 'localhost')"
    
def parsersetup():
    """Return configured command-line parser."""

    ver="Local build and serve docs for: http://www.nimbusproject.org"
    usage="see help (-h)."
    parser = optparse.OptionParser(version=ver, usage=usage)

    # ----

    group = optparse.OptionGroup(parser,  "Options", "-------------")

    group.add_option(ARGS.PORT, ARGS.PORT_LONG,
                     dest="port", metavar="PORT",
                     help=ARGS.PORT_HELP)
    
    group.add_option(ARGS.ADDR, ARGS.ADDR_LONG,
                     dest="address", metavar="HOST/IP",
                     help=ARGS.ADDR_HELP)

    parser.add_option_group(group)

    # ----
    
    return parser

# }}} END: Commandline arguments

# ############################################################
# Standalone entry and exit
# #########################################################{{{

def main(argv=None):

    if os.name != 'posix':
        print >>sys.stderr, "Only runs on POSIX systems."
        return 3

    parser = parsersetup()

    if argv:
        (opts, args) = parser.parse_args(argv[1:])
    else:
        (opts, args) = parser.parse_args()
        
    port = 8888
    if opts.port:
        port = int(opts.port)
        
    address = "localhost"
    if opts.address:
        address = opts.address
    
    run(port, address)

if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print "\n\nExiting..."

# }}} END: Standalone entry and exit


