import sys
from django.db import models
from django.contrib.auth.models import User, UserManager
import remote

class TokenFailure(models.Model):
    ip = models.IPAddressField(primary_key=True)
    count = models.IntegerField()

# ------------- User account, extra information --------------

class UserProfile(models.Model):
    """Extension to properties in django's user model, we hang things off the user record."""
    
    # Required field
    user = models.ForeignKey(User, unique=True, related_name="%(class)s_related")

    # The rest is up to us:
    initial_login_key = models.CharField(max_length=100)
    login_key_expires = models.DateTimeField(auto_now=False, null=True)
    login_key_used = models.DateTimeField(auto_now=False, null=True)
    registration_complete = models.BooleanField()
    registration_ip = models.IPAddressField(null=True)
    cert = models.TextField(null=True)
    certkey = models.TextField(null=True)
    certkey_ip = models.IPAddressField(null=True)
    certkey_time = models.DateTimeField(auto_now=False, null=True)
    query_id = models.TextField(null=True)
    query_secret = models.TextField(null=True)
    nimbus_userid = models.TextField(null=True)
    
if sys.argv.count("test") != 1: #are we running tests? better way?
    models.signals.post_save.connect(remote.nimbus_user_create, User)
