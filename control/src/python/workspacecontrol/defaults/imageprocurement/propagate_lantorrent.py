from commands import getstatusoutput
import os
from workspacecontrol.api.exceptions import *
import propagate_scp
import workspacecontrol.main.wc_args as wc_args
from propagate_common import url_parse

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
        self.ltport = int(self.ltport)
        if not self.ltport:
            self.ltport = 2893

        self.ssh = self.p.get_conf_or_none("propagation", "lantorrentexe")
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
        else:
            self.c.log.info("Successfully ran %s.  output %s" % (cmd, output))

        self.c.log.info("Transfer complete.")
    
    def _lt_command(self, local, remote):
        """
        Remote url: lantorrent://hostname:port/path.
        """

        if remote[:len(self.scheme)] != self.scheme:
            raise InvalidInput("get command invalid lantorrent url, not %s %s" % (self.scheme, remote))

        url = remote
        lt_exe = self.p.get_arg_or_none(wc_args.EXTRA_ARGS)
        if lt_exe == None:
            raise InvalidInput("the prop-extra-args parameter must be used and be a path to the remote execution script")

        (xfer_scheme, xfer_user, xfer_pw, xfer_host, xfer_port, xfer_path) =url_parse(url)
 
        if xfer_port == None:
            xfer_port = 22

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
        try:
            import uuid
            rid = str(uuid.uuid1())
        except:
            import commands
            rid = commands.getoutput("uuidgen")
        cmd = self.ssh + " %d %s%s %s %s %s %s %s:%d" % (xfer_port, xfer_user, xfer_host, lt_exe, xfer_path, local, rid, self.ltip, self.ltport)

        self.c.log.debug("lantorrent command %s " % (cmd))

        return cmd

def url_parse(url):
    parts = url.split('://', 1)
    scheme = parts[0]
    rest = parts[1]

    parts = rest.split('@', 1)
    if len(parts) == 1:
        user = None
        password = None
    else:
        rest = parts[1]
        u_parts = parts[0].split(':')
        user = parts[0]
        if len(u_parts) == 1:
            password = None
        else:
            password = parts[1]

    parts = rest.split('/', 1)
    contact_string = parts[0]
    if len(parts) > 1:
        path = '/' + parts[1]
    else:
        path = None

    parts = contact_string.split(':')
    hostname = parts[0]
    if len(parts) == 1:
        port = None
    else:
        port = int(parts[1])

    return (scheme, user, password, hostname, port, path)
