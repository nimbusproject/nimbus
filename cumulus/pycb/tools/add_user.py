#!/usr/bin/python
import getpass
import string
import random
import os
import sys
from ConfigParser import SafeConfigParser
import pycb
import pynimbusauthz
import shutil
import pycb.cbPosixSecurity
from pynimbusauthz.cmd_opts import cbOpts

def setup_options(argv):

    u = """[options] <display name>"""
    (parser, all_opts) = pynimbusauthz.get_default_options(u)

    opt = cbOpts("password", "p", "set password.  If not specified and we are not asked to generate one, the user will be prompted.", None)
    all_opts.append(opt)
    opt = cbOpts("new", "n", "This is a new user", False, flag=True)
    all_opts.append(opt)
    opt = cbOpts("genkey", "g", "Generate and echo a password", False, flag=False)
    all_opts.append(opt)
    opt = cbOpts("remove", "r", "Remove the user", False, flag=False)
    all_opts.append(opt)
    opt = cbOpts("force", "f", "Force the specified action to occur without warning", False, flag=False)
    all_opts.append(opt)
    opt = cbOpts("id", "i", "Use this user ID.  If not specified one is generated.", None)
    all_opts.append(opt)

    (o, args) = pynimbusauthz.parse_args(parser, all_opts, argv)

    return (o, args)


def add_user_generate_pw(len):
    chars = string.letters + string.digits
    newpasswd = ""
    for i in range(len):
        newpasswd = newpasswd + random.choice(chars)
    return newpasswd

def main(argv=sys.argv[1:]):

    auth = pycb.config.auth

    (opts, args) = setup_options(argv)
    if len(args) == 0:
        print "You must provide a display name. See --help for usage"
        return 1

    display_name = args[0]

    if opts.genkey:
        opts.password = add_user_generate_pw(42)
    if opts.new:
        # make new user
        if opts.password == None:
            pw1 = getpass.getpass(prompt="Enter new password:").strip()
            pw2 = getpass.getpass(prompt="Retype new password:").strip()
            if pw1 != pw2:
                print "Sorry, passwords do not match"
                return 1
            opts.password = pw1
        if opts.id == None:
            opts.id = add_user_generate_pw(21)

        auth.create_user(display_name, opts.id, opts.password, opts)
        print "Created a new user with:"
        print "ID:  %s Key: %s" % (opts.id, opts.password)

    else:
        try:
            user_id = auth.get_user_id_by_display(display_name)
        except:
            user_id = None

        if opts.id == None:
            opts.id = auth.get_user_id_by_display(display_name)
        if opts.id != user_id:
            print "WARNING: The provided user id does not match the display name"
    try:
        u = auth.get_user(opts.id)
    except:
        u = None
    if u == None:
        print "The user with id: %s does not exist" % (opts.id)
        return 2

    if opts.password != None:
        opts.id = u.set_user_pw(opts.password)
    if opts.remove:
        u.remove_user()

    return 0

if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

    


