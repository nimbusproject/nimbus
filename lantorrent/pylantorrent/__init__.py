import sys
import os
import logging
import traceback
from ConfigParser import SafeConfigParser
from optparse import OptionParser
from optparse import SUPPRESS_HELP

import hmac
try:
    from hashlib import sha1 as sha
    from hashlib import sha256 as sha256
except ImportError:
    import sha
import base64
import uuid

Version = "0.1"

def log(level, msg, tb=None):
    logging.log(level, msg)
    if tb != None:
        logging.log(level, "Stack trace")
        logging.log(level, "===========")
        stack = tb.format_exc()
        logging.log(level, stack)
        logging.log(level, "===========")
        logging.log(level, sys.exc_info()[0])

def create_endpoint_entry(host, dest_files, data_size, port=2893, block_size=128*1024, degree=1, rename=True):

    final = {}
    requests = []
    for df in dest_files:
        ent = {}
        ent['filename'] = df
        ent['rename'] = rename
        ent['id'] = str(uuid.uuid1())
        requests.append(ent)
          
    final['requests'] = requests
    final['host'] = host
    final['port'] = port
    final['block_size'] = block_size
    final['degree'] = degree
    final['length'] = data_size

    return final


class VConfig(object):

    def __init__(self):

        if 'LANTORRENT_HOME' not in os.environ:
            self.lt_home = os.path.expanduser("~/.lantorrent")
            emsg = "the env LAN_TORRENT_HOME is not set, using %s" % (self.lt_home)
            try:
                os.mkdir(self.lt_home)
            except:
                pass
            self.set_defaults()
            logging.basicConfig(filename=self.logfile, level=self.log_level,
                format="%(asctime)s - %(levelname)s - %(message)s")
            log(logging.WARNING, emsg)
        else:
            self.lt_home = os.environ['LANTORRENT_HOME']
            self.set_defaults()
            ini_file = os.path.join(self.lt_home, "etc/lt.ini")

            try:
                self.load_settings(ini_file)
            except:
                emsg = "failed to load %s, using defaults" % (ini_file)
                log(logging.WARNING, emsg, traceback)
            logging.basicConfig(filename=self.logfile, level=self.log_level,
                format="%(asctime)s - %(levelname)s - %(message)s")

    def set_defaults(self):
        self.pw = "nimbus"
        self.logfile = "%s/lantorrent.log" % (self.lt_home)
        self.log_level = logging.DEBUG
        self.dbfile = "%s/reqs.db" % (self.lt_home)
        self.db_error_max = 10
        self.insert_delay = 30

    def load_settings(self, ini_file):
        log_levels = {'debug': logging.DEBUG,
            'info': logging.INFO,
            'warning': logging.WARNING,
            'error': logging.ERROR,
            'critical': logging.CRITICAL}

        s = SafeConfigParser()
        s.readfp(open(ini_file, "r"))
        self.pw = s.get("security", "password")
        prog = os.path.splitext(os.path.basename(sys.argv[0]))[0]
        self.logfile = s.get("log", "file").replace("@LANTORRENT_HOME@", self.lt_home).replace("@PGM@", prog)
        try:
            log_level_str = s.get("log", "level")
            self.log_level = log_levels[log_level_str]
        except Exception, ex:
            pass
        try:
            self.dbfile = s.get("db", "file").replace("@LANTORRENT_HOME@", self.lt_home)
        except:
            pass

config = VConfig()

def get_auth_hash(header_str):
    myhmac = hmac.new(config.pw, digestmod=sha)
    header_str = header_str.replace("\n", "")
    header_str = header_str.replace("\r", "")
    myhmac.update(header_str)
    auth_hash = base64.encodestring(myhmac.digest()).strip()
    return auth_hash


class cbOpts(object):

    def __init__(self, long, short, description, default, vals=None, range=None, flag=None, count=False, long_help=False):
        self.long = "--" + long.replace('_', '-')
        self.dest = long
        self.short = "-" + short
        self.vals = vals
        self.default = default
        self.flag = flag
        self.range = range
        self.description = description
        self.count = count
        self.long_help = long_help

    def get_error_msg(self):
        if self.flag != None:
            emsg = "The option  %s | %s is a flag" % (self.short, self.long)
            return emsg

        if self.range != None:
            emsg = "The option  %s | %s must be between %s and %s" % (self.short, self.long, self.range[0], self.range[1])
            return emsg

        if self.vals != None:
            emsg = "The value for %s | %s must be: { " % (self.short, self.long)
            delim = ""
            for v in self.vals:
                emsg = emsg + delim + str(v)
                delim = " | "
            emsg = emsg + "}"

            return emsg

        return "Error"

    def validate(self, options):

        try:
            val = getattr(options, self.dest)
        except:
            emsg = self.get_error_msg()
            raise Exception(emsg)

        if val == None:
            return
        if self.flag != None:
            return
        if self.range != None:
            if len(self.range) == 2:
                if float(val) == -1.0:
                    if float(self.range[0]) != -1.0 and float(self.range[1]) != -1.0:
                        raise Exception("you specified a value out of range")

                    else:
                        return

                if (float(val) < float(self.range[0]) and float(self.range[0]) != -1.0) or (float(val) > float(self.range[1]) and float(self.range[1]) != -1.0):
                    emsg = self.get_error_msg()
                    raise Exception(emsg)
            return

        if self.vals != None:
            for v in self.vals:
                if val == v:
                    return

            emsg = self.get_error_msg()
            raise Exception(emsg)

    def get_description(self):
        if self.range != None:
            msg = self.description + " : between %s - %s" % (self.range[0], self.range[1])
            return msg

        if self.vals != None:
            msg = self.description + " : {"
            delim = ""
            for v in self.vals:
                msg = msg + delim + str(v)
                delim = " | "
            msg = msg + "}"

            return msg

        return self.description

    def add_opt(self, parser):
        if self.flag != None:
            if self.default:
                a = "store_false"
            else:
                a = "store_true"
            parser.add_option(self.short, self.long, dest=self.dest, default=self.default,
                action=a,
                help=self.get_description())
            return

        if self.count:
            parser.add_option(self.short, self.long, dest=self.dest,
                default=self.default,
                action="count",
                help=self.get_description())
            return

        parser.add_option(self.short, self.long, dest=self.dest,
            default=self.default, type="string",
            help=self.get_description())


__author__ = 'bresnaha'

def get_default_options(u):
    global Version
    version = "%prog " + (Version)
    parser = OptionParser(usage=u, version=version)

    all_opts = []

    opt = cbOpts("batch", "b", "Set to batch mode for machine parsing", False, flag=True)
    all_opts.append(opt)
    opt = cbOpts("quiet", "q", "Display no output", False, flag=True)
    all_opts.append(opt)
    opt = cbOpts("verbose", "V", "Display much output, repeat to increase level", 1, count=True)
    all_opts.append(opt)
    opt = cbOpts("instream", "I", SUPPRESS_HELP, None)
    all_opts.append(opt)
    opt = cbOpts("outstream", "O", SUPPRESS_HELP, None)
    all_opts.append(opt)

    return (parser, all_opts)


def parse_args(p, all_opts, argv):

    for o in all_opts:
        o.add_opt(p)
    (options, args) = p.parse_args(args=argv)

    if options.outstream == None:
        options.out_file = sys.stdout
    else:
        options.out_file = open(options.outstream, "w")
    if options.instream == None:
        options.in_file = sys.stdin
    else:
        options.in_file = open(options.instream, "r")

    for o in all_opts:
        o.validate(options)

    return (options, args)

