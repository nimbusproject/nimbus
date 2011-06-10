import os
import sys

from pynimbusconfig import autoca
from pynimbusconfig import pathutil
from pynimbusconfig.setuperrors import *

def run(basedir, certconf, keyconf, log, cadir=None, hostname=None):
    log.debug("Checking SSL")
    
    # If the configurations themselves are missing, we cannot continue.
    if not certconf:
        raise IncompatibleEnvironment("There is no 'ssl.cert' configuration")
    if not keyconf:
        raise IncompatibleEnvironment("There is no 'ssl.key' configuration")
        
    # If the configurations are relative, they are assumed to be relative from
    # the base directory.
    if not pathutil.is_absolute_path(certconf):
        certconf = pathutil.pathjoin(basedir, certconf)
        log.debug("ssl.cert was a relative path, converted to '%s'" % certconf)
    if not pathutil.is_absolute_path(keyconf):
        keyconf = pathutil.pathjoin(basedir, keyconf)
        log.debug("ssl.key was a relative path, converted to '%s'" % keyconf)
        
    # If the configured certificate exists, check the key permissions, then
    # exit.
    missingcert = None
    missingkey = None
    if not pathutil.check_path_exists(certconf):
        missingcert = "Configured 'ssl.cert' does not exist at '%s'" % certconf
    if not pathutil.check_path_exists(keyconf):
        missingkey = "Configured 'ssl.key' does not exist at '%s'" % keyconf
        
    if not missingcert and not missingkey:
        log.debug("cert and key confs exist already, checking key perms")
        # check key permission
        if pathutil.is_path_private(keyconf):
            log.debug("key is owner-read only: %s" % keyconf)
        else:
            print >>sys.stderr, "***"
            print >>sys.stderr, "*** WARNING ***"
            print >>sys.stderr, "***"
            print >>sys.stderr, "SSL key has bad permissions, should only be readable by the file owner.  ssl.key: '%s'" % keyconf
        return
        
    # If only one of the cert/key files exists, we cannot reason about
    # what to do: error.
    prefix = "Only one of the SSL cert/key file exists, cannot continue. "
    if missingcert and not missingkey:
        raise IncompatibleEnvironment(prefix + missingcert)
    if missingkey and not missingcert:
        raise IncompatibleEnvironment(prefix + missingkey)
        
    
    # The configured certificate and key do not exist; create them.
    
    print "Cannot find configured certificate and key for HTTPS, creating these for you."
    
    # If the internal CA does not exist, create that first.
    if not cadir:
        cadir = pathutil.pathjoin(basedir, "var/ca")
    if not pathutil.check_path_exists(cadir):
        print "\nCannot find internal CA, creating this for you.\n"
        print "Please pick a unique, one word CA name or hit return to use a UUID.\n"
        print "For example, if you are installing this on the \"Jupiter\" cluster, you could perhaps use \"JupiterNimbusCA\" as the name.\n"
        
        ca_name = raw_input("Enter a name: ")
        
        if not ca_name:
            ca_name = pathutil.uuidgen()
            print "You did not enter a name, using '%s'" % ca_name
        else:
            ca_name = ca_name.split()[0]
            print "Using '%s'" % ca_name
        
        autoca.createCA(ca_name, basedir, cadir, log)
        print "\nCreated internal CA: %s" % cadir
    
    if not hostname:
        print "\nEnter the fully qualified hostname of this machine.  If you don't know or care right now, hit return to use 'localhost'.\n"
        
        hostname = raw_input("Hostname: ")
        if not hostname:
            hostname = "localhost"
        print "Using '%s'" % hostname
    
    autoca.createCert(hostname, basedir, cadir, certconf, keyconf, log)
    print "\nCreated certificate: %s" % certconf
    print "Created key: %s\n" % keyconf
    
    
