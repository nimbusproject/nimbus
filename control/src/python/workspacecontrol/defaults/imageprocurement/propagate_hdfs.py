from commands import getstatusoutput
import os
import string
from time import time
from urlparse import urlparse
from propagate_adapter import PropagationAdapter
from workspacecontrol.api.exceptions import *


class propadapter(PropagationAdapter):
    """Enable nimbus to valitate and pull images from an hdfs storage.
    """
        
    def __init__(self, params, common):
        PropagationAdapter.__init__(self, params, common)
        self.hdfs_location = None
        
        
    def validate(self):
        self.c.log.debug("Validating hdfs propagation adapter")
        if not os.environ.has_key('HADOOP_HOME'):
            errmsg = "HDFS validation error - The $HADOOP_HOME enviroment variable is not set."
            self.c.log.error(errmsg)
            raise UnexpectedError(errmsg)
        
        # Generate absolute path to hadoop executable
        hadoop_home = os.path.expandvars("$HAHOOP_HOME")
        self.hdfs_location = os.path.join(hadoop_home, 'bin/hadoop')
        
        if not os.path.exists(self.hdfs_location):
            self.hdfs_location = None
            errmsg = "HDFS validation error - The path: %s does not exist" % self.hdfs_location
            self.c.log.error(errmsg)
            raise UnexpectedError(errmsg)
        if not os.path.isfile(self.hdfs_location):
            self.hdfs_location = None
            errmsg = "HDFS validation error - The path: %s is not a file" % self.hdfs_location
            self.c.log.error(errmsg)
            raise UnexpectedError(errmsg)
        
    def validate_propagate_source(self, imagestr):
        # Validate uri format
        url = urlparse(imagestr)
        if url[0] != 'hdfs':
            raise InvalidInput("Invalid hdfs url, must be of the form hdfs://")
        
        # Validate file exists on filesystem
        if not self.hdfs_location:
            self.validate()
        cmd = self.__generate_hdfs_test_cmd(imagestr)
        try:
            status,output = getstatusoutput(cmd)
        except:
            errmsg = "HDFS validation - unknown error when checking that file exists."
            self.c.log.error(errmsg)
            raise UnexpectedError(errmsg)
        # 0 returned if file exists
        # 1 returned if file does not exist
        if status:
            errmsg = "HDFS validation - file does not exist on hdfs."
            self.c.log.error(errmsg)
            raise InvalidInput(errmsg)
        
    
    def validate_unpropagate_target(self, imagestr):
        raise InvalidInput("HDFS unpropagation is not supported at this time.  Check back later.")
    
    def propagate(self, remote_source, local_absolute_target):
        self.c.log.info("HDFS propagation - remote source: %s" % remote_source)
        self.c.log.info("HDFS propagation - local target: %s" % local_absolute_target)
        
        cmd = self.__generate_hdfs_pull_cmd(remote_source, local_absolute_target)
        self.c.log.info("Running HDFS command: %s" % cmd)
        transfer_time = -time()
        try:
            status,output = getstatusoutput(cmd)
        except:
            errmsg = "HDFS propagation - unknown error.  Propagation failed"
            self.c.log.error(errmsg)
            raise UnexpectedError(errmsg)
        
        if status:
            errmsg = "problem running command: '%s' ::: return code" % cmd
            errmsg += ": %d ::: output:\n%s" % (status, output)
            self.c.log.error(errmsg)
            raise UnexpectedError(errmsg)
        else:
            transfer_time += time()
            self.c.log.info("Transfer complete:  %fs" % round(transfer_time))
        
    
    def unpropagate(self, local_absolute_source, remote_target):
        raise InvalidInput("HDFS unpropagation is not supported at this time.  Check back later.")
    
    #--------------------------------------------------------------------------
    
    def __generate_hdfs_pull_cmd(self, remote_target, local_absolute_target):
        # Generate command in the form of:
        # /path/to/hadoop/bin/hadoop dfs -fs <files system uri> -copyToLocal <src> <localdst>
        
        # <scheme>://<netloc>/<path>;<params>?<query>#<fragment>
        url = urlparse(imagestr)
        ops = [self.hdfs_location, "dfs",
               "-fs", url[1],
               "-coptToLocal", url[2], local_absolute_target]
        cmd = " ".join(ops)
        return cmd
    
    def __generate_hdfs_test_cmd(self, imagestr):
        # Generate command in the form of:
        # /path/to/hadoop/bin/hadoop dfs -fs <file system uri> -test -e <path>
        
        # <scheme>://<netloc>/<path>;<params>?<query>#<fragment>
        url = urlparse(imagestr)
        ops = [self.hdfs_location, "dfs",
               "-fs", url[1],
               "-test", "-e", url[2]]
        cmd = " ".join(ops)
        return cmd       

