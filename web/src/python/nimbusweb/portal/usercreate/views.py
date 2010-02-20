import sys

from django.shortcuts import render_to_response
from django.contrib.auth.decorators import login_required
from django.http import Http404, HttpResponseRedirect
from django.conf import settings

from util import extract_dn, autocreate_cert
from create import create_user
from forms import CertForm, DNForm, AutoCreateForm

USER_CREATE_METHODS = ( 
  ("cert", "Provide User's Cert file", "The DN is extracted from given Cert file, then used to create a new User."),
  ("dn", "Provide User's DN", "The provided DN will be used to create a new User."),
  ("autocreate", "Auto-create a Certificate and User", "A new certicate will be created, then used to create a new User.")
)

@login_required
def index(request):
    return render_to_response('usercreate/index.html', {"createmethods":USER_CREATE_METHODS})

@login_required
def method(request, method):
    if method not in ["cert", "dn", "autocreate"]:
        raise Http404

    dn = None
    if method == "cert":
        methodinfo = USER_CREATE_METHODS[0][1]
        if request.method == "POST":
            form = CertForm(request.POST, request.FILES)
            if form.is_valid():
                certdata = form.cleaned_data["cert"]
                cert = certdata.read()
                dn = extract_dn(cert)
                print "[from 'cert'] dn => ", dn
        else:
            form = CertForm()
            
    if method == "dn":
        methodinfo = USER_CREATE_METHODS[1][1]
        if request.method == "POST":
            form = DNForm(request.POST)
            if form.is_valid():
                dn = form.cleaned_data["DN"]
        else:
            form = DNForm()

    if method == "autocreate":
        methodinfo = USER_CREATE_METHODS[2][1]
        if request.method == "POST":
          #No form data needed, correct?
          form = AutoCreateForm(request.POST)
          if form.is_valid():
              cn = form.cleaned_data["username"] #username is used as the CN (common name)
              try:
                  (dn, cert, key) = autocreate_cert(cn)
              except:
                  raise Exception("Failed autocreating new cert and key.") #TODO: better error.
        else:
            form = AutoCreateForm()

    if dn is not None:
        try:
            username = form.cleaned_data["username"]
            firstname = form.cleaned_data["firstname"]
            lastname = form.cleaned_data["lastname"]
            email = form.cleaned_data["email"]
            (error, new_user, token) = create_user(dn, username, email, firstname, lastname)
            if error:
                raise Exception(error)
            return HttpResponseRedirect("/usercreate/success?token="+token)
        except:
            new_user.delete() #roll back newly create User and UserProfile
            exception_type = sys.exc_type
            try:
                exceptname = exception_type.__name__ 
            except AttributeError:
                exceptname = exception_type
            name = str(exceptname)
            err = str(sys.exc_value)
            errmsg = "Problem creating User: '%s: %s'" % (name, err)
            raise Exception(errmsg)

    return render_to_response('usercreate/method.html', {"form":form, "method":method, "methodinfo":methodinfo})


@login_required
def success(request):
    token = request.GET.get("token")
    baseurl = getattr(settings, "NIMBUS_PRINT_URL", "http://.../nimbus/")
    basepath = getattr(settings, "NIMBUS_PRINT_PATH", "register/token/")
    url = baseurl+basepath
    return render_to_response('usercreate/success.html', {"url":url, "token":token})
