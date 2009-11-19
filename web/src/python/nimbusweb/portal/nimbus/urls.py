from django.conf.urls.defaults import *
from django.contrib.auth.views import login, logout, password_change, password_change_done

urlpatterns = patterns('nimbusweb.portal.nimbus.views',
    (r'^$', 'index'),
    (r'^profile/$', 'profile'),
    (r'^profile/usercert.pem$', 'download_cert'),
    (r'^profile/userkey.pem$', 'download_key'),
    (r'^admin/$', 'admin'),
    (r'^accounts/$', 'accounts'),
    (r'^register/token/(?P<token>.*)/$', 'register'),
    (r'^accounts/password/$', password_change, {'template_name': 'nimbus/password.html'}),
    (r'^accounts/password/done/$', password_change_done, {'template_name': 'nimbus/password_done.html'}),
    (r'^accounts/login/$',  login, {'template_name': 'nimbus/login.html'}),
    (r'^accounts/logout/$', logout, {'template_name': 'nimbus/logout.html'}),
)
