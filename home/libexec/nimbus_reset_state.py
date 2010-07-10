#!/usr/bin/env python

"""
Calls out to database reset scripts in a sane way
"""

import commands
import logging
from optparse import OptionParser
from optparse import SUPPRESS_HELP
import os
import sys
from nimbusweb.setup.setuperrors import *
from nimbusweb.setup import runutil
import pynimbusauthz
from pynimbusauthz.cmd_opts import cbOpts
from pynimbusauthz.user import User

logger = logging.getLogger("nimbusreset")

def reset_accounting(nh):
    cmd = os.path.join(nh, "services/share/nimbus/acctdb-reset.sh")
    if not os.path.exists(cmd):
        raise CLIError('EPATH', "Can not find reset script: %s" % cmd)
    (exit, stdout, stderr) = runutil.runexe(cmd, logger, killtime=0)
    if exit:
        raise CLIError('ECMDLINE', "Problem:\nexit code: %d\nstdout: '%s'\nstderr: '%s'" % (exit, stdout, stderr))

def reset_vmstate(nh):
    cmd = os.path.join(nh, "services/share/nimbus/servicedb-reset.sh")
    if not os.path.exists(cmd):
        raise CLIError('EPATH', "Can not find reset script: %s" % cmd)
    (exit, stdout, stderr) = runutil.runexe(cmd, logger, killtime=0)
    if exit:
        raise CLIError('ECMDLINE', "Problem:\nexit code: %d\nstdout: '%s'\nstderr: '%s'" % (exit, stdout, stderr))


def reset_users(nh, clean_pattern='%'):
    dbobj = pynimbusauthz.get_db_connection_string()
    users_to_delete = User.find_user_by_friendly(dbobj, clean_pattern)
    for user in users_to_delete:
        user.destroy_brutally()

def get_nimbus_home():
    """Determines home directory of Nimbus install we are using.
    
    First looks for a NIMBUS_HOME enviroment variable, else assumes that
    the home directory is the parent directory of the directory with this
    script.
    """
    nimbus_home = os.getenv("NIMBUS_HOME")
    if not nimbus_home:
        script_dir = os.path.dirname(__file__)
        nimbus_home = os.path.dirname(script_dir)
    if not os.path.exists(nimbus_home):
        raise CLIError('ENIMBUSHOME', "NIMBUS_HOME must refer to a valid path:  %s" % (nimbus_home))

    return nimbus_home

def get_user_input(o, valuename, default=None, required=True):
    answer = None
    question = valuename + (default and ("(%s): " % default) or ": ")
    while not answer:
        value = raw_input(valuename+": ")
        if value:
            answer = value.strip()
        elif default:
            answer = default
        if not answer:
            if required:
                o.out_file.write("Invalid input. You must specify a value. Or hit Ctrl-C to give up.\n")
                o.out_file.flush()
            else:
                return None

    return answer



ACCOUNTING_HELP="IaaS accounting database, the number of minutes/credits used for all users"
VMSTATE_HELP="IaaS database, the runtime tracking of VM/VMM state, network leases, etc."
USERS_HELP="IaaS/Cumulus user database and Cumulus files"

def setup_options(argv):

    u = """[action(s)] [options]
Deletes state.  Dangerous program.  Interactive unless using --force.
    """
    
    (parser, all_opts) = pynimbusauthz.get_default_options(u)
    
    for opt in all_opts:
        if opt.long == "--batch":
            all_opts.remove(opt)
    # modified list, need another loop..
    for opt in all_opts:
        if opt.long == "--quiet":
            all_opts.remove(opt)
    
    opt = cbOpts("all", "a", "Reset all database state: IaaS, IaaS accounting, IaaS/Cumulus users, Cumulus files", False, flag=True)
    all_opts.append(opt)
    
    opt = cbOpts("accounting", "m", "Resets " + ACCOUNTING_HELP, False, flag=True)
    all_opts.append(opt)
    
    opt = cbOpts("vmstate", "e", "Resets " + VMSTATE_HELP, False, flag=True)
    all_opts.append(opt)
    
    opt = cbOpts("users", "u", "Resets " + USERS_HELP, False, flag=True)
    all_opts.append(opt)
    
    opt = cbOpts("force", "f", "Skips confirmation question", False, flag=True)
    all_opts.append(opt)
    
    (o, args) = pynimbusauthz.parse_args(parser, all_opts, argv)
    return (o, args, parser)

def main(argv=sys.argv[1:]):

    try:
        (o, args, p) = setup_options(argv)
        
        logger.setLevel(logging.DEBUG)
        formatstring = "%(message)s"
        formatter = logging.Formatter(formatstring)
        
        ch = logging.StreamHandler()
        if o.verbose > 1:
            ch.setLevel(logging.DEBUG)
        else:
            ch.setLevel(logging.INFO)
        ch.setFormatter(formatter)
        logger.addHandler(ch)
        
        if o.all:
            o.accounting = True
            o.vmstate = True
            o.users = True
            
        count = 0
        actions = [o.accounting, o.vmstate, o.users]
        for action in actions:
            if action:
                count += 1
            
        if count == 0:
            raise CLIError('EUSER', "No action, see --help (-h)")
            
        nh = get_nimbus_home()
        
        if not o.force:
            msg = "\nAbout to reset the following:"
            if o.accounting:
                msg += "\n - %s" % ACCOUNTING_HELP
            if o.vmstate:
                msg += "\n - %s" % VMSTATE_HELP
            if o.users:
                msg += "\n - %s" % USERS_HELP
        
            msg += "\n\nCurrent NIMBUS_HOME: %s" % nh
            msg += "\n\nThis is not recoverable. Proceed?\n\n"
            
            o.out_file.write(msg)
            o.out_file.flush()
        
            while True:
                answer = get_user_input(o, "Type 'yes'")
                if answer == "yes":
                    break
        
        if o.accounting:
            o.out_file.write("\n* Resetting %s\n" % ACCOUNTING_HELP)
            o.out_file.flush()
            reset_accounting(nh)
            o.out_file.write("Done.\n")
            o.out_file.flush()
        if o.vmstate:
            o.out_file.write("\n* Resetting %s\n" % VMSTATE_HELP)
            o.out_file.flush()
            reset_vmstate(nh)
            o.out_file.write("Done.\n")
            o.out_file.flush()
        if o.users:
            o.out_file.write("\n* Resetting %s\n" % USERS_HELP)
            o.out_file.flush()
            reset_users(nh)
            o.out_file.write("Done.\n")
            o.out_file.flush()
        
    except CLIError, clie:
        print clie
        return clie.get_rc()
    return 0

if __name__ == "__main__":
    sys.exit(main())
