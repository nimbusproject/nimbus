import os
import tempfile

from pynimbusconfig import autoca

class EzPzCA(object):
    """
    Exposes CA functionality by wrapping java ezpz CA
    """

    def __init__(self, cadir, webdir, tmpdir=None, log=None):
        self.cadir = cadir
        self.webdir = webdir
        self.tmpdir = tmpdir
        
        if log:
            self.log = log
        else:
            import logging
            self.log = logging

    def create_cert(self, cn):
        """
        Creates a new certificate with the specified CN.

        Returns a tuple (DN, cert, key)
        """

        (cert_fd, cert_path) = tempfile.mkstemp(dir=self.tmpdir)
        (key_fd, key_path) = tempfile.mkstemp(dir=self.tmpdir)

        cert_file = os.fdopen(cert_fd)
        key_file = os.fdopen(key_fd)

        try:
            dn = autoca.createCert(cn, self.webdir, self.cadir, cert_path, 
                    key_path, self.log, allow_overwrite=True)

            cert = cert_file.read()
            key = key_file.read()

            return (dn, cert, key)

        finally:
            # best-effort cleanup

            cert_file.close()
            key_file.close()
            os.remove(cert_path)
            os.remove(key_path)

    def get_cert_dn(self, cert):
        """
        Determines the DN of a provided certificate.
        """

        (cert_fd, cert_path) = tempfile.mkstemp(dir=self.tmpdir)

        try:
            cert_file = os.fdopen(cert_fd, 'wb')
            cert_file.write(cert)
            cert_file.close() # make sure write is buffered out

            return autoca.getCertDN(cert_path, self.webdir, self.log)

        finally:
            try:
                os.close(cert_fd)
            except:
                pass # FD may have been never opened-- or closed above

            os.remove(cert_path)
