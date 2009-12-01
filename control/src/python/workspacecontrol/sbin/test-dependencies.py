#!/usr/bin/env python

import sys

ERR = "** PROBLEM: "

problem_count = 0

version = sys.version
print "Python %s" % version.replace("\n", " | ")

curr = sys.version_info
required = (2,5)

if curr[0] < required[0]:
    print >>sys.stderr, "\n%sThe Python version looks too low, 2.5 is required." % ERR
    problem_count += 1
elif curr[1] < required[1]:
    print >>sys.stderr, "\n%sThe Python version looks too low, 2.5 is required." % ERR
    problem_count += 1

try:
    import zope.interface
except ImportError:
    print >>sys.stderr, "\n%sCannot locate the zope.interface package." % ERR
    print >>sys.stderr, "\nThis should be included in the workspace-control lib/ directory.  That directory should be loaded as part of the program's PYTHONPATH."
    problem_count += 1
    
try:
    import libvirt2
except:
    print >>sys.stderr, "\n%sCannot locate the libvirt Python bindings package." % ERR
    print >>sys.stderr, "\nOn some Linux distributions, this is only included when you install libvirt\n when you have previously installed the 'python-dev' package."
    print >>sys.stderr, "\nSee your distribution documentation."
    problem_count += 1
    
    
try:
    from workspacecontrol.api.exceptions import *
except:
    print >>sys.stderr, "\n%sCannot locate the 'workspacecontrol' Python package." % ERR
    print >>sys.stderr, "\nIf you installed this from the directions, please contact the mailing list.."
    problem_count += 1

if problem_count:
    sys.exit(1)
    
print "\nOK, looks like the Python dependencies are set up."
