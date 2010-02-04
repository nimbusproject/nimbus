from django.shortcuts import render_to_response
from django.contrib.auth.decorators import login_required
from django.http import Http404, HttpResponseRedirect

import util
from forms import CertKeyForm, DNForm, AutoCreateForm

USER_CREATE_METHODS = ( 
  ("certkey", "Provide User's Cert and Key files", "The DN will be extracted from upload keys to create a new User."),
  ("dn", "Provide User's DN", "The provided DN will be used to create a new User."),
  ("autocreate", "Auto-create a Certificate and User", "A new certicate will be created, then used to create a new User.")
)

@login_required
def index(request):
    return render_to_response('usercreate/index.html', {"createmethods":USER_CREATE_METHODS})

@login_required
def method(request, method):
    if method not in ["certkey", "dn", "autocreate"]:
        raise Http404

    dn = None
    if method == "certkey":
        methodinfo = USER_CREATE_METHODS[0][1]
        if request.method == "POST":
            # get cert and key
            form = CertKeyForm(request.POST, request.FILES)
            if form.is_valid():
                cert = form.cleaned_data["cert"]
                key = form.cleaned_data["key"]
                print cert, key, type(cert), type(key)
                try:
                    dn = util.extract_dn(cert, key)
                except:
                    #TODO: better error:
                    raise Exception("Failed getting DN from cert and key")
        else:
            form = CertKeyForm()
            
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
              dn = util.autocreate_cert()
        else:
            form = AutoCreateForm()

    if dn is not None:
        try:
            username = form.cleaned_data["username"]
            firstname = form.cleaned_data["firstname"]
            lastname = form.cleaned_data["lastname"]
            email = form.cleaned_data["email"]
            nimbus_userid = util.create_nimbus_user(dn) #if this fails, new User is deleted.
            #TODO save 'nimbus_userid' to UserProfile here, or does util.create_nimbus_user do it?
            print "=== final data ==> ", username, firstname, lastname, email, nimbus_userid
            unique_new_user_token="abc123"
            return HttpResponseRedirect("/usercreate/success?token="+unique_new_user_token)
        except:
            raise Exception("Failed creating Nimbus User")

    return render_to_response('usercreate/method.html', {"form":form, "method":method, "methodinfo":methodinfo})


@login_required
def success(request):
    token = request.GET.get("token")
    url = "http://localhost:1443/register/"#settings.APP_URL 
    return render_to_response('usercreate/success.html', {"url":url, "token":token})
