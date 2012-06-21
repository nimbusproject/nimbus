"""This is used as a programmatic way to remove user credentials and information
from the webapp system"""

import adminops
import nimbusweb.portal.settings as settings
import os
import sys
from django.contrib.auth.models import User
from django.db import IntegrityError
from datetime import datetime
from dateutil.relativedelta import *

from django.contrib.auth.models import User

import nimbusweb.portal.settings as settings

def remove_web_user(username):
    """Remove user and return error_msg or None if success"""
    try:
        user = User.objects.get(username=username)
        user.delete()
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__
        except AttributeError:
            exceptname = exception_type
        name = str(exceptname)
        err = str(sys.exc_value)
        errmsg = "%s: %s" % (name, err)
        return errmsg

    # success
    return None
