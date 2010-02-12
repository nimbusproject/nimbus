import unittest
from nimbusrest.admin.connection import AdminConnection


class FakeUser(object):
    id = 1
    dn = "test_dn"

class TestNimbusRestClient(unittest.TestCase):
    
    def setUp(self):
        self.user = FakeUser()
        self.uri = "https://localhost:4443/admin"
        self.key = "testadmin"
        self.secret = "secret"
        self.conn = AdminConnection(self.uri, self.key, self.secret)

    def test_add_user_has_key_secret(self):
        resp = self.conn.add_user(self.user) 
        self.assertEquals(resp.dn, "test_dn")
        user_id = resp.user_id
        access_key = self.conn.generate_user_access_key(user_id)
        self.assertTrue(hasattr(access_key, "secret"))
        self.assertEquals(access_key.key, user_id)


if __name__ == '__main__':
    unittest.main()
