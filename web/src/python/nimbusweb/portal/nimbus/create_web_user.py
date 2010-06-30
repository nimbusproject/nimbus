"""This is used as a programmatic way to add user credentials and information
into the webapp system"""

import adminops
import nimbusweb.portal.settings as settings
import os
import sys
from django.contrib.auth.models import User
from django.db import IntegrityError
from datetime import datetime
from dateutil.relativedelta import *

def create_web_user(username, email, cert_file, key_file, query_id, query_secret):
    """Create user and return tuple: (error_msg, url)
    
    Quick and dirty translation method, couldn't use adminops directly.
    
    * username -- username, required
    
    * email -- email address, required
    
    * cert_file -- X509 certificate file on local FS; optional
    
    * key_file -- key for X509 certificate, file on local FS, can
    not have password protection; optional
    
    * query_id -- query ID, optional 
    
    * query_secret -- query secret, optional
    
    """
    
    cert_content = None
    key_content = None
    try:
        if cert_file:
            if not os.path.exists(cert_file):
                errmsg = "Certificate file can not be found: %s" % cert_file
                return (errmsg, None)
            f = open(cert_file, "r")
            cert_content = f.read()
            f.close()
        if key_file:
            if not os.path.exists(key_file):
                errmsg = "Key file can not be found: %s" % key_file
                return (errmsg, None)
            f = open(key_file, "r")
            key_content = f.read()
            f.close()
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        name = str(exceptname)
        err = str(sys.exc_value)
        errmsg = "Problem with certificate intake: %s: %s" % (name, err)
        return (errmsg, None)
    
    password = adminops._generate_initial_password()
    # double check what we're getting from foreign function
    if len(password) < 50:
        return ("Could not create initial password", None)
        
    try:
        baseurl = settings.NIMBUS_PRINT_URL
    except:
        baseurl = "http://.../nimbus/"
        
    # token gets added to this below to make the full url
    url = baseurl + "register/token/"
    
    try:
        
        expire_hours = int(settings.NIMBUS_TOKEN_EXPIRE_HOURS)
        
        try:
            user = User.objects.create_user(username, email, password)
        except IntegrityError:
            return ("Username is taken already", None)
            
        user.first_name = ""
        user.last_name = ""
        user.save()
        
        token = adminops._generate_login_key()
        url += token
        
        profile = user.get_profile()
        profile.initial_login_key = token
        profile.cert = cert_content
        profile.certkey = key_content
        profile.query_id = query_id
        profile.query_secret = query_secret
        
        now = datetime.now()
        profile.login_key_expires = now + relativedelta(hours=+expire_hours)
        
        profile.save()
        
    except:
        # TODO: should probably back user out here, if created
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        name = str(exceptname)
        err = str(sys.exc_value)
        errmsg = "Unknown problem creating username: %s: %s" % (name, err)
        return (errmsg, None)
        
    # success
    return (None, url)
