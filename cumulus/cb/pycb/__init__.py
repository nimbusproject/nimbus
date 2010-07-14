import traceback
import sys
import os
import socket
import logging
from pynimbusauthz.cmd_opts import cbOpts
from cbPosixSecurity import cbPosixSec
from cbPosixBackend import cbPosixBackend
from ConfigParser import SafeConfigParser
from pycb.cbAuthzSecurity import cbAuthzUser
from pycb.cbAuthzSecurity import cbAuthzSec

from optparse import OptionParser
import hmac
try:
    from hashlib import sha1 as sha
    from hashlib import sha256 as sha256
except ImportError:
    import sha
import boto
import boto.utils
import base64

Version = "0.1"
logger = None
authenticated_user_id = "CumulusAuthenticatedUser"
public_user_id = "CumulusPublicUser"

def log(level, msg, tb=None):
    global logger

    if logger == None:
        print msg
        return

    logger.log(level, msg)

    if tb != None:
        logger.log(level, "Stack trace")
        logger.log(level, "===========")
        stack = tb.format_exc()
        logger.log(level, stack)
        logger.log(level, "===========")
        logger.log(level, sys.exc_info()[0])

class CBConfig(object):

    def __init__(self):
        self.default_settings()
        self.load_settings()

        config_error = False
        if self.auth == None:
            config_error = True
            print "No authentication module set: |%s|" % (self.auth_error)

        if self.bucket == None:
            config_error = True
            print "No backend set"

        if self.installdir == None:
            config_error = True
            print "No install dir set"

        if config_error:
            print """cumulus.ini file must have the following set:
installdir=<path to cumulus installation>

The search path for cumulus.ini is:

    /etc/nimbus/cumulus.ini
    $CUMULUS_HOME/etc/cumulus.ini
    ~/.nimbus/cumulus.ini
    cumulus.ini
    env 'CUMULUS_SETTINGS_FILE
"""

        self.setup_logger()

    def setup_logger(self):
        global logger
        if self.log_filename == None:
            self.log_filename = self.installdir + "/log/cumulus.log"

        logger = logging.getLogger('cumulus')
        handler = logging.handlers.RotatingFileHandler(
          self.log_filename, maxBytes=100*1024*1024, backupCount=2)
        logger.addHandler(handler)
        logger.setLevel(self.log_level)
        formatter = logging.Formatter("%(asctime)s - %(process)d - %(levelname)s - %(message)s")
        handler.setFormatter(formatter)
        logger.addHandler(handler)

    def default_settings(self):
        self.auth_error = ""
        self.hostname = socket.gethostname()
        self.installdir = None
        self.port = 8888
        self.auth = None
        self.bucket = None
        self.log_level = logging.INFO
        self.log_filename = None
        self.location = "CumulusLand"
        self.https_key = None
        self.https_cert = None
        self.use_https = False
        self.block_size = 1024*512

    def get_contact(self):
        return (self.hostname, self.port)

    def load_settings(self):
        config_path = []
        config_path.append("/etc/nimbus/cumulus.ini")
        if 'CUMULUS_HOME' in os.environ:
            config_path.append(os.path.join(os.environ['CUMULUS_HOME'], "etc/cumulus.ini"))
        config_path.append(os.path.expanduser('~/.nimbus/cumulus.ini'))
        config_path.append(os.path.expanduser('cumulus.ini'))
        config_path.append(os.environ.get('CUMULUS_SETTINGS_FILE'))
        log_levels = {'debug': logging.DEBUG,
            'info': logging.INFO,
            'warning': logging.WARNING,
            'error': logging.ERROR,
            'critical': logging.CRITICAL}

        for cp in config_path:
            try:
                s = SafeConfigParser()
                s.readfp(open(cp, "r"))
            except:
                continue

            try:
                self.installdir = s.get("cb", "installdir")
            except:
                pass
            try:
                self.port = int(s.get("cb", "port"))
            except:
                pass
            try:
                self.hostname = s.get("cb", "hostname")
            except:
                pass
            try:
                self.location = s.get("cb", "location")
            except:
                pass

            try:
                backend = s.get("backend", "type")
                if backend == "posix":
                    posix_dir = s.get("backend", "data_dir")
                    self.bucket = cbPosixBackend(posix_dir)
                    block_size = s.get("backend", "block_size")
            except:
                pass

            try:
                sec = s.get("security", "type")
                self.sec_type = sec
                if sec == "posix":
                    posix_dir = s.get("security", "security_dir")
                    self.auth = cbPosixSec(posix_dir)
                elif sec == "authz":
                    self.authzdb = s.get("security", "authzdb")
                    self.auth = cbAuthzSec(self.authzdb)
                else:
                    self.auth_error = self.auth_error + "no type %s" % (sec)
            except:
                traceback.print_exc(file=sys.stderr)
                x = str(sys.exc_info()[0])
                self.auth_error = x + self.auth_error

            try:
                log_level_str = s.get("log", "level")
                self.log_level = log_levels[log_level_str]
            except Exception, ex:
                pass
            try:
                self.log_filename = s.get("log", "file")
            except:
                pass

            try:
                self.https_key = s.get("https", "key").strip()
                self.https_cert = s.get("https", "cert").strip()
                self.use_https = s.getboolean("https", "enabled")
            except:
                pass


    def parse_cmdline(self, argv):
        global Version

        self.opts = []
        opt = cbOpts("port", "p", "use the give port number", None, range=(0, 65536))
        self.opts.append(opt)
        opt = cbOpts("https", "s", "Enable https", False, flag=True)
        self.opts.append(opt)

        u = """ [options]"""
    
        version = "%prog " + (Version)
        parser = OptionParser(usage=u, version=version)
        for o in self.opts:
            o.add_opt(parser)

        (options, args) = parser.parse_args(args=argv)

        for o in self.opts:
            o.validate(options)

        # override anything from the files        
        if options.port != None:
            self.port = int(options.port)


def get_auth_hash(key, method, path, headers, uri):

    sub_resources = ["?acl", "?location", "?logging", "?torrent"]
    for sr in sub_resources:
        ndx = uri.find(sr)
        if ndx > 0:
            path = path + sr

    myhmac = hmac.new(key, digestmod=sha)
    c_string = boto.utils.canonical_string(method, path, headers)
    myhmac.update(c_string)
    auth_hash = base64.encodestring(myhmac.digest()).strip()
    return auth_hash

config = CBConfig()
