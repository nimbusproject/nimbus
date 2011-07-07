from commands import getstatusoutput
import os
import string
from propagate_adapter import PropagationAdapter
from workspacecontrol.api.exceptions import *
from propagate_common import url_parse

class cp_propadapter(PropagationAdapter):
        
    def __init__(self, params, common):
        PropagationAdapter.__init__(self, params, common)
        self.cp = None

    def validate(self):
        self.c.log.debug("validating cp propagation adapter")
    
        self.cp = self.p.get_conf_or_none("propagation", "cp")
        if not self.cp:
            self.cp = "/bin/cp"
            
        if os.path.isabs(self.cp):
            if not os.access(self.cp, os.F_OK):
                raise InvalidConfig("cp is configured with an absolute path, but it does not seem to exist: '%s'" % self.cp)
                
            if not os.access(self.cp, os.X_OK):
                raise InvalidConfig("CP is configured with an absolute path, but it does not seem executable: '%s'" % self.cp)

        self.c.log.debug("CP configured: %s" % self.cp)
        
    def validate_propagate_source(self, imagestr):
        # will throw errors if invalid
        self._get_cp_command(imagestr, "fake")
    
    def validate_unpropagate_target(self, imagestr):
        # will throw errors if invalid
        self._get_cp_command("fake", imagestr)
    
    def propagate(self, remote_source, local_absolute_target):
        self.c.log.info("CP propagation - source: %s" % remote_source)
        self.c.log.info("CP propagation - target: %s" % local_absolute_target)
       
        src = self._parse_url(remote_source)
        cmd = self._get_cp_command(src, local_absolute_target) 
        self._run(cmd)

    def _run(self, cmd): 
        self.c.log.info("Running CP command: %s" % cmd)
        ret,output = getstatusoutput(cmd)
        if ret:
            errmsg = "problem running command: '%s' ::: return code" % cmd
            errmsg += ": %d ::: output:\n%s" % (ret, output)
            self.c.log.error(errmsg)
            raise UnexpectedError(errmsg)
        self.c.log.info("Transfer complete.")
    
    def unpropagate(self, local_absolute_source, remote_target):
        self.c.log.info("CP unpropagation - source: %s" % local_absolute_source)
        self.c.log.info("CP unpropagation - target: %s" % remote_target)
       
        dest = self._parse_url(remote_target) 
        cmd = self._get_cp_command(local_absolute_source, dest)
        self._run(cmd)
        
    def _parse_url(this, url):
        scheme = "cp://" 
        if url[:len(scheme)] != scheme:
            raise InvalidInput("invalid cp url, not cp:// " + url)
        (s, user, pw, host, port, path) = url_parse(url)
        return path

    def _get_cp_command(self, src, dest):
        cmd = self.cp + " " + src + " "  + dest
        return cmd
        
