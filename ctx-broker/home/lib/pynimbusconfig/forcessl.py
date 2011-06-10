import os
import sys

from pynimbusconfig import autoca
from pynimbusconfig import pathutil
from pynimbusconfig.setuperrors import *

def run(basedir, cadir, certconf, keyconf, hostnameconf, log):
    log.debug("Forcing a CA/hostcert install")
    
    # Reject relative paths
    if not pathutil.is_absolute_path(cadir):
        raise IncompatibleEnvironment("CA directory path is not absolute")
        
    if not pathutil.is_absolute_path(certconf):
        raise IncompatibleEnvironment("certificate path is not absolute")
        
    if not pathutil.is_absolute_path(keyconf):
        raise IncompatibleEnvironment("key path is not absolute")
        
    # The CA dir must not exist, create that first.
    autoca.createCA(pathutil.uuidgen(), basedir, cadir, log)
    print "Created auto CA: %s" % cadir
        
    # The configured certificate and key must not exist; create them.
    autoca.createCert(hostnameconf, basedir, cadir, certconf, keyconf, log)
    print "\nCreated hostcert: %s" % certconf
    print "Created hostkey: %s\n" % keyconf
    