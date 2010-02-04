
def create_nimbus_user_stub(dn):
    nimbus_userid = "test_nimbus_userid"
    return nimbus_userid
create_nimbus_user = create_nimbus_user_stub

def extract_dn_stub(cert, key):
    new_users_dn = "test_dn"
    return new_users_dn
extract_dn = extract_dn_stub

def autocreate_cert_stub():
    new_users_dn = "test_dn"
    return new_users_dn
autocreate_cert = autocreate_cert_stub
    
