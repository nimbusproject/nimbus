from django.contrib.auth.models import User
from django.db import IntegrityError
from django.http import HttpResponse, HttpResponseServerError

from random import Random
import os
import string
import sys
from datetime import datetime
from dateutil.relativedelta import *

from forms import NewUserForm
from models import UserProfile, TokenFailure
import nimbusweb.portal.settings as settings

# ------------------------------------------------------------------------------

def admin_post_op(request):
    """
    Deal with op, dispatches on the op value in order to make way for other
    forms on the admin page in the future.
    
    Returns data to send on to html templates
    (flash_msg, ok_flash_msg, newuserform)
    """
    
    operation = request.POST['op']
    if operation == "newuser":
        newuserform = NewUserForm(request.POST, request.FILES)
        if newuserform.is_valid():
            return _newuser(newuserform, request.FILES)
        else:
            return ("Problem creating new user", None, newuserform)
    else:
        return ("Could not find operation '%s'" % operation, None, None)
        
# ------------------------------------------------------------------------------
        
def _generate_initial_password():
    okchars = string.letters + string.digits + "!@%^_&*+-"
    okchars += okchars
    return ''.join( Random().sample(okchars, 50) )
    
def _generate_login_key():
    okchars = string.letters + string.digits + "_+-"
    okchars += okchars
    return ''.join( Random().sample(okchars, 80) )
        
def _newuser(newuserform, request_files):
    """Assumes newuserform input is validated"""
    
    cert = None
    cert_f = None
    key = None
    key_f = None
    try:
        cert_f = request_files['cert']
    except:
        pass
    try:
        key_f = request_files['key']
    except:
        pass
    
    try:
        if cert_f:
            if not cert_f.content_type == "text/plain":
                errmsg = "Certificate file is not of type text/plain?"
                return (errmsg, None, newuserform)
            
            # not reading in chunks, assuming administrator is not trying to
            # DoS the webapp.
            cert = cert_f.read()
        if key_f:
            if not key_f.content_type == "text/plain":
                errmsg = "Key file is not of type text/plain?"
                return (errmsg, None, newuserform)
            
            # not reading in chunks, assuming administrator is not trying to
            # DoS the webapp.
            key = key_f.read()
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__ 
        except AttributeError:
            exceptname = exception_type
        name = str(exceptname)
        err = str(sys.exc_value)
        errmsg = "Problem with file uploads: %s: %s" % (name, err)
        return (errmsg, None, newuserform)
            
    password = _generate_initial_password()
    # double check what we're getting from foreign function
    if len(password) < 50:
        return ("Could not create initial password", None, None)
    
    noerror_flash_msg = "Created new user, login URL: "
    # construct the token-based initial login URL to give to the user
    baseurl = ""
    try:
        baseurl = settings.NIMBUS_PRINT_URL
    except:
        baseurl = "http://.../nimbus/"
    noerror_flash_msg += baseurl + "register/token/"
    
    try:
        
        cd = newuserform.cleaned_data
        
        expire_hours = int(settings.NIMBUS_TOKEN_EXPIRE_HOURS)
        
        try:
            user = User.objects.create_user(cd[NewUserForm.KEYS.username], cd[NewUserForm.KEYS.email], password)
        except IntegrityError:
            return ("Username is taken already", None, newuserform)
            
        user.first_name = cd[NewUserForm.KEYS.firstname]
        user.last_name = cd[NewUserForm.KEYS.lastname]
        user.save()
        
        token = _generate_login_key()
        noerror_flash_msg += token
        
        profile = user.get_profile()
        profile.initial_login_key = token
        profile.cert = cert
        profile.certkey = key
        profile.query_id = cd[NewUserForm.KEYS.query_id]
        profile.query_secret = cd[NewUserForm.KEYS.query_secret]
        
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
        return (errmsg, None, newuserform)
        
    # return None for the newuserform because we are done with those values.
    # Otherwise it would fill in the form on the page with the values that
    # were successfully used which does not make sense, they should only be
    # filled in to help the user re-enter content when there is an error.
    return (None, noerror_flash_msg, None)

   
def ok_token_attempt(ipaddress, maxcount):
    """Return False if this IP has submitted too many tokens"""
    try:
        seen = TokenFailure.objects.get(pk=ipaddress)
    except TokenFailure.DoesNotExist:
        return True
    if seen.count >= maxcount:
        return False
    return True
    
def increment_offender(ipaddress):
    try:
        tf = TokenFailure.objects.get(pk=ipaddress)
        tf.count += 1
    except TokenFailure.DoesNotExist:
        tf = TokenFailure(ip=ipaddress)
        tf.count = 1
    tf.save()
    
def check_token(ipaddress, token):
    """Returns appropriate HTTP response, or None for success"""
    
    ups = UserProfile.objects.filter(initial_login_key__exact=token)
    if len(ups) == 0:
        increment_offender(ipaddress)
        resp = HttpResponse("Invalid token, contact administrator.")
        resp.status_code = 403
        return resp
        
    if len(ups) > 1:
        increment_offender(ipaddress)
        msg = "%d tokens == '%s'?" % (len(ups), token)
        resp = HttpResponse("Invalid token, contact administrator: " % msg)
        resp.status_code = 500
        return resp
    
    up = ups[0]
    
    now = datetime.now()

    if up.login_key_used:
        
        # If we are within 2 minutes of the usage AND registration_complete
        # is False, assume this is an error with password confirmation being
        # wrong, etc.
        if not up.registration_complete:
            cutoff = up.login_key_used + relativedelta(minutes=+2)
            if now < cutoff:
                return None
        
        increment_offender(ipaddress)
        resp = HttpResponse("Token already used.\n<br>\nIf you were not the one that used this token already, contact the administrator *IMMEDIATELY*")
        resp.status_code = 403
        return resp
    
    expires = up.login_key_expires
    if now > expires:
        increment_offender(ipaddress)
        resp = HttpResponse("Expired token, contact administrator.")
        resp.status_code = 403
        return resp

    # This token is accepted.  Mark it used.
    up.login_key_used = now
    up.save()

    return None
    
def token_to_user(token):
    ups = UserProfile.objects.filter(initial_login_key__exact=token)
    if len(ups) == 1:
        return ups[0].user
    else:
        return None

# ------------------------------------------------------------------------------

def datecmp(x, y):
    return x.sortkey < y.sortkey
    
def prettydiff_rdelta(dlt):
    ret = ""
    if dlt.months > 0:
        ret += "%d months" % dlt.months
    if dlt.days > 0:
        if ret:
            ret += ", "
        ret += "%d days" % dlt.days
    if dlt.hours > 0:
        if ret:
            ret += ", "
        ret += "%d hours" % dlt.hours
    if dlt.minutes > 0:
        if ret:
            ret += ", "
        ret += "%d minutes" % dlt.minutes
    if dlt.seconds > 0:
        if ret:
            ret += ", "
        ret += "%d seconds" % dlt.seconds
        
    if ret:
        return ret
    else:
        return "Expired."
    
class PendingUser:
    def __init__(self, user, date, expiresin, sortkey):
        self.user = user
        self.date = date
        self.expiresin = expiresin
        self.sortkey = sortkey
        
def pendingusers():
    # A pending user is all users with no "login_key_used" setting
    # In order to filter out manually created users (such as superusers), we
    # also need to remove anyone without an expiry date.
    
    ups = UserProfile.objects.filter(login_key_used__isnull=True)
    ret = []
    now = datetime.now()
    for up in ups:
        if not up.login_key_expires:
            continue
        date = up.login_key_expires.strftime("%Y-%m-%d %H:%M.%S")
        sortkey = up.login_key_expires
        
        rdelta = relativedelta(sortkey, now)
        expiresin = "%s" % prettydiff_rdelta(rdelta)
        pu = PendingUser(up.user, date, expiresin, sortkey)
        ret.append(pu)
        
    ret.sort(datecmp)
    return ret
    
class RecentUser:
    def __init__(self, user, logindate, activefor, regip, regflag, keydate, keyip, keyflag, sortkey):
        self.user = user
        self.logindate = logindate
        self.activefor = activefor
        self.regip = regip
        self.regflag = regflag
        self.keydate = keydate
        self.keyip = keyip
        self.keyflag = keyflag
        self.sortkey = sortkey
        
def recentusers():
        
    ups = UserProfile.objects.filter(login_key_used__isnull=False)
    ret = []
    now = datetime.now()
    for up in ups:
        logindate = up.login_key_used.strftime("%Y-%m-%d %H:%M.%S")
        sortkey = up.login_key_used
        rdelta = relativedelta(now, sortkey)
        activefor = "%s" % prettydiff_rdelta(rdelta)
        regip = up.registration_ip
        
        # hack:
        if up.user.is_superuser:
            logindate = "(superuser)"
        
        # Used token, but never completed registration entirely?
        regflag = False
        if not up.registration_complete:
            if not up.user.is_superuser:
                regflag = True
                activefor = None
            
        # both can be NULL
        keyip = up.certkey_ip
        keydate = up.certkey_time
        if keydate:
            keydate = keydate.strftime("%Y-%m-%d %H:%M.%S")
        
        # keyflag tells the admin that there is still a key waiting to be
        # downloaded.  This is not always true if there is no keyip or keydate
        # because the user could never have been assigned a key in the first
        # place.
        keyflag = False
        if up.certkey:
            keyflag = True
            
        ru = RecentUser(up.user, logindate, activefor, regip, regflag, keydate, keyip, keyflag, sortkey)
        
        ret.append(ru)
        
    ret.sort(datecmp)
    ret.reverse()
    return ret
    
