from commands import getstatusoutput
import os
import string
from urlparse import urlparse
import urllib
import urllib2
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
            raise InvalidInput("invalid url, not http:// or https:// " + remote)

    def validate_unpropagate_target(self, imagestr):
        raise InvalidInput("HTTP unpropagation is not supported.")

    def propagate(self, remote_source, local_absolute_target):
        self.c.log.info("HTTP propagation - remote source: %s" % remote_source)
        self.c.log.info("HTTP propagation - local target: %s" % local_absolute_target)

        try:
            remote_data = SafeURLopener().retrieve(remote_source, local_absolute_target)
        except UnexpectedError, e:
            errmsg = "HTTP propagation - %s" % e
            self.c.log.error(errmsg)
            raise

        self.c.log.info("Transfer complete.")
    
    def unpropagate(self, local_absolute_source, remote_target):
        raise InvalidInput("HTTP unpropagation is not supported.")


class SafeURLopener(urllib.FancyURLopener):
  def http_error_default(self, url, fp, errcode, errmsg, headers):
    errmsg = "HTTP error " + errmsg
    raise UnexpectedError(errmsg)
