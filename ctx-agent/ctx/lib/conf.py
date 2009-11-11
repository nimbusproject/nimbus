import os
import sys
import string
import ConfigParser

from ctx_exceptions import InvalidConfig, ProgrammingError

# ############################################################
# Globals
# #########################################################{{{

VERSION = "2.2"

# Apache License 2.0:
LICENSE = """

Copyright 1999-2009 University of Chicago

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License.

"""

# Project home: http://workspace.globus.org

# In the future, this value may come from bootstrap or the information
# may even be notification based.
POLL_DELAY_SECONDS_DEFAULT = 3

# For parsing response
NS_CTXBROKER = "http://www.globus.org/2008/12/nimbus"
NS_CTX = NS_CTXBROKER + "/contextualization"
NS_CTXTYPES = NS_CTXBROKER + "/ctxtypes"
NS_CTXDESC = NS_CTXBROKER + "/ctxdescription"

# }}} END: Globals

# ############################################################
# Embedded, default configuration file
# #########################################################{{{

DEFAULTCONFIG = """

# This is the default configuration file for the program.

# It can be changed inline or copied out into a file whose path can
# be passed into the program via command-line (see -h).  If that is done, 
# the program will NOT fall back to this default configuration if there
# is an error or misconfiguration with the supplied config file.


[sshd]

# Absolute path only, the sshd host key
generatedkey: /etc/ssh/ssh_host_rsa_key.pub

# SSHd hosts config is adjusted directly by this program right now, adds
# equiv hostnames and pubkeys for host based authorization across the whole
# contextualization context.

hostbasedconfig: /etc/hosts.equiv
knownhostsconfig: /etc/ssh/ssh_known_hosts


[reginst]

#### Regular instantiation

# Path to metadata server URL file
path: /var/nimbus-metadata-server-url

# Comma separated names of possible identity nics (do NOT use lo, for example).
# These are REAL local interface names that may be present (each is checked for
# an IP configuration).
nicnames: eth0, eth1


[systempaths]

# These can be relative or absolute paths.
hostname: hostname
curl: curl


# ***NOTE: it is unlikely you need to alter the configurations below this line

[taskpaths]

#### Calling order (this is explained in more detail below).
#### 0-etchosts
#### 1-ipandhost
#### 2-thishost
#### 3-data
#### 4-restarts
#### 5-thishostfinalize


#### 0-etchosts
#
# every identity seen is always sent to etchosts
#    arg1: IP
#    arg2: short hostname
#    arg3: FQDN

etchosts: /opt/nimbus/ctx-scripts/0-etchosts.sh


#### 1-ipandhost
#
# Directory where the scripts live that match the required role names.
# See samples for more explanation.
#
# These role scripts receive:
#    arg1: IP
#    arg2: short hostname
#    arg3: FQDN

ipandhostdir: /opt/nimbus/ctx-scripts/1-ipandhost


#### 2-thishost
#
# "thishost" scripts are called with network information known about the
# host this program is running on.
#
# Each script receives:
#    arg1: IP
#    arg2: Short local hostname
#    arg3: FQDN
#
# The names of the scripts in this directory must correspond to the interface
# that the context broker knows about, not the local interface which may not
# match.
# 
# Particular scripts may be absent.  The entire directory configuration
# may also be absent.

thishostdir: /opt/nimbus/ctx-scripts/2-thishost


#### 3-data
#
# The opaque data directory contains scripts that match provided data names.
# If data fields exist in the context, the data is written to a file and
# that file absolute path is sent as only argument to the scripts.
# The scripts are called after 'thishost' but before 'restarts'.

datadir: /opt/nimbus/ctx-scripts/3-data


#### 4-restarts
#
# The restart directory contains scripts that match provided roles.
#
# After all role information has been added via the ipandhostdir script AND
# after the "thishost" scripts have successfully run, this program will call
# the restart script for each required role it knows about (presumably to
# restart the service now that config has changed).
#
# No arguments are sent.
#
# It is OK for the required role to not have a script in this directory.

restartdir: /opt/nimbus/ctx-scripts/4-restarts


#### 5-thishostfinalize
#
# The "thishostfinalize" scripts are called with network information known
# about the host this program is running on.  It is called AFTER the restart
# scripts are successfully called.
#
# Each script receives:
#    arg1: IP
#    arg2: Short local hostname
#    arg3: FQDN
#
# The names of the scripts in this directory must correspond to the interface
# that the context broker knows about, not the local interface which may not
# match.
# 
# Particular scripts may be absent.  The entire directory configuration
# may also be absent.

thishostfinalizedir: /opt/nimbus/ctx-scripts/5-thishost-finalize


# "problem" script
# In case of problems, could call poweroff.  This script will be called after
# an attempt to notify the service of the error (that notification provides
# a log of the run to the context broker).
#
# Must be configured if "--poweroff" (-p) argument is used, will not be
# consulted if that argument is not used.

problemscript: /opt/nimbus/ctx-scripts/problem.sh


[ctxservice]

# logfile of the run
# If config is missing, no log will be written and nothing will be sent to
# service for error reporting.
logfilepath: /opt/nimbus/ctxlog.txt

# Directory where the program can write temporary files
scratchspacedir: /opt/nimbus/ctx/tmp

retr_template: /opt/nimbus/ctx/lib/retr-template-001.xml
retr_template2: /opt/nimbus/ctx/lib/retr-template-002.xml
err_template: /opt/nimbus/ctx/lib/err-template-001.xml
err_template2: /opt/nimbus/ctx/lib/err-template-002.xml
ok_template: /opt/nimbus/ctx/lib/ok-template-001.xml
ok_template2: /opt/nimbus/ctx/lib/ok-template-002.xml



[ec2]

#### EC2 instantiation (alternative to regular method)

# URLs for the Amazon REST instance data API
localhostnameURL:  http://169.254.169.254/2007-01-19/meta-data/local-hostname
publichostnameURL: http://169.254.169.254/2007-01-19/meta-data/public-hostname
localipURL:        http://169.254.169.254/2007-01-19/meta-data/local-ipv4
publicipURL:       http://169.254.169.254/2007-01-19/meta-data/public-ipv4
publickeysURL:     http://169.254.169.254/2007-01-19/meta-data/public-keys/
userdataURL:       http://169.254.169.254/2007-01-19/user-data


"""
# }}} END: Embedded, default configuration file



# ############################################################
# Configuration objects
# #########################################################{{{

# These are objects that are populated from a combination of
# commandline and config file settings.  Their fields are set
# to control the behavior of the action.

class CommonConf:

    """Class for configurations common to all actions and objects."""

    def __init__(self, trace, evaluate, ipandhostdir, restartdir, polltime, sshdkeypath, scratchdir, retr_template, retr_template2, err_template, err_template2, ok_template, ok_template2, hostbasedconfig, knownhostsconfig, thishostdir, thishostfinalizedir, logfilepath, curlpath, hostnamepath, datadir, etchosts_exe):
        """Set the configurations.

        todo: many of these configs can be scoped out of common, the list
              has gotten long ...
                     
        Required parameters:

        * trace -- Make extra log statements to DEBUG level (boolean).

        * evaluate -- "Dryrun" mode (boolean).
        
        * ipandhostdir -- The directory with role-specific task scripts that
        accept IP as arg1 and hostname as arg2
        
        * restartdir -- The directory with role-specific task scripts that,
        if they exist, will be called after a role has received all new
        information (presumably to restart or start the service with its
        new config).
        
        * polltime -- Poll delay in seconds.
        
        * sshdkeypath -- Path to created sshd pubkey.
        
        * scratchdir -- Directory for writing temporary files to.
        
        * retr_template -- Template XML file for WS retrieval
        
        * retr_template2 -- Template XML file for WS retrieval
        
        * err_template -- Template XML file for WS error reporting
        
        * err_template2 -- Template XML file for WS error reporting
        
        * ok_template -- Template XML file for WS OK reporting
        
        * ok_template2 -- Template XML file for WS OK reporting
        
        * hostbasedconfig -- for adding hostnames to do host based authz
        
        * knownhostsconfig -- for adding pubkeys to do host based authz
        
        * thishostdir -- 'thishost' directory, see config file
        
        * thishostfinalizedir -- 'thishostfinalize' dir, see config file
        
        * logfilepath -- Path to write log file
        
        * curlpath -- relative or abs command for curl
        
        * hostnamepath -- relative or abs command for hostname
        
        * datadir -- Directory with data specific task scripts that will be
        called when a data field is present in the context information.
        
        * etchosts_exe -- path to send all identity info

        """

        self.trace = trace
        self.evaluate = evaluate
        
        try:
            polltime = int(polltime)
        except:
            raise InvalidConfig("polltime is required to be an integer (number of seconds)")
            
        self.polltime = polltime
        self.sshdkeypath = sshdkeypath
        self.scratchdir = scratchdir
        self.hostbasedconfig = hostbasedconfig
        self.knownhostsconfig = knownhostsconfig
        self.ipandhostdir = ipandhostdir
        self.restartdir = restartdir
        self.thishostdir = thishostdir
        self.thishostfinalizedir = thishostfinalizedir
        self.logfilepath = logfilepath
        self.curlpath = curlpath
        self.hostnamepath = hostnamepath
        self.datadir = datadir
        self.etchosts_exe = etchosts_exe
        
        # On the crazier side of things -- perhaps use ZSI but we also might
        # not use WS at all next round, so sticking with text-over-http hack
        # One important characteristic: a very low dependency situation (curl)
        self.retr_template = retr_template
        self.retr_template2 = retr_template2
        self.err_template = err_template
        self.err_template2 = err_template2
        self.ok_template = ok_template
        self.ok_template2 = ok_template2
        
class AmazonConf:

    """Class for amazon configurations, version 1."""

    def __init__(self, localhostnameURL, publichostnameURL, localipURL, publicipURL, publickeysURL, userdataURL):
        """Set the configurations.

        Required parameters:

        * localhostnameURL -- The 

        * publichostnameURL -- The 
        
        * localipURL --  The
        
        * publicipURL -- The
        
        * publickeysURL -- The
        
        * userdataURL -- The

        Raise InvalidConfig if there is a problem with parameters.

        """
        
        self.localhostnameURL = localhostnameURL
        self.publichostnameURL = publichostnameURL
        self.localipURL = localipURL
        self.publicipURL = publicipURL
        self.publickeysURL = publickeysURL
        self.userdataURL = userdataURL

class ReginstConf:

    """Class for regular configurations, version 1."""

    def __init__(self, path, niclist):
        """Set the configurations.

        Required parameters:

        * path -- Path to bootstrap file
        
        * niclist -- list of possible nic names

        Raise InvalidConfig if there is a problem with parameters.

        """
        
        if not path:
            raise InvalidConfig("No bootstrap path, illegal argument")
        
        name = "metadata server URL path"
        if not os.path.exists(path):
            msg = "%s '%s' does not exist on filesystem" % (name, path)
            raise InvalidConfig(msg)
            
        if not os.path.isabs(path):
            msg = "%s '%s' should be absolute path" % (name, path)
            raise InvalidConfig(msg)
            
        self.path = path
        
        if not isinstance(niclist, list):
            raise InvalidConfig("niclist must be a list")
            
        if len(niclist) == 0:
            raise InvalidConfig("niclist must not be empty")
            
        self.niclist = niclist

# }}} END: Configuration objects

# ############################################################
# Convert configurations
# #########################################################{{{

def getCommonConf(opts, config):
    """Return populated CommonConf object or raise InvalidConfig.

    Required parameters:

    * opts -- parsed optparse opts

    * config -- parsed ConfigParser

    """

    if not opts:
        raise ProgrammingError("opts is None")
    if not config:
        raise ProgrammingError("config is None")
        
    polltime = POLL_DELAY_SECONDS_DEFAULT
    if opts.polltime:
        polltime = opts.polltime
        
    try:
        sshdkeypath = config.get("sshd", "generatedkey")
        hostbasedconfig = config.get("sshd", "hostbasedconfig")
        knownhostsconfig = config.get("sshd", "knownhostsconfig")
        scratchdir = config.get("ctxservice", "scratchspacedir")
        retr_template = config.get("ctxservice", "retr_template")
        retr_template2 = config.get("ctxservice", "retr_template2")
        err_template = config.get("ctxservice", "err_template")
        err_template2 = config.get("ctxservice", "err_template2")
        ok_template = config.get("ctxservice", "ok_template")
        ok_template2 = config.get("ctxservice", "ok_template2")
        ipandhostdir = config.get("taskpaths", "ipandhostdir")
        restartdir = config.get("taskpaths", "restartdir")
        thishostdir = config.get("taskpaths", "thishostdir")
        thishostfinalizedir = config.get("taskpaths", "thishostfinalizedir")
        logfilepath = config.get("ctxservice", "logfilepath")
        curl = config.get("systempaths", "curl")
        hostname = config.get("systempaths", "hostname")
        datadir = config.get("taskpaths", "datadir")
        etchosts_exe = config.get("taskpaths", "etchosts")
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        msg = "%s: %s" % (str(exceptname), str(sys.exc_value))
        raise InvalidConfig(msg)
        
    # no evaluate yet, pass False for now
    return CommonConf(opts.trace, False, ipandhostdir, restartdir, polltime, sshdkeypath, scratchdir, retr_template, retr_template2, err_template, err_template2, ok_template, ok_template2, hostbasedconfig, knownhostsconfig, thishostdir, thishostfinalizedir, logfilepath, curl, hostname, datadir, etchosts_exe)

def getReginstConf(opts, config):
    """Return populated reginstConf object or raise InvalidConfig

    Required parameters:

    * opts -- parsed optparse opts

    * config -- parsed ConfigParser
    
    Raise InvalidConfig if there is a problem.

    """
    
    if not opts:
        raise ProgrammingError("opts is None")
    if not config:
        raise ProgrammingError("config is None")
        
    path = opts.bootstrap_path
    
    try:
        # commandline takes precendence
        if not path:
            path = config.get("reginst", "path")
        nicnames = config.get("reginst", "nicnames")
        niclist = map(string.strip, nicnames.split(","))
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        msg = "%s: %s" % (str(exceptname), str(sys.exc_value))
        raise InvalidConfig(msg)
    
    return ReginstConf(path, niclist)
    
def getAmazonConf(opts, config):
    """Return populated AmazonConf object or raise InvalidConfig

    Required parameters:

    * opts -- parsed optparse opts

    * config -- parsed ConfigParser
    
    Raise InvalidConfig if there is a problem.

    """

    if not opts:
        raise ProgrammingError("opts is None")
    if not config:
        raise ProgrammingError("config is None")
        
    try:
        localhostnameURL = config.get("ec2", "localhostnameURL")
        publichostnameURL = config.get("ec2", "publichostnameURL")
        localipURL = config.get("ec2", "localipURL")
        publicipURL = config.get("ec2", "publicipURL")
        publickeysURL = config.get("ec2", "publickeysURL")
        userdataURL = config.get("ec2", "userdataURL")
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        msg = "%s: %s" % (str(exceptname), str(sys.exc_value))
        raise InvalidConfig(msg)
    
    return AmazonConf(localhostnameURL, publichostnameURL, localipURL, publicipURL, publickeysURL, userdataURL)
    
# }}} END: Convert configurations

# ############################################################
# External configuration
# #########################################################{{{

class FakeFile:

    """Class for feeding a string to an API needing a file-like object.

    Only the readline() method is implemented.

    """

    def __init__(self, string):

        """Instantiate class with string.

        Required arguments:

        * string -- String to treat as file-like object, can contain newlines

        """

        if string is None:
            raise InvalidConfig("config file (string) can not be None")

        self.lines = string.splitlines(True)
        self.gen = self.genline()

    def readline(self):
        """Return the next line in the file until EOF (then return None)."""
        try:
            ret = self.gen.next()
        except StopIteration:
            return None
        return ret

    def genline(self):
        """Return the next line in the list until the end (StopIteration)."""
        for line in self.lines:
            yield line

def getconfig(string=None, filepath=None):
    """Return SafeConfigParser instantiated from supplied config source.

    Keyword arguments:

    * string -- String containing the text of the config file (with newlines)

    * filepath -- Path to a config file

    One (and not both) of these keyword arguments must be supplied.

    Raise InvalidConfig if there is a problem.

    """

    if not string and not filepath:
        raise InvalidConfig("neither string nor filepath was supplied "
                            "to getconfig()")
    if string and filepath:
        raise InvalidConfig("both string and filepath were supplied "
                            "to getconfig()")

    config = ConfigParser.SafeConfigParser()
    if string:
        config.readfp(FakeFile(string))
    if filepath:
        config.read(filepath)
    return config
    
# }}} END: External configuration


