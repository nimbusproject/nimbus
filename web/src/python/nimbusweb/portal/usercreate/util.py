from django.conf import settings
from nimbusweb.setup.ezpz_ca import EzPzCA

def extract_dn(cert):
    ezpz = EzPzCA(settings.NIMBUS_CADIR, settings.WEBDIR)
    DN = ezpz.get_cert_dn(cert)
    return DN

def autocreate_cert(cn):
    """Create a cert using local CA functionality.

    The 'cn' (common name) is the 'username' of the new User.
    """
    ezpz = EzPzCA(settings.NIMBUS_CADIR, settings.WEBDIR)
    (DN, cert, key) = ezpz.create_cert(cn)
    return (DN, cert, key)
