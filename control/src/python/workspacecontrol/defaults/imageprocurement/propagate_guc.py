import os
import string
from propagate_adapter import PropagationAdapter
from workspacecontrol.api.exceptions import *

class propadapter(PropagationAdapter):
        
    def __init__(self, params, common):
        PropagationAdapter.__init__(self, params, common)
        self.guc = None

    def validate(self):
        self.c.log.debug("validating GridFTP based propagation adapter")
    
        self.guc = self.p.get_conf_or_none("propagation", "guc")
        if not self.guc:
            raise InvalidConfig("no path to globus-url-copy")
            
        if os.path.isabs(self.guc):
            if not os.access(self.guc, os.F_OK):
                raise InvalidConfig("globus-url-copy is configured with an absolute path, but it does not seem to exist: '%s'" % self.guc)
                
            if not os.access(self.guc, os.X_OK):
                raise InvalidConfig("globus-url-copy is configured with an absolute path, but it does not seem executable: '%s'" % self.guc)

        self.c.log.info("globus-url-copy configured: %s" % self.guc)

    def validate_propagate_source(self, imagestr):
        # will throw errors if invalid
        self._get_pull_command(imagestr, "fake")
    
    def validate_unpropagate_target(self, imagestr):
        # will throw errors if invalid
        self._get_push_command("fake", imagestr)
    
    def propagate(self, remote_source, local_absolute_target):
        self.c.log.info("GridFTP propagation - remote source: %s" % remote_source)
        self.c.log.info("GridFTP propagation - local target: %s" % local_absolute_target)
        
        cmd = self._get_pull_command(remote_source, local_absolute_target)
        self.c.log.info("Running GridFTP command: %s" % cmd)
    
    def unpropagate(self, local_absolute_source, remote_target):
        self.c.log.info("GridFTP unpropagation - local source: %s" % local_absolute_source)
        self.c.log.info("GridFTP unpropagation - remote target: %s" % remote_target)
        
        cmd = self._get_push_command(local_absolute_source, remote_target)
        self.c.log.info("Running GridFTP command: %s" % cmd)
        
    # --------------------------------------------------------------------
    
    def _get_push_command(self, local, remote):
        """Return command to send a local file to somewhere remote
        local -- absolute path on local filesystem
        remote -- URL like "gsiftp://host:port/path"
        """
        return self._get_remote_command(local, remote, True)
        
    def _get_pull_command(self, remote, local):
        """Return command to retrieve a remote file.
        remote -- URL like "gsiftp://host:port/path"
        local -- absolute path on local filesystem
        """
        return self._get_remote_command(local, remote, False)
        
    def _get_remote_command(self, local, remote, push):
        
        if remote[:9] != "gsiftp://":
            raise InvalidInput("invalid gsiftp url, not 'gsiftp://': " + remote)
            
        given = remote[9:]
        path_index = string.find(given, '/')
        if path_index == -1:
            raise InvalidInput("invalid gsiftp url, no host? " + remote)
        
        if push:
            return self.guc + " " + local + " " + remote
        else:
            return self.guc + " " + remote + " " + local
        
        
        