from django.conf.urls.defaults import *

urlpatterns = patterns('nimbusweb.portal.usercreate.views',
    (r'^$', 'index'),
    (r'^(?P<method>.*)/$', 'method'),
    (r'^success$', 'success'),
)
