from django.shortcuts import render_to_response
import nimbusweb.portal.settings as settings
from django.contrib.auth.decorators import login_required, user_passes_test
from django.http import HttpResponse, HttpResponseRedirect
from django.core.urlresolvers import reverse
from datetime import datetime
import adminops
import forms

# -----------------------------------------------------------------------------
# User views
# -----------------------------------------------------------------------------

def index(request):
    if request.user.is_authenticated():
        return HttpResponseRedirect(reverse('nimbusweb.portal.nimbus.views.profile'))
    message = "Welcome!"
    accountprompt = settings.NIMBUS_ACCOUNT_PROMPT
    return render_to_response('nimbus/index.html', 
                              {'message': message, 
                               'accountprompt':accountprompt})

def accounts(request):
    if request.user.is_authenticated():
        return HttpResponseRedirect(reverse('nimbusweb.portal.nimbus.views.profile'))
    else:
        return HttpResponseRedirect(reverse('nimbusweb.portal.nimbus.views.index'))
        
        
def register(request, token):
    if request.user.is_authenticated():
        # This does not make sense if they are already logged in
        return HttpResponseRedirect(reverse('nimbusweb.portal.nimbus.views.profile'))
        
    if not token or not token.strip():
        return HttpResponse("Token missing.")
        
    # Outright deny an IP address that has tried and failed with 10 tokens
    ip = request.META['REMOTE_ADDR']
    if not adminops.ok_token_attempt(ip, 10):
        resp = HttpResponse("Your IP has been banned.")
        resp.status_code = 403
        return resp
        
    resp = adminops.check_token(ip, token)
    if resp:
        return resp
        
    user = adminops.token_to_user(token)
    if not user:
        resp = HttpResponse("Cannot locate account.")
        resp.status_code = 403
        return resp
    
    new_password1 = None
    new_password2 = None
    if "new_password1" in request.POST:
        new_password1 = request.POST['new_password1']
    if "new_password2" in request.POST:
        new_password2 = request.POST['new_password2']
    
    if new_password1 and new_password2:
        new_password1 = new_password1.strip()
        new_password2 = new_password2.strip()
        if new_password1 != new_password2:
            flash_msg = "Passwords don't match."
            return render_to_response('nimbus/register.html', 
                              {'flash_msg': flash_msg,
                               'user': user})
        else:
            if len(new_password2) < 12:
                flash_msg = "Password is not 12 characters or more."
                return render_to_response('nimbus/register.html', 
                                          {'flash_msg': flash_msg,
                                           'user': user})
            
            user.set_password(new_password2)
            user.save()
            
            up = user.get_profile()
            up.registration_complete = True
            up.registration_ip = ip
            up.save()
            
            return HttpResponseRedirect(reverse('nimbusweb.portal.nimbus.views.index'))
        
    return render_to_response('nimbus/register.html', 
                              {'user': user})
    

@login_required
def profile(request):
    user = request.user
    message = "Hello"
    if user.first_name:
        message += ", %s" % user.first_name
    message += "!"
    
    cert_present = False
    key_present = False
    query_present = False
    props_present = False

    up = user.get_profile()
    if up.cert:
        cert_present = True
    if up.certkey:
        key_present = True
    if up.query_id and up.query_secret:
        query_present = True
    if up.cloudprop_file:
        props_present = True
    
    templateparams = {'message': message, 
                      'user': user,
                      'cert_present': cert_present,
                      'key_present': key_present,
                      'access_present': query_present,
                      'props_present': props_present,
                      'access_key': up.query_id,
                      'access_secret': up.query_secret
                     }
    return render_to_response('nimbus/profile.html', templateparams)

@login_required
def download_cert(request):
    up = request.user.get_profile()
    if not up.cert:
        resp = HttpResponse("Your have no certificate on file.")
        resp.status_code = 404
        return resp
    resp = HttpResponse(up.cert, mimetype="application/octet-stream")
    return resp

@login_required
def download_propsfile(request):
    up = request.user.get_profile()
    if not up.cert:
        resp = HttpResponse("Your have no cloud.properties on file.")
        resp.status_code = 404
        return resp
    resp = HttpResponse(up.cloudprop_file, mimetype="application/octet-stream")
    return resp
    
@login_required
def download_key(request):
    up = request.user.get_profile()
    if not up.certkey:
        # The theory here is that they would not know about this page unless
        # they tried once, failed to download, and were trying again.
        # People trying the URL without a referrer might not have ever had
        # a key, oh well.
        resp = HttpResponse("404<br><br><p>You have no key on file.</p><p>It can only be retrieved once.</p><p>If you did not obtain the key, contact your administrator immediately.</p>")
        resp.status_code = 404
        return resp
    
    ip = request.META['REMOTE_ADDR']
    
    resp = HttpResponse(up.certkey, mimetype="application/octet-stream")
    up.certkey = None
    up.certkey_ip = ip
    up.certkey_time = datetime.now()
    up.save()
    return resp

# -----------------------------------------------------------------------------
# Superuser views
# -----------------------------------------------------------------------------

@login_required
def admin(request):
    if not request.user.is_superuser:
        return HttpResponseRedirect(reverse('nimbusweb.portal.nimbus.views.profile'))
    
    flash_msg = None
    ok_flash_msg = None
    newuserform = None
    
    # check if this was a POST to change anything
    if 'op' in request.POST:
        (flash_msg, ok_flash_msg, newuserform) = adminops.admin_post_op(request)
    
    if not newuserform:
        newuserform = forms.NewUserForm()
        
    # bypasses 'pending user' inclusion for superusers (superusers are created
    # out of band)
    up = request.user.get_profile()
    if not up.login_key_used:
        up.login_key_used = datetime.now()
        up.save()
    
    pendingusers = adminops.pendingusers()
    
    recentusers = adminops.recentusers()
    
    templateparams = {'pendingusers': pendingusers,
                      'recentusers': recentusers,
                      'newuserform': newuserform,
                      'flash_msg': flash_msg,
                      'ok_flash_msg': ok_flash_msg,
                      }
    
    return render_to_response('nimbus/admin.html', templateparams)
