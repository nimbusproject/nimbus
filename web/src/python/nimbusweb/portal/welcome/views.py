from django.shortcuts import render_to_response
from django.http import HttpResponseRedirect
from django.core.urlresolvers import reverse

def index(request):
    return HttpResponseRedirect(reverse('nimbusweb.portal.nimbus.views.index'))
