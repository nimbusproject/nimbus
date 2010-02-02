import unittest
from django.test import TestCase

from django.contrib.auth.models import User
from django.conf import settings

#from nimbusrest.admin import User as NimbusUser
import remote

class FakeNimbusUser(object):
    user_id = "test_nimbus_userid"

class CreateNimbusUserTest(unittest.TestCase):
    
    def setUp(self):
        self.django_user = User.objects.create_user("test_username", "test@email.com", "test_password")
        self.nimbus_user = FakeNimbusUser() 
        self.fail = False
        self.passthrough = lambda x:x

    def _fake_remote_user_creator(self, user_instance): 
        """Creator real request, fake response and failure states.

        Get necessary data from `user_instance` to form a correct
        request to create a remote Nimbus User.
        """
        if not self.fail:
            return self.nimbus_user
        else:
            raise Exception("fake error in '_fake_remote_user_creator'")

    def test_create_user(self):
        """
        """
        created = remote.nimbus_user_create(self.passthrough, self.django_user, created=True, 
                remote_user_creator=self._fake_remote_user_creator)
        user = User.objects.get(username="test_username")
        self.assertEquals(user.username, "test_username")
        self.django_user.delete()

    def test_create_user_failed(self):
        #test that User rollback is successful.
        self.fail = True
        try:
            created = remote.nimbus_user_create(self.passthrough, self.django_user, created=True, 
                    remote_user_creator=self._fake_remote_user_creator)
        except:
            pass
        user = User.objects.filter(username="test_username")
        #make sure the User does not exist
        self.assertEquals(len(user), 0)

