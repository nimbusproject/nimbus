import os
import sys
import time
import shutil
import tempfile

try:
    #import elementtree.ElementTree as ET
    from xml.etree import ElementTree as ET #XXX what Python version support this?
except ImportError:
    print "elementtree not installed on the system, trying our backup copy"
    import embeddedET.ElementTree as ET #FIXME this is now broken

#local imports
from ctx_exceptions import InvalidConfig, UnexpectedError, ProgrammingError
from ctx_logging import getlog
from ctx_types import RetrieveResult
from utils import runexe, ifconfig, uuidgen, write_repl_file
from parsers import response2_parse_file, response2_parse_for_fatal
from conf import NS_CTX, NS_CTXTYPES, NS_CTXDESC

# #########################################################
# OK/ERR reporting
# #########################################################
    
def set_broker_okaction(instance):
    global _brokerOK
    _brokerOK = instance
    
def get_broker_okaction():
    try:
        _brokerOK
    except:
        return None
    return _brokerOK
    
def set_broker_erraction(instance):
    global _brokerERR
    _brokerERR = instance
    
def get_broker_erraction():
    try:
        _brokerERR
    except:
        return None
    return _brokerERR

# }}} END: OK/ERR reporting
    


# ############################################################
# Bootstrap consumption (v3)
# #########################################################{{{

class Bootstrap:
    
    """Class for bootrap parsing, in order to override later on.
       This is for syntax v3.
    """
    
    # spec is at least 20 ='s:
    BOOTSTRAP_FIELD_SEPARATOR = "===================="
    
    def __init__(self, text, log_override=None):
        self.log = getlog(override=log_override)
        self.cluster = None
        self.service_url = None
        self.resource_key = None
        self.credential_string = None
        self.private_key_string = None
        self.parse_xml_userdata(text)
            
    #### utlities etc.
    
    def parse_xml_userdata(self, text):
        
        if not isinstance(text, str):
            self.log.error("Bootstrap text input is null or not a string?")
            return None
        
        if text.strip() == "":
            self.log.error("Bootstrap text is empty?")
            return None
            
        self.log.debug("First 20 chars of userdata: '%s'" % text[:20])
        
        try:
            tree = ET.fromstring(text)
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            name = str(exceptname)
            err = str(sys.exc_value)
            errmsg = "Problem parsing userdata: %s: %s\n" % (name, err)
            self.log.error(errmsg)
            return None
        
        if tree.tag != "NIMBUS_CTX":
            raise UnexpectedError("unknown element in userdata: '%s' (expecting NIMBUS_CTX)" % str(tree.tag))
            
        namespace_context = NS_CTX
        namespace_types = NS_CTXTYPES
        namespace_desc = NS_CTXDESC
        
        contactTag = "{%s}contact" % namespace_desc
        contact = tree.find(contactTag)
        
        clusterTag = "{%s}cluster" % namespace_desc
        clusterXML = tree.find(clusterTag)
                
        if contact == None:
            raise UnexpectedError("could not locate broker contact type in userdata")
        
        if clusterXML == None:
            raise UnexpectedError("could not locate cluster context document in userdata")
        clustertext = ET.tostring(clusterXML, encoding="UTF-8")
        clusterXMllines = clustertext.split("\n")
        self.cluster = "\n".join(clusterXMllines[1:])
            
        brokerURLTag = "{%s}brokerURL" % namespace_desc
        self.service_url = contact.find(brokerURLTag)
        
        contextIDTag = "{%s}contextID" % namespace_desc
        self.resource_key = contact.find(contextIDTag)
        
        secretTag = "{%s}secret" % namespace_desc
        secret = contact.find(secretTag)
        
        if self.service_url == None:
            raise UnexpectedError("could not locate broker URL in userdata")
        else:
            self.service_url = self.service_url.text
            
        if self.resource_key == None:
            raise UnexpectedError("could not locate context ID in userdata")
        else:
            keyprefix = "{%s}NimbusContextBrokerKey=" % namespace_context
            key = self.resource_key.text.strip()
            if key[:79] != keyprefix:
                raise UnexpectedError("context ID has unexpected namespace: '%s'" % key)
            self.resource_key = key[79:]
            
        if secret == None:
            raise UnexpectedError("could not locate secret in userdata")
        
        sections = self.get_sections_tuple(secret.text)
        self.credential_string = sections[1]
        self.private_key_string = sections[2]
        
    def get_sections_tuple(self, text):
        if not isinstance(text, str):
            self.log.error("text input is null or not a string?")
            return None
        
        if text.strip() == "":
            self.log.error("text is empty?")
            return None
            
        lines = text.split("\n")
            
        sections = []
        buf = ""
        for line in lines:
            if line.strip() == "":
                continue
            if line.strip().startswith(Bootstrap.BOOTSTRAP_FIELD_SEPARATOR):
                sections.append(buf)
                buf = ""
                continue
            buf += line + "\n"
        
        return sections

# }}} Bootstrap consumption (v3)

class DefaultOK:
    
    def __init__(self, cmd, log_override=None):
        self.runcmd = cmd
        self.log = getlog(override=log_override)

    def run(self):
        if not self.runcmd:
            errmsg = "no runcmd configured for defaultOK (?)"
            try:
                self.log.error(errmsg)
            except:
                print >>sys.stderr, errmsg
            return
        
        msg = "Attempting OK report to context broker."
        try:
            self.log.info(msg)
        except:
            print >>sys.stderr, msg
        
        self.log.info("CMD: " + self.runcmd)
        
        (exit, stdout, stderr) = runexe(self.runcmd, killtime=10)
        result = "'%s': exit=%s, stdout='%s'," % (self.runcmd, exit, stdout)
        result += " stderr='%s'" % (stderr)
        
        self.log.debug(result)
        
        if exit != "0":
            msg = "PROBLEM: reporting OK to context broker failed, "
            msg += "result: %s" % result
            self.log.error(msg)
        else:
            self.log.info("Reported OK to context broker.")
        
class DefaultERR:
    
    def __init__(self, cmd, templatepath, log_override=None):
        self.runcmd = cmd
        self.templatepath = templatepath
        self.log = getlog(override=log_override)
        
    def run(self, errcodestr, errmessage):
        
        if not self.runcmd:
            errmsg = "no runcmd configured for defaultERR (?)"
            try:
                self.log.error(errmsg)
            except:
                print >>sys.stderr, errmsg
            return
            
        if not self.templatepath:
            errmsg = "no templatepath configured for defaultERR (?)"
            try:
                self.log.error(errmsg)
            except:
                print >>sys.stderr, errmsg
            return
            
        self.complete_template(errcodestr, errmessage)
            
        msg = "Attempting error report to context broker."
        try:
            self.log.info(msg)
        except:
            print >>sys.stderr, msg
            
        self.log.info("CMD: " + self.runcmd)
        
        (exit, stdout, stderr) = runexe(self.runcmd, killtime=10)
        result = "'%s': exit=%s, stdout='%s'," % (self.runcmd, exit, stdout)
        result += " stderr='%s'" % (stderr)
        
        self.log.info(result)
        
        if exit != "0":
            msg = "PROBLEM: reporting ERROR to context broker failed, "
            msg += "result: %s" % result
            self.log.error(msg)
        else:
            self.log.info("Reported ERROR to context broker.")
        
    def complete_template(self, errcodestr, errmessage):
        text = ""
        f = open(self.templatepath)
        try:
            for line in f:
                text += line
        finally:
            f.close()
            
        text = text.replace("REPLACE_ERRORCODE", str(errcodestr))
        text = text.replace("REPLACE_ERRORMSG", str(errmessage))
        write_repl_file(self.templatepath, text)
 


# ############################################################
# Action
# #########################################################{{{

class Action:

    """Parent class of every action."""
    
    def __init__(self, commonconf, log_override=None):
        """Initialize result and common conf fields.
        
        Required parameters:

        * commonconf -- CommonConf instance
        
        """
        self.common = commonconf
        self.result = None
        self.log = getlog(override=log_override)

    def run(self):
        """Start.

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """

        pass

# }}} END: Action

# ############################################################
# RegularInstantiation(Action)
# #########################################################{{{

class RegularInstantiation(Action):

    """Class implementing bootstrap retrieval from Nimbus metadata server.  It
    also populates the identity and sshd pubkey values."""
    
    def __init__(self, commonconf, regconf, log_override=None):
        """Instantiate object with configurations necessary to operate.

        Required parameters:

        * commonconf -- CommonConf instance

        * regconf -- ReginstConf instance

        Raise InvalidConfig if problem with the supplied configurations.
        
        Sets its result field to IdentityAndCredentialResult instance.

        """
        
        Action.__init__(self, commonconf, log_override)
        self.conf = regconf
        self.result = None
        
    def run(self):
        """Start.

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """
        
        self.realnics = self.analyze_active_nics()
        self.numnics = len(self.realnics)
        if self.numnics < 1:
            raise UnexpectedError("No network interfaces to resolve")
        elif self.numnics > 2:
            raise UnexpectedError("Too many network interfaces to resolve")
        
        # all fields are None in new instance:
        self.result = InstantiationResult()
        
        self._populate_one_or_two()
        
        # get generated pubkey from file
        self.result.pub_key = self.consume_sshd_key()
        if self.result.pub_key == None or self.result.pub_key == "":
            raise UnexpectedError("Couldn't obtain sshd pubkey")
            
        if self.common.trace:
            self.log.debug("Found sshd key: '%s'" % self.result.pub_key)
            
        self.consume_bootstrap()
        
    def _populate_one_or_two(self):
        
        # This does not set iface_name or iface2_name.
        # Those names must come from context broker and be matched to
        # what we've found on the real system (matching done by ip).
        
        self.result.iface_REAL_name = self.realnics[0]
        ifreq = ifconfig(self.realnics[0])
        addr = ifreq['addr']
        self.result.iface_hostname = ""
        self.result.iface_short_hostname = ""
        self.result.iface_ip = addr
            
        if self.numnics == 2:
            self.result.iface2_REAL_name = self.realnics[1]
            ifreq = ifconfig(self.realnics[1])
            addr = ifreq['addr']
            self.result.iface2_hostname = ""
            self.result.iface2_short_hostname = ""
            self.result.iface2_ip = addr
        
    def has_ip_assignment(self, iface):
        try:
            self.log.debug("Checking addresses of '%s' interface" % iface)
            ifreq = ifconfig(iface)
            self.log.debug("Address check of '%s' interface: " % ifreq)
            if ifreq and ifreq.has_key('addr'):
                addr = ifreq['addr']
                if addr:
                    msg = "Found address '%s' for interface '%s'" % (addr, iface)
                    self.log.debug(msg)
                    return True
        except:
            self.log.exception("exception looking for '%s' ip assignment" % iface)
        return False
        
    def analyze_active_nics(self):
        nics = []
        for nicname in self.conf.niclist:
            if self.has_ip_assignment(nicname):
                nics.append(nicname)
        return nics
        
    def consume_sshd_key(self):
        
        path = self.common.sshdkeypath
        
        if not os.path.exists(path):
            raise UnexpectedError("'%s' does not exist on filesystem" % path)
        
        # TODO consume better later
        text = ""
        f = open(path)
        try:
            for line in f:
                text += line
        finally:
            f.close()
        
        # strip any newlines and extra whitespace
        return text.replace("\n", "").strip()
        
    def get_stdout(self, url):
        # todo: switch to python classes
        
        timeout = 5
        curlcmd = "%s --silent --url %s" % (self.common.curlpath, url)
        
        (exit, stdout, stderr) = runexe(curlcmd, killtime=timeout+1)
        result = "'%s': exit=%s, stdout='%s'," % (curlcmd, exit, stdout)
        result += " stderr='%s'" % (stderr)
        
        if self.common.trace:
            self.log.debug(result)
        
        if exit != "0":
            msg = "PROBLEM: curl command failed, "
            msg += "result: %s" % result
            self.log.error(msg)
            return None
        return stdout
        
    # can raise UnexpectedError
    def consume_bootstrap(self):
        
        # get the metadata server URL
        try:
            f = None
            text = ""
            try:
                f = open(self.conf.path)
                for line in f:
                    text += line
            finally:
                if f:
                    f.close()
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            msg2 = "%s: %s" % (str(exceptname), str(sys.exc_value))
            
            msg = "Problem reading metadata URL file @ '%s'.  " % self.conf.path
            msg += msg2
            self.log.error(msg)
            raise UnexpectedError(msg)
        
        if text == None or text.strip() == "":
            raise UnexpectedError("no metadata URL to get userdata")
            
        metadataServerURL = text.strip()
        
        self.log.info("Found metadata server URL: '%s'" % metadataServerURL)
        
        metadataServerURL = metadataServerURL + "/2007-01-19/"
            
        geturl = metadataServerURL + "user-data"
        userdataText = self.get_stdout(geturl)
        if userdataText == None or userdataText.strip() == "":
            raise UnexpectedError("could not obtain userdata @ '%s'" % geturl)
        
        bootstrap = Bootstrap(userdataText)
        
        url = bootstrap.service_url
        if self.common.trace:
            self.log.debug("contextualization service URL = '%s'" % url)
        self.result.ctx_url = url
        
        key = bootstrap.resource_key
        if self.common.trace:
            self.log.debug("contextualization service key = '%s'" % key)
        self.result.ctx_key = key
        
        privPEM = bootstrap.private_key_string
        #self.log.debug("private key PEM = '%s'" % privPEM)
        self.result.ctx_keytext = privPEM
            
        pubPEM = bootstrap.credential_string
        if self.common.trace:
            self.log.debug("public cert PEM = '%s'" % pubPEM)
        self.result.ctx_certtext = pubPEM
        
        self.result.cluster_text = bootstrap.cluster

        # now figure out which onboard NIC we found corresponds to which NIC in
        # metadata server
        
        # start by getting NIC information from the metadata server
        
        geturl = metadataServerURL + "meta-data/local-hostname"
        localHostname = self.get_stdout(geturl)
        if localHostname != None and localHostname.strip() == "":
            localHostname = None
        
        geturl = metadataServerURL + "meta-data/local-ipv4"
        localIP = self.get_stdout(geturl)
        if localIP != None and localIP.strip() == "":
            localIP = None
            
        geturl = metadataServerURL + "meta-data/public-hostname"
        publicHostname = self.get_stdout(geturl)
        if publicHostname != None and publicHostname.strip() == "":
            publicHostname = None
            
        geturl = metadataServerURL + "meta-data/public-ipv4"
        publicIP = self.get_stdout(geturl)
        if publicIP != None and publicIP.strip() == "":
            publicIP = None
        
        if localIP == None and publicIP == None:
            raise UnexpectedError("could not obtain any network information from metadata server @ '%s'" % metadataServerURL)
            
        if localIP:
            localIP = localIP.strip()
        if publicIP:
            publicIP = publicIP.strip()
        if localHostname:
            localHostname = localHostname.strip()
        if publicHostname:
            publicHostname = publicHostname.strip()
            
        # only one NIC found on system:
        if self.result.iface_ip and not self.result.iface2_ip:
            
            if self.result.iface_ip not in (localIP, publicIP):
                msg = "Found one local NIC IP ('%s')" % self.result.iface_ip
                msg += " but it is not described in the metadata server."
                raise UnexpectedError(msg)
                
            if self.result.iface_ip == localIP:
                self.result.iface_hostname = localHostname
                self.result.iface_name = "localnic"
            elif self.result.iface_ip == publicIP:
                self.result.iface_hostname = publicHostname
                self.result.iface_name = "publicnic"
            self.result.iface_short_hostname = self.result.iface_hostname.split(".")[0]
            
        # two
        if self.result.iface_ip and self.result.iface2_ip:
            # Real IPs
            rip1 = self.result.iface_ip
            rip2 = self.result.iface2_ip
            
            # Bootstrap IPs
            bip1 = localIP
            bip2 = publicIP
            
            self.log.info("Local IPs: #1:'%s' and #2:'%s'" % (rip1, rip2))
            self.log.info("Bootstrap IPs: #1:'%s' and #2:'%s'" % (bip1, bip2))
            
            matched = False
            flipped = False
            
            if rip1 == bip1:
                self.log.debug("1's match")
                if rip2 == bip2:
                    self.log.debug("and 2's match")
                    matched = True
                else:
                    msg = "One real NIC and one metadata server NIC match, but the "
                    msg += "other doesn't?  '%s' equals '%s' " % (rip1, bip1)
                    msg += "but the local IP '%s' that was found " % (rip2)
                    msg += "does not match '%s' in metadata server." % (bip2)
                    raise UnexpectedError(msg)
            
            elif rip1 == bip2:
                self.log.debug("1 flipped IP match")
                if rip2 == bip1:
                    self.log.debug("and other flip matches")
                    flipped = True
                else:
                    msg = "One real NIC and one metadata server NIC match, but the "
                    msg += "other doesn't?  '%s' equals '%s' " % (rip1, bip2)
                    msg += "but the local IP '%s' that was found " % (rip2)
                    msg += "does not match '%s' in metadata server." % (bip1)
                    raise UnexpectedError(msg)
                    
            elif rip2 == bip1:
                msg = "One real NIC and one metadata server NIC match, but the "
                msg += "other doesn't?  '%s' equals '%s' " % (rip2, bip1)
                msg += "but the local IP '%s' that was found " % (rip1)
                msg += "does not match '%s' in metadata server." % (bip2)
                raise UnexpectedError(msg)
                
            elif rip2 == bip2:
                msg = "One real NIC and one metadata server NIC match, but the "
                msg += "other doesn't?  '%s' equals '%s' " % (rip2, bip2)
                msg += "but the local IP '%s' that was found " % (rip1)
                msg += "does not match '%s' in metadata server." % (bip1)
                raise UnexpectedError(msg)
                
            else:
                msg = "Neither local NIC that was found matches to the "
                msg += "available IPs in the metadata server."
                raise UnexpectedError(msg)
                
                
            nicname1 = "localnic"
            nicname2 = "publicnic"
                
            host1 = localHostname
            host2 = publicHostname
            
            shorthost1 = host1.split(".")[0]
            shorthost2 = host2.split(".")[0]
                
            if matched:
                
                self.result.iface_name = nicname1
                self.result.iface_hostname = host1
                self.result.iface_short_hostname = shorthost1
                
                self.result.iface2_name = nicname2
                self.result.iface2_hostname = host2
                self.result.iface2_short_hostname = shorthost2
                
            elif flipped:
                
                self.result.iface_name = nicname2
                self.result.iface_hostname = host2
                self.result.iface_short_hostname = shorthost2
                
                self.result.iface2_name = nicname1
                self.result.iface2_hostname = host1
                self.result.iface2_short_hostname = shorthost1
                
            else:
                raise ProgrammingError("either matched or flipped here only")
                
        
# }}} END: RegularInstantiation(Action)

# ############################################################
# AmazonInstantiation(Action)
# #########################################################{{{

class AmazonInstantiation(Action):

    """Class implementing bootstrap retrieval from EC2.  It also populates
       the identity and sshd pubkey values."""
    
    def __init__(self, commonconf, ec2conf, log_override=None):
        """Instantiate object with configurations necessary to operate.

        Required parameters:

        * commonconf -- CommonConf instance

        * ec2conf -- AmazonConf instance

        Raise InvalidConfig if problem with the supplied configurations.
        
        Sets its result field to IdentityAndCredentialResult instance.

        """
        
        Action.__init__(self, commonconf, log_override)
        self.conf = ec2conf
        
    def run(self):
        """Start.

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """
        
        #    self.conf.localhostnameURL
        #    self.conf.publichostnameURL
        #    self.conf.userdataURL
        
        result = InstantiationResult()
        
        # force these names on amazon, 
        # i.e. ctx document given to broker must match
        result.iface_name = "publicnic" # public
        result.iface2_name = "localnic" # private LAN address
        
        r = self.get_stdout(self.conf.publicipURL)
        if r == None:
            raise UnexpectedError("Couldn't obtain pub IP")
        result.iface_ip = r.replace("\n", "").strip()
        
        r = self.get_stdout(self.conf.localipURL)
        if r == None:
            raise UnexpectedError("Couldn't obtain local IP")
        result.iface2_ip = r.replace("\n", "").strip()
         
        r = self.get_stdout(self.conf.publichostnameURL)
        if r == None:
            raise UnexpectedError("Couldn't obtain pub hostname")
        result.iface_hostname = r.replace("\n", "").strip()
        result.iface_short_hostname = result.iface_hostname.split(".")[0]
         
        r = self.get_stdout(self.conf.localhostnameURL)
        if r == None:
            raise UnexpectedError("Couldn't obtain local hostname")
        result.iface2_hostname = r.replace("\n", "").strip()
        result.iface2_short_hostname = result.iface2_hostname.split(".")[0]
        
        # get generated pubkey from file
        result.pub_key = self.consume_sshd_key()
        if result.pub_key == None or result.pub_key == "":
            raise UnexpectedError("Couldn't obtain sshd pubkey")
            
        if self.common.trace:
            self.log.debug("Found sshd key: '%s'" % result.pub_key)
            
        self.result = result
            
        self.consume_bootstrap()
        
    def consume_sshd_key(self):
        
        path = self.common.sshdkeypath
        
        if not os.path.exists(path):
            raise UnexpectedError("'%s' does not exist on filesystem" % path)
        
        # TODO consume properly later
        text = ""
        f = open(path)
        try:
            for line in f:
                text += line
        finally:
            f.close()
        
        # strip any newlines and extra whitespace
        return text.replace("\n", "").strip()
        
    # can raise UnexpectedError
    def consume_bootstrap(self):
        text = self.get_stdout(self.conf.userdataURL)
        if text == None:
            raise UnexpectedError("no user data for bootstrap")
            
        bootstrap = Bootstrap(text)
        
        url = bootstrap.service_url
        if self.common.trace:
            self.log.debug("contextualization service URL = '%s'" % url)
        self.result.ctx_url = url
        
        key = bootstrap.resource_key
        if self.common.trace:
            self.log.debug("contextualization service key = '%s'" % key)
        self.result.ctx_key = key
        
        privPEM = bootstrap.private_key_string
        if self.common.trace:
            self.log.debug("private key PEM = '%s'" % privPEM)
        self.result.ctx_keytext = privPEM
            
        pubPEM = bootstrap.credential_string
        if self.common.trace:
            self.log.debug("public cert PEM = '%s'" % pubPEM)
        self.result.ctx_certtext = pubPEM
        
        self.result.cluster_text = bootstrap.cluster
        
    def get_stdout(self, url):
        # todo: switch to python classes
        
        timeout = 5
        curlcmd = "%s --silent --url %s" % (self.common.curlpath, url)
        
        (exit, stdout, stderr) = runexe(curlcmd, killtime=timeout+1)
        result = "'%s': exit=%s, stdout='%s'," % (curlcmd, exit, stdout)
        result += " stderr='%s'" % (stderr)
        
        if self.common.trace:
            self.log.debug(result)
        
        if exit != "0":
            msg = "PROBLEM: curl command failed, "
            msg += "result: %s" % result
            self.log.error(msg)
            return None
        return stdout
    
# }}} END: AmazonInstantiation(Action)

# ############################################################
# DefaultRetrieveAction(Action)
# #########################################################{{{

class DefaultRetrieveAction(Action):

    """Class implementing getting the contextualization information.
    
       Right now there is only one implementation.
    """
    
    def __init__(self, commonconf, instresult, log_override=None):
        """Instantiate object with configurations necessary to operate.

        Required parameters:

        * commonconf -- CommonConf instance
        
        * instresult -- valid InstantiationResult instance

        Raise InvalidConfig if problem with the supplied configurations.
        
        Sets its result field to RetrieveResult instance.

        """
        
        Action.__init__(self, commonconf, log_override)
        
        if instresult == None:
            raise InvalidConfig("supplied instantiation result is None")
            
        # self.result is reserved for results of actions, not previous results
        self.result = None
        self.instresult = instresult
        
        # todo: could validate instresult here
        
    def run(self):
        """Start.

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """

        self.priv_pem_path = os.path.join(self.common.scratchdir, "ctxkey.pem")
        write_repl_file(self.priv_pem_path, self.instresult.ctx_keytext)
        
        self.pub_pem_path = os.path.join(self.common.scratchdir, "ctxcert.pem")
        write_repl_file(self.pub_pem_path, self.instresult.ctx_certtext)
        
        # stores path to filled in template copy
        self.postdocumentpath = None
        self.okdocumentpath = None
        self.errdocumentpath = None
        
        self.poll_until_done()
        
    def poll_until_done(self):
        
        result = None
        while (not self.analyze_result(result)):
            if self.common.trace:
                self.log.debug("Waiting %d seconds before poll." % self.common.polltime)
            time.sleep(self.common.polltime)
            result = self.retrieve_result()

        self.result = result
        
    def retrieve_result(self):
        
        timeout = 8
        
        responsepath = os.path.join(self.common.scratchdir, "response.xml")
        
        if self.postdocumentpath == None:
            self.filltemplates()
            self.zeroresponsepath(responsepath)
        
            curlpath = self.common.curlpath
            
            curlcmd = "%s --cert %s " % (curlpath, self.pub_pem_path)
            curlcmd += "--key %s " % self.priv_pem_path
            curlcmd += "--max-time %s " % timeout
            
            # The --insecure flag means that we do not validate remote
            # host which would close spoof possibility: for future version.
            curlcmd += " --insecure --random-file /dev/urandom --silent "
            
            # RETR
            curlretr = curlcmd + "--output %s " % responsepath
            curlretr += "--upload-file %s " % self.postdocumentpath
            curlretr += "%s" % self.instresult.ctx_url
            self.runcmd = curlretr
            
            # OK operation, set up for later
            curlok = curlcmd + "--upload-file %s " % self.okdocumentpath
            curlok += "%s" % self.instresult.ctx_url
            set_broker_okaction(DefaultOK(curlok))
            
            # ERR operation, set up for later
            curlerr = curlcmd + "--upload-file %s " % self.errdocumentpath
            curlerr += "%s" % self.instresult.ctx_url
            set_broker_erraction(DefaultERR(curlerr, self.errdocumentpath))
        
        self.log.info("Contacting service (retrieve operation)")
        
        if not self.runcmd:
            raise ProgrammingError("no runcmd setup for retrieve action")
        
        (exit, stdout, stderr) = runexe(self.runcmd, killtime=timeout+1)
        result = "'%s': exit=%s, stdout='%s'," % (self.runcmd, exit, stdout)
        result += " stderr='%s'" % (stderr)
        
        if self.common.trace:
            self.log.debug(result)
        
        if exit != "0":
            msg = "PROBLEM: curl command failed, "
            msg += "result: %s" % result
            self.log.error(msg)
            return None
        
        result = response2_parse_file(responsepath, self.common.trace)
        
        # Case for specific responses that mean there is just no way
        # we can continue (that will raise an UnexpectedError here)
        if result == None:
            response2_parse_for_fatal(responsepath, self.common.trace)
            
        return result
        
    def zeroresponsepath(self, path):
        f = None
        try:
            try:
                # touch the file or replace what was there
                f = open(path, 'w')
                f.close()
                f = None
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                ename = str(exceptname)
                err = str(sys.exc_value)
                errmsg = "Problem zeroing '%s': %s: %s\n" \
                         % (path, ename, err)
                self.log.error(errmsg)
                raise UnexpectedError(errmsg)
        finally:
            if f:
                f.close()
        
    # returns true if locked and complete
    def analyze_result(self, result):
        if result == None:
            return False
            
        if not isinstance(result, RetrieveResult):
            self.log.error("Only handling None or RetrieveResult")
            return False
            
        if result.locked and result.complete:
            return True
        
        return False
            
    def copy_template(self, source, dest):
        try:
            shutil.copyfile(source, dest)
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            ename = str(exceptname)
            err = str(sys.exc_value)
            errmsg = "Problem creating '%s': %s: %s\n" % (dest, ename, err)
            self.log.error(errmsg)
            raise UnexpectedError(errmsg)
            
    def filltemplates(self):
        """The SOAP engine..."""
        
        numnics = 0
        
        if self.instresult.iface_ip:
            numnics += 1
        if self.instresult.iface2_ip:
            numnics += 1
            
        if not numnics:
            raise UnexpectedError("no template is valid for no NICs")
            
        if numnics == 1:
            path = self.common.retr_template
            self.log.debug("Using retrieve template for one nic: '%s'" % path)
            
            okpath = self.common.ok_template
            self.log.debug("Using ok template for one nic: '%s'" % okpath)
            
            errpath = self.common.err_template
            self.log.debug("Using error template for one nic: '%s'" % errpath)
        elif numnics == 2:
            path = self.common.retr_template2
            self.log.debug("Using retrieve template for two nics: '%s'" % path)
            
            okpath = self.common.ok_template2
            self.log.debug("Using ok template for two nics: '%s'" % okpath)
            
            errpath = self.common.err_template2
            self.log.debug("Using error template for two nics: '%s'" % errpath)
        else:
            raise UnexpectedError("no templates are valid for > 2 NICs")
            
        if not os.path.exists(path):
            raise UnexpectedError("template '%s' doesn't exist?" % path)
        if not os.path.exists(okpath):
            raise UnexpectedError("template '%s' doesn't exist?" % okpath)
        if not os.path.exists(errpath):
            raise UnexpectedError("template '%s' doesn't exist?" % errpath)
            
        self.postdocumentpath = \
                        os.path.join(self.common.scratchdir, "retr.xml")
        self.okdocumentpath = \
                        os.path.join(self.common.scratchdir, "ok.xml")
        self.errdocumentpath = \
                        os.path.join(self.common.scratchdir, "err.xml")
                        
        self.copy_template(path, self.postdocumentpath)
        self.copy_template(okpath, self.okdocumentpath)
        self.copy_template(errpath, self.errdocumentpath)
            
        # all messages send at least this subset
        
        tpaths = [ self.postdocumentpath, 
                   self.okdocumentpath, 
                   self.errdocumentpath ]
        
        for i,tpath in enumerate(tpaths):
        
            # assuming template contents are correct.
            text = ""
            f = open(tpath)
            try:
                for line in f:
                    text += line
            finally:
                f.close()
        
            message_id = None
            try:
                message_id = uuidgen()
            except:
                # uuidgen not installed. hack is to use resource key ...
                self.log.error("could not generate uuid for message ID, using "
                          "resource key ...")
                message_id = self.theresult.ctx_key
            
            r = self.instresult
            
            text = text.replace("REPLACE_MESSAGE_ID", message_id)
            text = text.replace("REPLACE_SERVICE_URL", r.ctx_url)
            text = text.replace("REPLACE_RESOURCE_KEY", r.ctx_key)
            
            # iface_name is what the broker knows this as, the real interface
            # name is not relevant to the broker
            text = text.replace("REPLACE_IFACE_NAME", r.iface_name)
            text = text.replace("REPLACE_IFACE_IP", r.iface_ip)
            text = text.replace("REPLACE_IFACE_HOSTNAME", r.iface_hostname)
            text = text.replace("REPLACE_IFACE_SSH_KEY", r.pub_key)
            
            if numnics == 2:
                text = text.replace("REPLACE_IFACE2_NAME", r.iface2_name)
                text = text.replace("REPLACE_IFACE2_IP", r.iface2_ip)
                text = text.replace("REPLACE_IFACE2_HOSTNAME", r.iface2_hostname)
                text = text.replace("REPLACE_IFACE2_SSH_KEY", r.pub_key)
                
            if i == 0:
                text = text.replace("REPLACE_CLUSTER_XML", str(r.cluster_text))
            
            write_repl_file(tpath, text)
        
# }}} END: DefaultRetrieveAction(Action)

# ############################################################
# DefaultConsumeRetrieveResult(Action)
# #########################################################{{{

class DefaultConsumeRetrieveResult(Action):

    """Class implementing consuming the common RetrieveResult object that
       comes from the combination of actions that have been run before.
       
       Calls task scripts to handle each role.
       
       Adjusts SSHd to do host based authorization with nodes in the
       contextualization (hard coded for now, this should probably be a 
       task specific decision).
       
       Does not set its result field, there is no 'result' besides returning
       without exception.
       
       Right now there is only one implementation.
    """
    
    def __init__(self, commonconf, retrresult, instresult, log_override=None):
        """Instantiate object with configurations necessary to operate.

        Required parameters:

        * commonconf -- CommonConf instance
        
        * retrresult -- valid RetrieveResult instance  (todo: name better)
        
        * instresult -- valid InstantiationResult instance
        

        Raise InvalidConfig if problem with the supplied configurations.

        """
        
        Action.__init__(self, commonconf, log_override)
        
        if retrresult == None:
            raise InvalidConfig("supplied retrresult is None")
        if instresult == None:
            raise InvalidConfig("supplied instresult is None")
            
        self.retrresult = retrresult
        self.instresult = instresult
        
    def run(self):
        """Start.

        Return nothing.

        Raise UnexpectedError if it is impossible to proceed.

        Raise IncompatibleEnvironment if it is impossible to proceed.

        """

        self.handle_identities()
        self.handle_opensshd() 
        self.handle_roles()
        self.handle_thishost()
        self.handle_opaquedata()
        self.handle_restarts()
        self.handle_thishost(finalize=True)
        
    # todo: this could be treated as just another role in the future
    # perhaps passing in a parameter that both ip/hostname are required
    # for this "role"
    def handle_identities(self):
        etchostspath = self.common.etchosts_exe
        
        # todo verify exe exists and is executable etc.
        
        for ident in self.retrresult.identities:
            
            # no point in adding to /etc/hosts if ip or host is missing
            if ident.ip == None or ident.host == None:
                msg = "identity missing IP or host, not sending to 'etchosts'"
                self.log.debug(msg)
                continue
            
            
            short_host = ident.host.split(".")[0]
            
            cmd = "%s %s %s %s" % (etchostspath, ident.ip, short_host, ident.host)
            (exit, stdout, stderr) = runexe(cmd, killtime=0)
            result = "'%s': exit=%s, stdout='%s'," % (cmd, exit, stdout)
            result += " stderr='%s'" % (stderr)
            
            if self.common.trace:
                self.log.debug(result)
            
            if exit != "0":
                msg = "PROBLEM: etchosts addition command failed, "
                msg += "result: %s" % result
                raise UnexpectedError(msg)
        
    # assuming openssh for now, todo: turn this into a task script itself
    def handle_opensshd(self):
        
        # server config must have HostbasedAuthentication already turned on
        # recommended to use "IgnoreUserKnownHosts yes" and
        # "IgnoreRhosts yes"
        
        # entries are added to "hostbasedconfig" with this syntax:
        # "$hostname"
        # No "+" wildcarding is supported, accounts on one machine may only
        # freely access the same account name on this machine.  A matching
        # /etc/passwd is implied across the whole contextualization group.
        # To fix a problem with reverse DNS lookups and hostbased authN (HBA),
        # also adding an IP line
        
        # entries are added to "knownhostsconfig" with this syntax:
        # "$hostname,$ip $pubkey"
        
        equivpath = self.common.hostbasedconfig
        knownpath = self.common.knownhostsconfig
        
        self.log.debug("Adjusting equiv policies in '%s'" % equivpath)
        self.log.debug("Adjusting known keys in '%s'" % knownpath)
        
        equivlines = []
        knownlines = []
        for iden in self.retrresult.identities:
            if iden.ip == None or iden.host == None or iden.pubkey == None:
                self.log.debug("Skipping identity with IP '%s' because it does "
                          "not have ip and host and pubkey" % iden.ip)
                continue
            equivlines.append("%s" % iden.host)
            equivlines.append("%s" % iden.ip) # fixes rev dns issues for HBA
            knownlines.append("%s,%s %s" % (iden.host, iden.ip, iden.pubkey))
            knownlines.append("%s,%s %s" % (iden.host.split(".")[0], iden.ip, iden.pubkey))
        
        # replace both files -- right now not supporting client rigged
        # known hosts (they could manually adjust after contextualization
        # for now...)
        
        # bail on any error
        f = None
        try:
            try:
                f = open(equivpath, 'w')
                text = ""
                for line in equivlines:
                    text += line
                    text += "\n"
                text += "\n"
                f.write(text)
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                name = str(exceptname)
                err = str(sys.exc_value)
                errmsg = "Problem writing to '%s': " % equivpath
                errmsg += "%s: %s\n" % (name, err)
                raise UnexpectedError(errmsg)
        finally:
            if f:
                f.close()

        f = None
        try:
            try:
                f = open(knownpath, 'w')
                text = ""
                for line in knownlines:
                    text += line
                    text += "\n"
                text += "\n"
                f.write(text)
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                name = str(exceptname)
                err = str(sys.exc_value)
                errmsg = "Problem writing to '%s': " % knownpath
                errmsg += "%s: %s\n" % (name, err)
                raise UnexpectedError(errmsg)
        finally:
            if f:
                f.close()
        
    def handle_roles(self):
        for role in self.retrresult.roles:
            rolename = role.name
            taskpath = os.path.join(self.common.ipandhostdir, rolename)
            if not os.path.exists(taskpath):
                self.log.info("No ipandhost script for required role %s" % rolename)
                continue
            cmd = "%s %s" % (taskpath, role.ip)
            
            ident = self.locate_identity(role.ip)
            if ident == None:
                # assuming just FOR NOW that this is because IP is all that
                # is needed and there was no all-identities requirement
                # in the contextualization schema.
                self.log.error("No identity located for '%s'" % role.ip)
            elif ident.host != None:
                cmd += " %s %s" % (ident.host.split(".")[0], ident.host)
            
            (exit, stdout, stderr) = runexe(cmd, killtime=0)
            result = "'%s': exit=%s, stdout='%s'," % (cmd, exit, stdout)
            result += " stderr='%s'" % (stderr)
            
            if self.common.trace:
                self.log.debug(result)
            
            if exit != "0":
                msg = "PROBLEM: task command failed, "
                msg += "result: %s" % result
                raise UnexpectedError(msg)
    
    def locate_identity(self, ip):
        for ident in self.retrresult.identities:
            if ip == ident.ip:
                return ident
        return None
        
    def data_file(self, taskdir, scratchdir, name, data):
        """Fill a file in directory 'scratchdir' called name prefix 'name' 
        with contents 'data'.  Opaque data field names can repeat so file
        needs to be unique to this bit of data.  Then call task with the
        name if it exists in taskdir"""
        
        if not name:
            raise UnexpectedError("given data instance has no name?")
        if not data:
            self.log.info("data instance ('%s') has no data (file will be touched)" % name)
        else:
            # ok when only handling text files
            data += "\n"
        
        abspath = None
        p = name + "-"
        try:
            (fd, abspath) = tempfile.mkstemp(prefix=p, dir=scratchdir, text=True)
            self.log.info("Data file created for '%s': %s" % (name, abspath))
            numbytes = 0
            if data:
                numbytes = os.write(fd, data)
                os.fsync(fd)
                self.log.info("Wrote %d bytes to %s" % (numbytes, abspath))
            os.close(fd)
        except:
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            ename = str(exceptname)
            err = str(sys.exc_value)
            errmsg = "Problem with file creation for data instance of "
            errmsg += "'%s':: %s: %s\n" % (name, ename, err)
            raise UnexpectedError(errmsg)
            
        taskpath = os.path.join(taskdir, name)
        cmd = "%s %s" % (taskpath, abspath)
        
        self.log.debug("CMD: %s" % cmd)
        
        (exit, stdout, stderr) = runexe(cmd, killtime=0)
        result = "'%s': exit=%s, stdout='%s'," % (cmd, exit, stdout)
        result += " stderr='%s'" % (stderr)
        
        if self.common.trace:
            self.log.debug(result)
        
        if exit != "0":
            msg = "PROBLEM: data task command failed, "
            msg += "result: %s" % result
            raise UnexpectedError(msg)
        
    def handle_opaquedata(self):
        
        if len(self.retrresult.data) == 0:
            self.log.debug("No opaque data fields")
            return
            
        if not self.common.datadir:
            msg = "Opaque data task directory not configured but data supplied"
            raise UnexpectedError(msg) # abort
            
        if not os.path.exists(self.common.datadir):
            msg = "Opaque data task directory configured but does not exist ('%s')" % (self.common.datadir)
            raise UnexpectedError(msg) # abort
            
        if not os.path.exists(self.common.scratchdir):
            msg = "No scratchdir for data ('%s')" % (self.common.scratchdir)
            raise UnexpectedError(msg) # abort
        
        for one in self.retrresult.data:
            self.data_file(self.common.datadir, self.common.scratchdir, one.name, one.data)
        
    def handle_thishost(self, finalize=False):
        
        if finalize:
            taskdir = self.common.thishostfinalizedir
            self.logname = "thishost-finalize"
        else:
            taskdir = self.common.thishostdir
            self.logname = "thishost"
            
        if taskdir == None:
            self.log.debug("%s directory not configured" % self.logname)
            return
            
        if not os.path.exists(taskdir):
            msg = "%s '%s' directory configured but does not exist" % (self.logname, taskdir)
            raise UnexpectedError(msg)
            
        #    arg1: IP
        #    arg2: short hostname
        #    arg3: FQDN
        
        # list of tuples.
        # (NAME, IP, short hostname, FQDN)
        possible_scripts = []
        
        if self.instresult.iface_name:
            possible_scripts.append( (self.instresult.iface_name,
                                      self.instresult.iface_ip,
                                      self.instresult.iface_short_hostname,
                                      self.instresult.iface_hostname) )
        if self.instresult.iface2_name:
            possible_scripts.append( (self.instresult.iface2_name,
                                      self.instresult.iface2_ip,
                                      self.instresult.iface2_short_hostname,
                                      self.instresult.iface2_hostname) )
        
        if len(possible_scripts) == 0:
            self.log.error("No identities for %s?" % self.logname)
            return
            
        for ident in possible_scripts:
            
            self.log.debug("Trying %s for identity '%s'" % (self.logname, ident[0]))
            
            taskpath = os.path.join(taskdir, ident[0])
            
            if not os.path.exists(taskpath):
                self.log.debug("No %s script for identity '%s'" % (self.logname, ident[0]))
                if self.common.trace:
                    self.log.debug("Does not exist: '%s'" % taskpath)
                    
                # OK if it's absent, just taken to mean nothing to do
                continue
            
            cmd = "%s %s %s %s" % (taskpath, ident[1], ident[2], ident[3])
            
            (exit, stdout, stderr) = runexe(cmd, killtime=0)
            result = "'%s': exit=%s, stdout='%s'," % (cmd, exit, stdout)
            result += " stderr='%s'" % (stderr)
            
            if self.common.trace:
                self.log.debug(result)
            
            if exit != "0":
                msg = "PROBLEM: %s command failed, " % self.logname
                msg += "result: %s" % result
                self.log.error(msg)
                raise UnexpectedError(msg)
            else:
                self.log.info("Successfully ran '%s'" % cmd)
        
    def handle_restarts(self):
        
        seenroles = []
        
        for role in self.retrresult.roles:
            
            # there could have been and often are duplicates, only do it
            # once per role name
            if role.name in seenroles:
                continue
            seenroles.append(role.name)
            
            taskpath = os.path.join(self.common.restartdir, role.name)
            if not os.path.exists(taskpath):
                if self.common.trace:
                    self.log.debug("role '%s' has no restart script configured")
                continue
                
            (exit, stdout, stderr) = runexe(taskpath, killtime=0)
            result = "'%s': exit=%s, stdout='%s'," % (taskpath, exit, stdout)
            result += " stderr='%s'" % (stderr)
            
            if self.common.trace:
                self.log.debug(result)
            
            if exit != "0":
                msg = "PROBLEM: restart task command failed, "
                msg += "result: %s" % result
                raise UnexpectedError(msg)
            else:
                self.log.info("Successfully ran '%s'" % taskpath)
        
# }}} END: DefaultConsumeRetrieveResult(Action)
    

# ############################################################
# InstantiationResult
# #########################################################{{{

class InstantiationResult:

    """Class holding result of bootstrap and identity population.
    
       Only supporting two interfaces right now.
    
    """
    
    def __init__(self):
        self.iface_name = None
        self.iface_ip = None
        self.iface_hostname = None
        self.iface_short_hostname = None
        
        self.iface2_name = None
        self.iface2_ip = None
        self.iface2_hostname = None
        self.iface2_short_hostname = None
        
        # what the onboard VM interface is called.  In some cases this might
        # not even be populated (for example on EC2)
        self.iface_REAL_name = None
        self.iface2_REAL_name = None
        
        # full text of local key
        self.pub_key = None
        
        self.ctx_certtext = None
        self.ctx_keytext = None
        self.ctx_url = None
        self.ctx_key = None
        
        # full cluster xml text for retrieve operation
        self.cluster_text = None
    
# }}} END: InstantiationResult


