from commands import getstatusoutput
import os
import string
from propagate_adapter import PropagationAdapter
from workspacecontrol.api.exceptions import *
import propagate_scp
import urlparse
import workspacecontrol.main.wc_args as wc_args
import socket

class LantorrentPropadapter(propagate_scp.propadapter):
        
    def __init__(self, params, common):
        propagate_scp.propadapter.__init__(self, params, common)
        self.ssh = None
        self.ltport = None
        self.ltip = None
        self.scheme = "lantorrent://"

    def validate(self):
        # validate scp adaptor 
        propagate_scp.propadapter.validate(self)
        self.c.log.debug("validating lantorrent propagation adapter")
    
        self.ltip = self.p.get_conf_or_none("propagation", "lantorrentip")
        if not self.ltip:
            self.ltip = ""

        self.ltport = self.p.get_conf_or_none("propagation", "lantorrentport")
        if not self.ltport:
            self.ltport = 5893

        self.ssh = self.p.get_conf_or_none("propagation", "ssh")
        if not self.ssh:
            raise InvalidConfig("no path to ssh")
            
        if os.path.isabs(self.scp):
            if not os.access(self.scp, os.F_OK):
                raise InvalidConfig("SSH is configured with an absolute path, but it does not seem to exist: '%s'" % self.ssh)
                
            if not os.access(self.scp, os.X_OK):
                raise InvalidConfig("SSH is configured with an absolute path, but it does not seem executable: '%s'" % self.ssh)

        self.c.log.debug("SSH configured: %s" % self.ssh)
        
        self.sshuser = self.p.get_conf_or_none("propagation", "ssh_user")
        if self.sshuser:
            self.c.log.debug("SSH default user: %s" % self.sshuser)
        else:
            self.c.log.debug("no SSH default user")

    def translate_to_scp(self, imagestr):
        if imagestr[:len(self.scheme)] != self.scheme:
            raise InvalidInput("scp trans invalid lantorrent url, not %s %s" % (self.scheme, imagestr))
        url = "scp://" + imagestr[len(self.scheme):]
        url_a = url.split("?")
        return url_a[0]


    def validate_unpropagate_target(self, imagestr):
        imagestr = self.translate_to_scp(imagestr)
        propagate_scp.propadapter.validate_unpropagate_target(self, imagestr)

    def unpropagate(self, local_absolute_source, remote_target):
        remote_target = self.translate_to_scp(remote_target)
        propagate_scp.propadapter.unpropagate(self, local_absolute_source, remote_target)

    def validate_propagate_source(self, imagestr):
        # will throw errors if invalid
        self._lt_command("fake", imagestr)
    
    def propagate(self, remote_source, local_absolute_target):
        self.c.log.info("lantorrent propagation - remote source: %s" % remote_source)
        self.c.log.info("lantorrent propagation - local target: %s" % local_absolute_target)
        
        cmd = self._lt_command(local_absolute_target, remote_source)
        self.c.log.info("Running lantorrent command: %s" % cmd)
        
        ret,output = getstatusoutput(cmd)
        if ret:
            errmsg = "problem running command: '%s' ::: return code" % cmd
            errmsg += ": %d ::: output:\n%s" % (ret, output)
            self.c.log.error(errmsg)
            raise UnexpectedError(errmsg)
        self.c.log.info("Transfer complete.")
    
    def _lt_command(self, local, remote):
        """
        Remote url: lantorrent://hostname:port/path.
        """

        if remote[:len(self.scheme)] != self.scheme:
            raise InvalidInput("get command invalid lantorrent url, not %s %s" % (self.scheme, remote))

        ra = remote.split("?", 1)
        if len(ra) != 2:
            raise InvalidInput("invalid lantorrent url, %s.  It must contain parameters the remoteexe" % (remote))

        url = ra[0]
        lt_exe = ra[1]

        up = urlparse.urlparse(url)
        xfer_host = up.hostname
        xfer_user = up.username
        xfer_port = int(up.port)
        if xfer_port == None:
            xfer_port = 22
        xfer_path = up.path

        if xfer_user:
            self.c.log.info("allowing client to specify this account: %s" % xfer_user) 
        else:
            self.c.log.debug("client did not specify account") 

            # if default is not specified, we just uses current account
            if self.sshuser:
                self.c.log.debug("using the default ssh account") 
                xfer_user = self.sshuser
            else:
                self.c.log.debug("using the program runner for ssh account") 

        if xfer_user:
            xfer_user = xfer_user + "@"
        else:
            xfer_user = ""
        rid = str(uuid.uuid1())
        cmd = self.ssh + " -p %d %s%s %s %s %s %s:%d" % (xfer_port, xfer_user, xfer_host, lt_exe, xfer_path, local, rid, self.ltip, self.ltport)

        self.c.log.debug("lantorrent command %s " % (cmd))

        return cmd
