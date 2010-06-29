from commands import getstatusoutput
import os
import string
from urlparse import urlparse
import urllib
from propagate_adapter import PropagationAdapter
from workspacecontrol.api.exceptions import *

class propadapter(PropagationAdapter):

    def __init__(self, params, common):
        PropagationAdapter.__init__(self, params, common)

    def validate(self):
        self.c.log.debug("validating http propagation adapter")
        # Nothing to validate...

    def validate_propagate_source(self, imagestr):

        url = urlparse(imagestr)
        #urlparse breaks the url into a tuple
        if url[0] != "http" and url[0] != "https":
            raise InvalidInput("invalid http(s) url, not http:// or https:// " + remote)

    def validate_unpropagate_target(self, imagestr):
        raise InvalidInput("HTTP unpropagation is not supported.")

    def propagate(self, remote_source, local_absolute_target):
        self.c.log.info("HTTP propagation - remote source: %s" % remote_source)
        self.c.log.info("HTTP propagation - local target: %s" % local_absolute_target)

        try:
            remote_data = urllib.urlretrieve(remote_source, local_absolute_target)
        #TODO: catch possible exceptions
        #except urllib2.HTTPError, e:
            #errmsg = "HTTP propagation - %s error. Propagation failed" % e.code
            #self.c.log.error(errmsg)
            #raise UnexpectedError(errmsg)
        #except urllib2.URLError, e:
            #errmsg = "HTTP propagation - %s error. Propagation failed" % e.reason
            #self.c.log.error(errmsg)
            #raise UnexpectedError(errmsg)
        except:
            errmsg = "HTTP propagation - unknown error. Propagation failed"
            self.c.log.error(errmsg)
            raise UnexpectedError(errmsg)

        self.c.log.info("Transfer complete.")
    
    def unpropagate(self, local_absolute_source, remote_target):
        raise InvalidInput("HTTP unpropagation is not supported.")
