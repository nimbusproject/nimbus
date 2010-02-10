from django.conf import settings
from nimbusweb.setup.ezpz_ca import EzPzCA

def create_nimbus_user_stub(dn):
    nimbus_userid = "test_nimbus_userid - dn =>", dn
    return nimbus_userid
create_nimbus_user = create_nimbus_user_stub

def extract_dn_stub(cert, key):
    new_users_dn = "test_dn"
    return new_users_dn
extract_dn = extract_dn_stub

def autocreate_cert_stub(cn):
    new_users_dn = "test_dn"
    return new_users_dn
autocreate_cert = autocreate_cert_stub

def autocreate_cert(cn):
    """Create a cert using local CA functionality.

    The 'cn' (common name) is the 'username' of the new User.
    """
    ezpz = EzPzCA(settings.NIMBUS_CADIR, settings.WEBDIR)
    (DN, cert, key) = ezpz.create_cert(cn)
    return (DN, cert, key)
