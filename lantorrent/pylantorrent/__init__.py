import sys
import os
import logging
import traceback
from ConfigParser import SafeConfigParser
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

def create_endpoint_entry(host, dest_file, data_size, port=2893, block_size=128*1024, degree=1, rid=None):
    if rid == None:
        rid = str(uuid.uuid1())

    final = {}
    final['file'] = dest_file
    final['host'] = host
    final['port'] = port
    final['block_size'] = block_size
    final['degree'] = degree
    final['id'] = rid
    final['length'] = data_size

    return final


class VConfig(object):

    def __init__(self):
        self.set_defaults()
        if 'VIRGA_HOME' not in os.environ:
            emsg = "the env VIRGA_HOME must be set"
            self.lt_home = os.path.expanduser("lantorrent")
            log(logging.WARNING, emsg)
        else:
            self.lt_home = os.environ['LANTORRENT_HOME'] 
            ini_file = os.path.join(self.lt_home, "etc/lt.ini")

            try:
                self.load_settings(ini_file)
            except:
                emsg = "failed to load %s, using defaults" % (ini_file)
                log(logging.WARNING, emsg)
        logging.basicConfig(filename=self.logfile, level=self.log_level)

    def set_defaults(self):
        self.pw = "nimbus"
        self.logfile = "lantorrent.log"
        self.log_level = logging.DEBUG
        self.dbfile = None

    def load_settings(self, ini_file):
        log_levels = {'debug': logging.DEBUG,
            'info': logging.INFO,
            'warning': logging.WARNING,
            'error': logging.ERROR,
            'critical': logging.CRITICAL}


        s = SafeConfigParser()
        s.readfp(open(ini_file, "r"))
        self.pw = s.get("security", "password")
        self.logfile = s.get("log", "file").replace("@VIRGA@", self.lt_home)
        self.host = s.get("host", "host")
        self.port = s.getint("port", "port")
        try:
            log_level_str = s.get("log", "level")
            self.log_level = log_levels[log_level_str]
        except Exception, ex:
            pass
        try:
            self.dbfile = s.get("db", "file").replace("@VIRGA@", self.lt_home)
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


