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

    def test_add_user(self):
        resp = self.conn.add_user(self.user) 
        self.assertEquals(resp.dn, "test_dn")


if __name__ == '__main__':
    unittest.main()
