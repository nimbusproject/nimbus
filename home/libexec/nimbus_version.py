#!/usr/bin/env python

import ConfigParser
import optparse
import os
import sys

# see docurl()
DOCURL_BASE="http://www.nimbusproject.org/docs/"
GUIDE_TAIL="admin/z2c/"

CONFIGSECTION = 'nimbusversion'
EXAMPLECONFIG = """
[nimbusversion]

# required
major: 2
minor: 5
patch: 0
docversion: 2.5
commit: 76202cc04b50cac0086115c95df26fb49016cddd

# optional
rc: RC2
buildtime: UTC_2010-07-13_19.14.29
"""

class ARGS:
    """Class for command-line argument constants"""
    
    MAJORMINOR_LONG = "--majorminor"
    MAJORMINOR_HELP = "Just the major.minor fields"
    
    FULLVERSION_LONG = "--fullversion"
    FULLVERSION_HELP = "Prints x.y.z (if z is 0, just x.y)"
    
    MAJOR_LONG = "--major"
    MAJOR_HELP = "Just the major field"
    
    MINOR_LONG = "--minor"
    MINOR_HELP = "Just the minor field"
    
    PATCH_LONG = "--patch"
    PATCH_HELP = "Just the patch field"
    
    RC_LONG = "--rc"
    RC_HELP = "Just the RC version, exits non-zero if there is none"
    
    DOCVERSION_LONG = "--docversion"
    DOCVERSION_HELP = "Just the docversion"

    ADMINGUIDE_LONG = "--guide"
    ADMINGUIDE_HELP = "Just the admin guide URL"
    
    COMMIT_LONG = "--commit"
    COMMIT_HELP = "Just the commit this was based on"
    
    TIME_LONG = "--buildtime"
    TIME_HELP = "Just build time, exits non-zero if there is none"

def parsersetup():
    """Return configured command-line parser."""

    ver = "Nimbus Version: Infinity and Beyond"
    usage = "Select action or provide no argument for friendly message.  See help (-h)."
    parser = optparse.OptionParser(version=ver, usage=usage)

    parser.add_option(ARGS.MAJORMINOR_LONG, action="store_true", default=False,
                      dest="majorminor", help=ARGS.MAJORMINOR_HELP)
    
    parser.add_option(ARGS.FULLVERSION_LONG, action="store_true", default=False,
                      dest="fullversion", help=ARGS.FULLVERSION_HELP)
    
    parser.add_option(ARGS.MAJOR_LONG, action="store_true", default=False,
                      dest="major", help=ARGS.MAJOR_HELP)
    
    parser.add_option(ARGS.MINOR_LONG, action="store_true", default=False,
                      dest="minor", help=ARGS.MINOR_HELP)
    
    parser.add_option(ARGS.PATCH_LONG, action="store_true", default=False,
                      dest="patch", help=ARGS.PATCH_HELP)

    parser.add_option(ARGS.RC_LONG, action="store_true", default=False,
                      dest="rc", help=ARGS.RC_HELP)
    
    parser.add_option(ARGS.DOCVERSION_LONG, action="store_true", default=False,
                      dest="docversion", help=ARGS.DOCVERSION_HELP)
    
    parser.add_option(ARGS.TIME_LONG, action="store_true", default=False,
                      dest="buildtime", help=ARGS.TIME_HELP)
    
    parser.add_option(ARGS.COMMIT_LONG, action="store_true", default=False,
                      dest="commit", help=ARGS.COMMIT_HELP)
    
    parser.add_option(ARGS.ADMINGUIDE_LONG, action="store_true", default=False,
                      dest="guide", help=ARGS.ADMINGUIDE_HELP)

    return parser

def validateargs(opts):
    
    seeh = "see help (-h)"
    actions = 0
    for action in [ opts.majorminor, opts.fullversion, opts.major, opts.minor, opts.patch, opts.rc, opts.docversion, opts.commit, opts.guide ]:
        if action:
            actions += 1
    
    if actions > 1:
        raise Exception("Select only one action (or use no args for friendly message)")

def _get(config, key, required=True):
    try:
        item = config.get(CONFIGSECTION, key)
        return item.strip()
    except:
        if required:
            raise Exception("Missing required configuration '%s'" % key)
        return None

def friendlymsg(config):
    print "Nimbus Version: %s" % fullversion(config)
    print "Documentation: %s" % adminguide(config)
    print "Git: %s" % _get(config, "commit")
    rc = _get(config, "rc", required=False)
    if rc:
        print "\nWarning: this is a release candidate, not a stable release."

def fullversion(config):
    major = _get(config, "major")
    minor = _get(config, "minor")
    patch = _get(config, "patch", required=False)
    v = ""
    if int(patch) == 0:
        v = "%s.%s" % (major, minor)
    else:
        v = "%s.%s.%s" % (major, minor, patch)
    rc = _get(config, "rc", required=False)
    if rc:
        return v + rc
    else:
        return v

def docurl(config, tail):
    """Generic method to combine docversion and doc tail with the base URL
    
    config -- Valid ".nimbusversion" config
    
    tail -- The part of the documentation URL after the docversion that you
    require.  Must not start with "/".  Example: "admin/reference.html"

    """
    return "%s%s/%s" % (DOCURL_BASE, _get(config, "docversion"), tail)

def adminguide(config):
    return docurl(config, GUIDE_TAIL)

def parsetarname(name):
    """Return (major, minor, patch, rc) based on tarball name"""
    n = name.strip()
    if not n:
        raise Exception("invalid name: '%s'" % name)
    prefix = "nimbus-"
    suffix = "-src.tar.gz"
    if not n.startswith(prefix):
        raise Exception("does not start with %s" % prefix)
    if not n.endswith(suffix):
        raise Exception("does not end with %s" % suffix)
    version = n[len(prefix):]
    version = version[:-len(suffix)]
    return _parsefullversion(version)
    
def newconfig(major, minor, patch, rc):
    """NOT COMPLETE: the installer will have to insert such a line:
    commit: 76202cc04b50cac0086115c95df26fb49016cddd"""
    
    conf = """[nimbusversion]
major: %d
minor: %d
patch: %d
docversion: %d.%d
""" % (major, minor, patch, major, minor)
    
    if rc:
        conf = conf + "rc: %s\n" % rc
    return conf

def _legalversion(major, minor, patch, rc):
    if not isinstance(major, int):
        raise Exception("suspicious major version: %s" % str(major))
    if major < 2:
        raise Exception("suspicious major version: %d" % major)

    if not isinstance(minor, int):
        raise Exception("suspicious minor version: %s" % str(minor))
    if minor < 0:
        raise Exception("suspicious minor version: %d" % minor)
        
    if not isinstance(patch, int):
        raise Exception("suspicious patch version: %s" % str(patch))
    if patch < 0:
        raise Exception("suspicious patch version: %d" % patch)
    
    if rc:
        if not isinstance(rc, str):
            raise Exception("suspicious rc part: %s" % str(rc))
        if not rc.startswith("RC"):
            raise Exception("suspicious rc part: %s" % rc)

def _parsefullversion(version):
    
    version = version.strip()
    
    major = -1
    minor = -1
    patch = 0
    rc = None
    
    rcidx = version.rfind("RC")
    if rcidx >= 0:
        if rcidx == 0:
            raise Exception("version cannot start with RC: '%s'" % version)
        rc = version[rcidx:]
        if len(rc) == 2:
            raise Exception("has RC but no RC version: '%s'" % version)
        
        version = version[:rcidx]
    
    # version should now only contain x.y or x.y.z
    
    parts = version.split(".")
    if len(parts) == 3:
        major = int(parts[0])
        minor = int(parts[1])
        patch = int(parts[2])
        if patch <= 0:
            raise Exception("patch level cannot be explicitly 0: '%s'" % version)
    elif len(parts) == 2:
        major = int(parts[0])
        minor = int(parts[1])
    else:
        raise Exception("version is not legal: '%s'" % version)
    
    _legalversion(major, minor, patch, rc)
    return (major, minor, patch, rc)

def main(argv=None):
        
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
        nh = os.getenv("NIMBUS_HOME")
        if not nh:
            raise Exception("Requires that NIMBUS_HOME is set.")
        if not os.path.exists(nh):
            raise Exception("NIMBUS_HOME does not exist: %s" % nh)
        conf = os.path.join(nh, "libexec/.nimbusversion")
        if not os.path.exists(conf):
            raise Exception("Version information does not exist: %s\nYou probably installed from the code repository." % conf)
    except:
        print >>sys.stderr, sys.exc_value
        return 2
    
    try:
        config = ConfigParser.SafeConfigParser()
        config.read(conf)
        
        if opts.majorminor:
            major = _get(config, "major")
            minor = _get(config, "minor")
            print "%s.%s" % (major, minor)
        elif opts.fullversion:
            print fullversion(config)
        elif opts.major:
            print _get(config, "major")
        elif opts.minor:
            print _get(config, "minor")
        elif opts.patch:
            print _get(config, "patch")
        elif opts.rc:
            rc = _get(config, "rc", required=False)
            if rc:
                print rc
            else:
                return 3
        elif opts.buildtime:
            time = _get(config, "buildtime", required=False)
            if time:
                print time
            else:
                return 3
        elif opts.docversion:
            print _get(config, "docversion")
        elif opts.commit:
            print _get(config, "commit")
        elif opts.guide:
            print adminguide(config)
        else:
            friendlymsg(config)
        
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
    
    # hidden argument to support the make-dist process, not for users
    if len(sys.argv) == 3:
        if sys.argv[1] == "--tar":
            (major, minor, patch, rc) = parsetarname(sys.argv[2])
            newconf = newconfig(major, minor, patch, rc)
            print newconf
            sys.exit(0)
    
    sys.exit(main())

def test_parsetarname():
    tars = { "nimbus-2.5RC1.9-src.tar.gz": (2, 5, 0, "RC1.9"),
             "nimbus-2.4-src.tar.gz": (2, 4, 0, None),
             "nimbus-2.4.2-src.tar.gz": (2, 4, 2, None),
             "nimbus-2.5.2RC2-src.tar.gz": (2, 5, 2, "RC2"),
             "nimbus-3.0-src.tar.gz": (3, 0, 0, None),
             "nimbus-3.0RCsomething-src.tar.gz": (3, 0, 0, "RCsomething"),
    }
    
    for (tarname,tup) in tars.items():
        (major, minor, patch, rc) = parsetarname(tarname)
        assert major == tup[0]
        assert minor == tup[1]
        assert patch == tup[2]
        assert rc == tup[3]

def test_badparsetarname():
    
    badnames = [ "nimbus-2.5R2-src.tar.gz",
                 "nimbus-1.5-src.tar.gz",
                 "nimbus-2.5.tar.gz",
                 "nimbus-RC2-src.tar.gz",
                 "nimbus-2.5.tar",
                 "2.5RC2",
                 "nimbus-2.5.0-src.tar.gz",
    ]
    
    for tarname in badnames:
        invalid_input = False
        try:
            parsetarname(tarname)
        except:
            invalid_input = True
        else:
            print tarname
        assert invalid_input
