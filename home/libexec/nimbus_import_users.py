#!/usr/bin/env python

"""
Import users from a flat file
"""

import os
import sys
from fileinput import FileInput
import csv
from itertools import izip
import tempfile
import traceback

import pynimbusauthz
import pycb.tools
from pynimbusauthz.cmd_opts import cbOpts
from nimbusweb.setup.setuperrors import CLIError
import nimbus_list_users
import nimbus_edit_user
import nimbus_remove_user
import nimbus_new_user

_fields = ["dn", "canonical_id", "access_id", "access_secret", 
        "display_name", "group"]
_fields_csv = ','.join(_fields)

def setup_options(argv):
    u = """[options] [file]
Import Nimbus users

If file is not specified, standard input will be used"""
    (parser, all_opts) = pynimbusauthz.get_default_options(u)

    opt = cbOpts('dryrun', 'd', 'Don\'t actually do anything, just print', False, 
            flag=True)
    all_opts.append(opt)

    opt = cbOpts('delim', 'D', 'Character between columns of input data', ',')
    all_opts.append(opt)
    
    #opt = cbOpts('fields', 'f', 'Specify field order in output, from the following: '+
    #        _fields_csv, None)
    #all_opts.append(opt)

    #opt = cbOpts('ignore', 'i', 'Ignore specified user(s)')
    #all_opts.append(opt)

    opt = cbOpts('update', 'u', 'Perform updates of existing users', False, flag=True)
    all_opts.append(opt)
    
    opt = cbOpts('remove', 'r', 'Remove users that exist locally but not in input file', 
            False, flag=True)
    all_opts.append(opt)
    
    opt = cbOpts('changes_only', 'c', 'Only report changed and unmatched users', 
            False, flag=True)
    all_opts.append(opt)
    
    (o, args) = pynimbusauthz.parse_args(parser, all_opts, argv)

    return (o, args, parser)

def read_users(input, delimiter=',', fields=_fields):
    """Read in users from flat text input. 
    
    If input is None or an empty list, STDIN is used.
    """
    f = FileInput(input)
    reader = csv.reader(f, delimiter=delimiter)

    users = {}
    for row in reader:

        if len(row) != len(fields):
            raise CLIError('EUSER', 
                    'Line %s of input has incorrect number of fields. Expecting "%s"' %
                    (f.lineno(), delimiter.join(fields)))
        u = dict([(k, v) for k, v in izip(fields, row)])

        name = u['display_name']
        grp = _fix_group(u['group'])
        u['group'] = grp
        if name in users:
            raise CLIError('EUSER', 'Duplicate entry present for user "%s"' % name)
        users[name] = u
    
    return users

class printer_obj(object):
    def __init__(self):
        pass
def _fix_group(group):
    #workaround bug in 2.5 nimbus-edit-user. expects zero padded group number
    if group.isdigit() and group[0] != '0':
        return '0'+group
    return group

def update_user(current, desired, opts):
    args = []
    if current['dn'] != desired['dn']:
        args.extend(['-s', desired['dn']])
    if current['access_id'] != desired['access_id']:
        args.extend(['-a', desired['access_id']])
    if current['access_secret'] != desired['access_secret']:
        args.extend(['-p', desired['access_secret']])
    if current['group'] != desired['group']:
        args.extend(['-g', _fix_group(desired['group'])])

    if current['canonical_id'] != desired['canonical_id']:
        # canonical ID cannot be updated (is used in cumulus paths)
        return "CANNOT_UPDATE"

    if args:
        name = current['display_name']
        args.extend(['-q', name])

        if not opts.update:
            pynimbusauthz.print_msg(opts, 2, 
                    "Not updating mismatched user %s: --update is not specified" %
                    name)
            return "MISMATCHED"

        pynimbusauthz.print_msg(opts, 2, "Calling nimbus-edit-user with args: " +
                str(args))

        if opts.dryrun:
            return "UPDATED"

        ok = False
        try:
            ok = nimbus_edit_user.main(args) == 0
        except:
            pynimbusauthz.print_msg(opts, 2, "Error: " + traceback.format_exc())
        return ok and "UPDATED" or "UPDATE_FAILED" 

    return "UNCHANGED"

def new_user(user, opts):
    args = ['-s', user['dn'], '-i', user['canonical_id'], '-a', user['access_id'],
            '-p', user['access_secret'], '-g', _fix_group(user['group']),
            '-P', '-q', user['display_name']]

    pynimbusauthz.print_msg(opts, 2, "Calling nimbus-new-user with args: " +
            str(args))

    if opts.dryrun:
        return "ADDED"

    ok = False
    try:
        ok = nimbus_new_user.main(args) == 0
    except:
        pynimbusauthz.print_msg(opts, 2, "Error: " + traceback.format_exc())
    return ok and "ADDED" or "ADD_FAILED"

def remove_user(user_name, opts):
    args = [user_name]
    
    if not opts.remove:
        pynimbusauthz.print_msg(opts, 2, 
                "Not removing extra user %s: --remove is not specified" %
                user_name)
        return "EXTRA"

    pynimbusauthz.print_msg(opts, 2, "Calling nimbus-remove-user with args: " +
            str(args))

    if opts.dryrun:
        return "REMOVED"

    ok = False
    try:
        ok = nimbus_remove_user.main(args) == 0
    except:
        pynimbusauthz.print_msg(opts, 2, "Error: " + traceback.format_exc())
    return ok and "REMOVED" or "REMOVE_FAILED"

def walk_users(current, desired, opts):

    def print_record(o): 
        if not opts.changes_only or o.state != 'UNCHANGED':
            pycb.tools.print_report(o, 'display_name,state', opts)

    all_ok = True

    for name,user in desired.iteritems():
        o = printer_obj()
        o.display_name = name

        current_user = current.pop(name, None)
        if current_user:
            o.state = update_user(current_user, user, opts)
            if o.state == 'UPDATE_FAILED':
                all_ok = False

        else:
            o.state = new_user(user, opts)
            if o.state == 'ADD_FAILED':
                all_ok = False
        print_record(o)

    # anything left in current users is potentially to be removed
    for name,user in current.iteritems():
        o = printer_obj()
        o.display_name = name
        o.state = remove_user(name, opts)
        if o.state == 'REMOVE_FAILED':
            all_ok = False
        
        print_record(o)

    return all_ok

def main(argv=sys.argv[1:]):

    try:
        (opts, args, p) = setup_options(argv)

        file_users = read_users(args, delimiter=opts.delim)

        file,path = tempfile.mkstemp()
        pynimbusauthz.print_msg(opts, 2, "Using temp file: " + path) 

        try:
            nimbus_list_users.main(['-b', '-D', opts.delim, '-r', _fields_csv,
                '-O', path, '%'])
            current_users = read_users(path, delimiter=opts.delim)
        finally:
            os.remove(path)

        if not walk_users(current_users, file_users, opts):
            return 1

    except CLIError, clie:
        print clie
        return clie.get_rc()

    return 0

if __name__ == '__main__':
    sys.exit(main())

