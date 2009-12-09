import os
import string
from propagate_adapter import PropagationAdapter
from workspacecontrol.api.exceptions import *

class propadapter(PropagationAdapter):
        
    def __init__(self, params, common):
        PropagationAdapter.__init__(self, params, common)
        self.scp = None

    def validate(self):
        self.c.log.debug("validating scp propagation adapter")
    
        self.scp = self.p.get_conf_or_none("propagation", "scp")
        if not self.scp:
            raise InvalidConfig("no path to scp")
            
        if os.path.isabs(self.scp):
            if not os.access(self.scp, os.F_OK):
                raise InvalidConfig("SCP is configured with an absolute path, but it does not seem to exist: '%s'" % self.scp)
                
            if not os.access(self.scp, os.X_OK):
                raise InvalidConfig("SCP is configured with an absolute path, but it does not seem executable: '%s'" % self.scp)

        self.c.log.info("SCP configured: %s" % self.scp)
        

        self.scpuser = self.p.get_conf_or_none("propagation", "scp_user")
        if self.scpuser:
            self.c.log.info("SCP default user: %s" % self.scpuser)
        else:
            self.c.log.info("no SCP default user")
            
        override_conf = self.p.get_conf_or_none("propagation", "scp_user_override")
        
        if not override_conf:
            self.scpuser_override = False
        else:
            if string.lower(override_conf) == "false":
                self.scpuser_override = False
            else:
                self.scpuser_override = True

    def validate_propagate_source(self, imagestr):
        # will throw errors if invalid
        self._get_pull_command(imagestr, "fake")
    
    def validate_unpropagate_target(self, imagestr):
        # will throw errors if invalid
        self._get_push_command("fake", imagestr)
    
    def propagate(self, remote_source, local_absolute_target):
        self.c.log.info("SCP propagation - remote source: %s" % remote_source)
        self.c.log.info("SCP propagation - local target: %s" % local_absolute_target)
        
        cmd = self._get_pull_command(remote_source, local_absolute_target)
        self.c.log.info("Running SCP command: %s" % cmd)
    
    def unpropagate(self, local_absolute_source, remote_target):
        self.c.log.info("SCP unpropagation - local source: %s" % local_absolute_source)
        self.c.log.info("SCP unpropagation - remote target: %s" % remote_target)
        
        cmd = self._get_push_command(local_absolute_source, remote_target)
        self.c.log.info("Running SCP command: %s" % cmd)
        
        
    # --------------------------------------------------------------------------
    
    def _get_push_command(self, local, remote):
        """Return command to send a local file to somewhere remote
        local -- absolute path on local filesystem
        remote -- URL like "scp://host:port/path" (might contain user as well)
        """
        return self._get_remote_command(local, remote, True)
        
    def _get_pull_command(self, remote, local):
        """Return command to retrieve a remote file.
        remote -- URL like "scp://host:port/path" (might contain user as well)
        local -- absolute path on local filesystem
        """
        return self._get_remote_command(local, remote, False)
        
    def _get_remote_command(self, local, remote, push):
                
        # 'remote' arg is a URL like "scp://host:port/path"
        # scp instead needs: host -P port remote local
        #       (or if push: host -P port local remote)
        
        self.c.log.debug("(scp) examining remote '%s'" % remote)
        
        if remote[:6] != "scp://":
            raise InvalidInput("invalid scp url, not scp:// " + remote)

        xfer_host = None
        xfer_user = None
        xfer_port = 22
        xfer_path = None

        given = remote[6:]
        colon_index = string.find(given, ':')
        if colon_index == -1:
            # no port
            path_index = string.find(given, '/')
            if path_index == -1:
                raise InvalidInput("invalid scp url, no host? " + remote)
            host = given[:path_index]
            xfer_path = given[path_index:]
        else:
            # found a port
            host = given[:colon_index]
            given = given[colon_index+1:]
            path_index = string.find(given, '/')
            if path_index == -1:
                raise InvalidInput("invalid scp url, no path? " + remote)
                
            port = given[:path_index]
            
            try:
                xfer_port = int(port)
            except:
                raise InvalidInput("port, but not an integer? " + remote)
                
            xfer_path = given[path_index:]

        # host var could contain user specification
        at_index = string.find(host, '@')
        if at_index == -1:
            xfer_host = host
        else:
            xfer_user = host[:at_index]
            xfer_host = host[at_index+1:]

        if xfer_user:
            self.c.log.debug("client wants override of scp account") 

            if not self.scpuser_override:
                errmsg = "client specified in SCP URL %s, " % remote
                errmsg += "but scpuser_override is configured to "
                errmsg += "false, meaning the default account "
                errmsg += "is not allowed to be overriden"
                raise InvalidInput(errmsg)
            else:
                self.c.log.info("scpuser_override is true, so allowing client to specify this account: %s" % xfer_user) 
        else:
            self.c.log.debug("client did not specify account") 

            # if default is not specified, we just uses current account
            if self.scpuser:
                self.c.log.debug("using the default scp account") 
                xfer_user = self.scpuser
            else:
                self.c.log.debug("using the program runner for scp account") 

        self.c.log.debug("SCP user %s, host %s, port %d, path %s" 
                  % (xfer_user, xfer_host, xfer_port, xfer_path))

        cmd = self.scp + " -P %d " % xfer_port

        if push:
            cmd += local + ' '

        if xfer_user:
            cmd += xfer_user + "@"

        # never make path relative to remote homedir
        if xfer_path[0] != '/':
            tail = ":/" + xfer_path
        else:
            tail = ":" + xfer_path
        cmd += xfer_host + tail

        if not push:
            cmd +=  ' ' + local

        return cmd
        
        