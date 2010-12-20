from commands import getstatusoutput
import os
import string
from urlparse import urlparse
import httplib
import shutil
from propagate_adapter import PropagationAdapter
from workspacecontrol.api.exceptions import *
import workspacecontrol.main.wc_args as wc_args

class propadapter(PropagationAdapter):

    def __init__(self, params, common):
        PropagationAdapter.__init__(self, params, common)

    def validate(self):
        self.c.log.debug("validating https propagation adapter")
        self._get_credential()
        # Nothing to validate...

    def validate_propagate_source(self, imagestr):

        url = urlparse(imagestr)
        #urlparse breaks the url into a tuple
        if url[0] != "https":
            raise InvalidInput("invalid url, not https:// " + remote)

    def validate_unpropagate_target(self, imagestr):
        raise InvalidInput("HTTPS unpropagation is not supported.")

    def propagate(self, remote_source, local_absolute_target):
        self.c.log.info("HTTPS propagation - remote source: %s" % remote_source)
        self.c.log.info("HTTPS propagation - local target: %s" % local_absolute_target)

        url = urlparse(remote_source)
        scheme = url[0]
        netloc = url[1]
        path = url[2] + "?" + url[4]
        host_port = netloc.split(":")
        host = host_port[0]
        try:
            port = host_port[1]
        except IndexError:
            if scheme == 'http':
                port = 80
            else:
                port = 443
        credential = self._get_credential()
        self.c.log.debug("server: %s port %s credential %s" % (host, port, credential))
        if credential:
            connection = httplib.HTTPSConnection(host, port, strict=False, key_file=credential, cert_file=credential)
        else:
            connection = httplib.HTTPSConnection(host, port)

        try:
            response = self._get_handle_redirects(connection, path)
        except:
            errmsg = "HTTP propagation - Couldn't get image"
            self.c.log.error(errmsg)
            raise
        else:
            if response.status != 200:
                errmsg = "HTTP propagation: Got status %s from web server. Can't download image" % response.status
            else:
                try:
                    shutil.copyfileobj(response, open(local_absolute_target, 'w'))
                except:
                    self.c.log.exception("Couldn't save image file to %s." % local_absolute_target)
                    raise

        self.c.log.info("Transfer complete.")

    def unpropagate(self, local_absolute_source, remote_target):
        raise InvalidInput("HTTP unpropagation is not supported.")

    def _get_credential(self):
        extra_args = self.p.get_arg_or_none(wc_args.EXTRA_ARGS)
        if extra_args == None:
            return None

        # unpack extra-args in format arg=value;arg=value;...;arg=value
        credential_name = None
        for extra_arg in extra_args.split(";"):
            try:
                parts = extra_arg.split("=")
                if parts[0] == "credential":
                    credential_name = parts[1]
            except:
                continue

        if not credential_name:
            return None

        tmpdir = self.p.get_conf_or_none("mount", "tmpdir")
        tmpdir = self.c.resolve_var_dir(tmpdir)
        credential = tmpdir + "/" + credential_name

        # If the file is readable, we assume it's good. Otherwise we'll fail on propagate
        if not (os.path.exists(credential) and os.access(credential, os.R_OK)):
            raise InvalidInput("Cannot read credential '%s'" % credential)

        return credential

    def _get_handle_redirects(self, connection, path):
        """
        _handle_redirects -- takes an httplib.connection object, follows
        redirects if there are any, then returns a new connection object

        """
        connection.request("GET", path)
        response = connection.getresponse()
        status_class = response.status / 100

        # all 3xx return codes are redirects
        if status_class != 3:
            return response
        else:
            try:
                redirect_url = response.getheader("Location")
            except:
                errmsg = "Got a redirect, but couldn't follow redirect from: %s" % response.msg
                self.c.log.exception(errmsg)
                raise UnexpectedError(errmsg)

            self.c.log.info("Redirected to %s" % redirect_url)

            # parse new URL
            url = urlparse(redirect_url)
            netloc = url[1]
            host_port = netloc.split(":")
            host = host_port[0]
            redirect_path = url[2] + "?" + url[4]

            if host != connection.host:
                errmsg = "Cannot follow cross-server redirect from %s to %s" % (connection.host, host)
                self.c.log.error(errmsg)
                raise UnexpectedError(errmsg)

            return self._get_handle_redirects(connection, redirect_path)
