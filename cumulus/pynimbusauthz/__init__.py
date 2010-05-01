import sqlite3
import os
import sys
import string
import random
from optparse import OptionParser
from optparse import SUPPRESS_HELP
from pynimbusauthz.cmd_opts import cbOpts

alias_type_s3 = "s3"
alias_type_x509 = "x509"
alias_type_ssh = "ssh"
alias_type_unix = "unix"

alias_types = {}
alias_types[alias_type_s3] = 0
alias_types[alias_type_x509] = 1
alias_types[alias_type_ssh] = 2
alias_types[alias_type_unix] = 3

object_type_s3 = 's3'
object_type_gridftp = 'gridftp'
object_type_hdfs = 'hdfs'

object_types = {}
object_types[object_type_s3] = 0
object_types[object_type_gridftp] = 1
object_types[object_type_hdfs] = 2

Version = "0.1"

def reverse_lookup_type(dict, val):
    for x in dict.keys():
        if dict[x] == val:
            return x
    return None

def get_default_options(u):
    global Version
    version = "%prog " + (Version)
    parser = OptionParser(usage=u, version=version)

    all_opts = []

    opt = cbOpts("quiet", "q", "Display no output", False, flag=True)
    all_opts.append(opt)
    opt = cbOpts("verbose", "V", "Display much output", 1, count=True)
    all_opts.append(opt)
    opt = cbOpts("instream", "I", SUPPRESS_HELP, None)
    all_opts.append(opt)
    opt = cbOpts("outstream", "O", SUPPRESS_HELP, None)
    all_opts.append(opt)

    return (parser, all_opts)

def print_msg(opts, level, msg):
    if opts.quiet:
        return
    if level > opts.verbose:
        return
    opts.out_file.write(msg)
    opts.out_file.write('\n')
    opts.out_file.flush()

def random_string_gen(len):
    chars = string.letters + string.digits
    newpasswd = ""
    for i in range(len):
        newpasswd = newpasswd + random.choice(chars)
    return newpasswd

def get_db_connection_string():
    con_str = os.environ['NIMBUS_AUTHZ_DB']
    return con_str

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

