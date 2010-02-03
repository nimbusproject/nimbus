from django.shortcuts import render_to_response
from django.contrib.auth.decorators import login_required, user_passes_test
from django.http import HttpResponse, HttpResponseRedirect
from django.core.urlresolvers import reverse

USER_CREATE_METHODS = ( 
  ("certkey", "Provide User's Cert and Key files", "The DN will be extracted from upload keys to create a new User."),
  ("dn", "Provide User's DN", "The provided DN will be used to create a new User."),
  ("autocreate", "Auto-create a Certificate and User", "A new certicate will be created, then used to create a new User.")
)

def index(request):
    return render_to_response('usercreate/index.html', {"createmethods":USER_CREATE_METHODS})
