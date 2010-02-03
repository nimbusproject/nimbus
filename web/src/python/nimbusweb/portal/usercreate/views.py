from django.shortcuts import render_to_response
from django.contrib.auth.decorators import login_required
from django.http import Http404, HttpResponseRedirect

#from forms import CertKeyForm, DNForm

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

    if method == "certkey":
        if request.method == "POST":
            # get cert and key
            form = CertKeyForm(request.POST)
            if form.is_valid():
                cert = form.cleaned_data["cert"]
                key = form.cleaned_data["key"]
                try:
                    dn = util.extract_dn(cert, key)
                except:
                    raise Exception("Failed getting DN from cert and key")
        else:
            form = CertKeyForm()
            
    if method == "dn":
        if request.method == "POST":
            form = DNForm(request.POST)
            if form.is_valid():
                dn = form.cleaned_data["dn"]
        else:
            form = DNForm()

    if method == "autocreate":
        if request.method == "POST":
          #No form data needed, correct?
          dn = util.autocreate_cert()

    try:
        nimbus_userid = util.create_nimbus_user(dn) #if this fails, new User is deleted.
        #TODO save 'nimbus_userid' to UserProfile here, or does util.create_nimbus_user do it?
    except:
        raise Exception("Failed creating Nimbus User")

    if request.method == "POST":
        #if we get this far, Django and Nimbus User have just been successfully created.
        HttpResponseRedirect("/usercreate/success")
    return render_to_response('usercreate/method.html', {"form":form, "method":method})


@login_required
def success(request):
    return render_to_response('usercreate/success.html')#, {"details":details})
