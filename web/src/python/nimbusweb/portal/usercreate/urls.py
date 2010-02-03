from django.conf.urls.defaults import *
from django.contrib.auth.views import login, logout, password_change, password_change_done

urlpatterns = patterns('nimbusweb.portal.usercreate.views',
    (r'^$', 'index'),
)
