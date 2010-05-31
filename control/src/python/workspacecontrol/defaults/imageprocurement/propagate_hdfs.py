from commands import getstatusoutput
import os
from time import time
from propagate_adapter import PropagationAdapter
from workspacecontrol.api.exceptions import *


class propadapter(PropagationAdapter):
    """Propagation adapter for HDFS.
    
    Image file must exist on HDFS to validate propagation
    Image file must not exist on HDFS to validate unpropagation
    """
        
    def __init__(self, params, common):
        PropagationAdapter.__init__(self, params, common)
        self.hadoop = None                  # Hadoop executable location
        self.parsed_source_url = None       # Source URL when propagating
        self.parsed_dest_url = None         # Destination URL when unpropagating
        
    def validate(self):
        self.c.log.debug("Validating hdfs propagation adapter")
        
        self.hadoop = self.p.get_conf_or_none("propagation", "hdfs")
        if not self.hadoop:
            raise InvalidConfig("no path to hadoop")
        
        # Expand any enviroment variables first
        self.hadoop = os.path.expandvars(self.hadoop)
        if os.path.isabs(self.hadoop):
            if not os.access(self.hadoop, os.F_OK):
                raise InvalidConfig("HDFS resolves to an absolute path, but it does not seem to exist: '%s'" % self.hadoop)
                
            if not os.access(self.hadoop, os.X_OK):
                raise InvalidConfig("HDFS resolves to an absolute path, but it does not seem executable: '%s'" % self.hadoop)
        else:
            raise InvalidConfig("HDFS contains unknown enviroment varibales: '%s'" % self.hadoop)    

        self.c.log.debug("HDFS configured: %s" % self.hadoop)
        
    def validate_propagate_source(self, imagestr):
        # Validate uri format
        if imagestr[:7] != 'hdfs://':
            raise InvalidInput("Invalid hdfs url, must be of the form hdfs://")
        
        # Validate file exists on filesystem
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
        # Validate uri format
        if imagestr[:7] != 'hdfs://':
            raise InvalidInput("Invalid hdfs url, must be of the form hdfs://")
        
        # Validate file does not exists on filesystem already
        cmd = self.__generate_hdfs_test_cmd(imagestr)
        try:
            status,output = getstatusoutput(cmd)
        except:
            errmsg = "HDFS validation - unknown error when checking that directory exists."
            self.c.log.error(errmsg)
            raise UnexpectedError(errmsg)
        # 0 returned if file exists
        # 1 returned if file does not exist
        if not status:
            errmsg = "HDFS validation - File already exists at destination: %s" % imagestr
            self.c.log.error(errmsg)
            raise InvalidInput(errmsg)
        
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
        self.c.log.info("HDFS unpropagation - local target: %s" % local_absolute_target)
        self.c.log.info("HDFS unpropagation - remote source: %s" % remote_source)
                
        cmd = self.__generate_hdfs_push_cmd(remote_source, local_absolute_target)
        self.c.log.info("Running HDFS command: %s" % cmd)
        transfer_time = -time()
        try:
            status,output = getstatusoutput(cmd)
        except:
            errmsg = "HDFS unpropagation - unknown error.  Unpropagation failed"
            self.c.log.error(errmsg)
            raise UnexpectedError(errmsg)
        
        if status:
            errmsg = "problem running command: '%s' ::: return code" % cmd
            errmsg += ": %d ::: output:\n%s" % (status, output)
            self.c.log.error(errmsg)
            raise UnexpectedError(errmsg)
        else:
            transfer_time += time()
            self.c.log.info("Unpropagation transfer complete:  %fs" % round(transfer_time))
    
    #--------------------------------------------------------------------------
    # Private helper functions
    
    def __generate_hdfs_pull_cmd(self, remote_target, local_absolute_target):
        # Generate command in the form of:
        # /path/to/hadoop/bin/hadoop fs -fs <files system uri> -copyToLocal <src> <localdst>
        if not self.parsed_source_url:
            self.parsed_source_url = self.__parse_url(remote_target)
            
        ops = [self.hadoop, "fs",
               "-fs", self.parsed_source_url[0]+'://'+self.parsed_source_url[1],
               "-copyToLocal", self.parsed_source_url[2], local_absolute_target]
        cmd = " ".join(ops)
        return cmd
    
    def __generate_hdfs_push_cmd(self, remote_target, local_absolute_target):
        # Generate command in the form of:
        # /path/to/hadoop/bin/hadoop fs -fs <files system uri> -copyFromLocal <local> <dst>
        if not self.parsed_dest_url:
            self.parsed_dest_url = self.__parse_url(remote_target)
            
        ops = [self.hadoop, "fs",
               "-fs", self.parsed_dest_url[0]+'://'+self.parsed_dest_url[1],
               "-copyFromLocal", local_absolute_target ,self.parsed_dest_url[2]]
        cmd = " ".join(ops)
        return cmd
    
    def __generate_hdfs_test_cmd(self, imagestr):
        # Generate command in the form of:
        # /path/to/hadoop/bin/hadoop dfs -fs <file system uri> -test -e <path>
        if not self.parsed_source_url:
            self.parsed_source_url = self.__parse_url(imagestr)
            
        ops = [self.hadoop, "fs",
               "-fs", self.parsed_source_url[0]+'://'+self.parsed_source_url[1],
               "-test", "-e", self.parsed_source_url[2]]
        cmd = " ".join(ops)
        return cmd
    
    def __parse_url(self, url):
        # Since Python2.4 urlparse library doesn't recognize the hdfs scheme,
        # we need to parse the url by hand.
        url = url.split('://')
        if len(url) != 2:
            raise InvalidInput("url not of the form <scheme>://<netloc>/<path>")
        scheme = url[0]
        netloc, path = url[1].split('/', 1)
        # Add leading / back in since it was used to partition netloc from path
        path = '/'+path
        return (scheme, netloc, path)

