#!/usr/bin/env python

import optparse
import os
import sys
    
class ARGS:
    """Class for command-line argument constants"""
    
    IMAGE_LONG = "--image"
    IMAGE_HELP = "VM image file to test"

    NETSAMPLE_LONG = "--netsample"
    NETSAMPLE_HELP = "Net sample file obtained from Nimbus service node"
    
    MEMORY_LONG = "--memory"
    MEMORY_HELP = "Amount of MB RAM to assign"

    MOUNTPOINT_LONG= "--mountpoint"
    MOUNTPOINT_HELP = "Mountpoint like 'sda1' to mount image to"


def parsersetup():
    """Return configured command-line parser."""

    ver = "Control Tester version: Infinity and Beyond"
    usage = "see help (-h)."
    parser = optparse.OptionParser(version=ver, usage=usage)

    parser.add_option(ARGS.IMAGE_LONG, dest="image", 
                      metavar="PATH", help=ARGS.IMAGE_HELP)
    
    parser.add_option(ARGS.NETSAMPLE_LONG, dest="netsample", 
                      metavar="PATH", help=ARGS.NETSAMPLE_HELP)
    
    parser.add_option(ARGS.MEMORY_LONG, dest="memory", 
                      metavar="INT", help=ARGS.MEMORY_HELP)
    
    parser.add_option(ARGS.MOUNTPOINT_LONG, dest="mountpoint", 
                      metavar="id", help=ARGS.MOUNTPOINT_HELP)
    
    return parser

def validateargs(opts):
    
    seeh = "see help (-h)"

    if not opts.image:
        raise Exception("%s required, %s." % (ARGS.IMAGE_LONG, seeh))
    
    if not os.path.exists(opts.image):
        raise Exception("%s file specified does not exist: '%s'" % 
                (ARGS.IMAGE_LONG, opts.image))
    
    if not opts.netsample:
        raise Exception("%s required, %s." % (ARGS.NETSAMPLE_LONG, seeh))
    
    if not os.path.exists(opts.netsample):
        raise Exception("%s file specified does not exist: '%s'" % 
                (ARGS.NETSAMPLE_LONG, opts.netsample))
        
    if not opts.memory:
        raise Exception("%s required, %s." % (ARGS.MEMORY_LONG, seeh))
    
    try:
        int(opts.memory)
    except:
        raise Exception("%s requires an integer." % (ARGS.MEMORY_LONG, seeh))
        
    if not opts.mountpoint:
        raise Exception("%s required, %s." % (ARGS.MOUNTPOINT_LONG, seeh))
    
class NetEntry:
    def __init__(self):
        self.nicname = "eth0"
        self.netname = None
        self.mac = None
        self.nettype = "Bridged"
        self.alloctype = "Static"
        self.ip = None
        self.gateway = None
        self.broadcast = None
        self.netmask = None
        self.dns = None
        self.hostname = None
        
    def get_ordered(self, delimeter=";"):
        
        ordered = [ self.nicname, self.netname, self.mac, self.nettype, self.alloctype, self.ip, self.gateway, self.broadcast, self.netmask, self.dns, self.hostname ]
        
        ret = ""
        for thing in ordered:
            ret += self._get_as_null(thing) + delimeter
        return ret
        
    def _get_as_null(self, string):
        if string:
            return string.strip()
        else:
            return "null"

def get_net_entry(path):
    # TODO: parse file...
    ne = NetEntry()
    ne.netname = "public"
    ne.mac = "A2:AA:BB:62:9B:0B"
    ne.ip = "192.168.0.32"
    ne.gateway = "192.168.0.1"
    ne.broadcast = "192.168.0.255"
    ne.netmask = "255.255.255.0"
    ne.dns = "192.168.0.1"
    ne.hostname = "example.com"
    return ne

def main(argv=None):
    if os.name != 'posix':
        print >>sys.stderr, "\nERROR: Only runs on POSIX systems."
        return 3

    if sys.version_info < (2,4):
        print >>sys.stderr, "\nERROR: Your system must have Python version 2.4 or later. "
        print >>sys.stderr, 'Detected version: "'+sys.version+'"'
        return 4
        
    parser = parsersetup()

    try:
        if argv:
            (opts, args) = parser.parse_args(argv[1:])
        else:
            (opts, args) = parser.parse_args()
    except:
        # it thinks -h should be exit(0)
        return 1
    
    try:
        validateargs(opts)
        action = os.getenv("WCONTROL_ACTION")
        if not action:
            print >>sys.stderr, "dear developer, you are calling this wrong"
            return 2
    except:
        print >>sys.stderr, sys.exc_value
        return 2
    
    try:
        ne = get_net_entry(opts.netsample)
        narg = ne.get_ordered()
        
        cmd = "--action %s --name control-test " % action
        cmd += "--memory %d --images '%s' " % (int(opts.memory), opts.image)
        cmd += "--imagemounts '%s' --networking '%s'" % (opts.mountpoint, narg)
        
        print cmd
        
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        name = str(exceptname)
        err = str(sys.exc_value)
        errmsg = "%s: %s" % (name, err)
        print >>sys.stderr, errmsg
        return 2
    
    return 0

if __name__ == "__main__":
    sys.exit(main())
    