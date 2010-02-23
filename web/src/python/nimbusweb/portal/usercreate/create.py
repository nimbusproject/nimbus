import os
import sys
import string
from datetime import datetime
from random import Random

from django.contrib.auth.models import User
from django.db import IntegrityError
from django.conf import settings

from dateutil.relativedelta import *
from nimbusrest.admin.connection import AdminConnection


def create_user(dn, cert, key, username, email, firstname, lastname):
    """
    Returns (error_text, new_user, success_token)
    """
    password = _generate_initial_password()
    try:
        user = User.objects.create_user(username, email, password)
    except IntegrityError:
        return ("Username is taken already", user, None)
    user.first_name = firstname
    user.last_name = lastname
    user.save()
        
    user.dn = dn #XXX hack
    nimbus_user = nimbus_user_create_remote(user)
    access_key_obj = nimbus_user.generate_access_key()
    query_id, query_secret =  access_key_obj.key, access_key_obj.secret
    print "[create_user] nimbus_user => ", nimbus_user
    token = _generate_login_key()
    _insert_user_profile_data(user, token=token, cert=cert, key=key, query_id=query_id, query_secret=query_secret)
    return (None, user, token)


def _insert_user_profile_data(user, token=None, cert=None, key=None, query_id=None, query_secret=None):
    """
    'user' is a Django User instance.
    """
    profile = user.get_profile()
    profile.initial_login_key = token
    profile.cert = cert
    profile.certkey = key
    profile.query_id = query_id
    profile.query_secret = query_secret
    now = datetime.now()
    expire_hours = int(settings.NIMBUS_TOKEN_EXPIRE_HOURS)
    profile.login_key_expires = now + relativedelta(hours=+expire_hours)
    profile.save()
    return profile


def nimbus_user_create_remote(user_instance):
    """Use the Nimbus API to register a new Nimbus User.
    
    `user_instance` is a `Django User` instance.
    """
    nimbus_key = getattr(settings, "NIMBUS_KEY", "testadmin")
    nimbus_secret = getattr(settings, "NIMBUS_SECRET", "secret")
    service_uri = getattr(settings, "NIMBUS_SERVICE_URI", "https://localhost:4443/admin")
    conn = AdminConnection(service_uri, nimbus_key, nimbus_secret)
    nimbus_user = conn.add_user(user_instance)
    return nimbus_user


def _generate_initial_password():
    okchars = string.letters + string.digits + "!@%^_&*+-"
    okchars += okchars
    password = ''.join( Random().sample(okchars, 50))
    # double check what we're getting from foreign function
    if len(password) < 50:
        raise Exception("Could not create initial password") #XXX
    return password
    
def _generate_login_key():
    okchars = string.letters + string.digits + "_-"
    okchars += okchars
    token = ''.join(Random().sample(okchars, 80)).replace(" ", "_")
    return token
