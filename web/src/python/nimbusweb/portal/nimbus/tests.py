import unittest
from django.test import TestCase

from django.contrib.auth.models import User

import remote

class FakeUser(object):
    username = "test_username"
    email = "test@example.com" 
    password = "test_password"

class CreateNimbusUserTest(unittest.TestCase):
    
    def setUp(self):
        #self.user = User.objects.create_user("test_username", "test@email.com", "test_password")
        #creating a real user kicks off a real 'models.signals.post_save.connect'! So do this:
        self.user = FakeUser() 
        self.ok_resp = {"nimbus_userid":"abc123"}  #, "state":"created"}

    def _fake_remote_user_creator(self, user_instance, fail=False): 
        """Creator real request, fake response and failure states.

        Get necessary data from `user_instance` to form a correct
        request to create a remote Nimbus User.
        """
        if not fail:
            return self.ok_resp
        else:
            raise Exception("fake error in '_fake_remote_user_creator'")

    def test_create_user(self):
        """
        """
        passthrough = lambda x:x
        created = remote.nimbus_user_create(passthrough, self.user, created=True, 
                remote_user_creator=self._fake_remote_user_creator)
        self.assertEquals(created, True)

    def test_create_user_failed(self):
        #test that User rollback is successful.
        pass

