from django.conf.urls.defaults import *
from django.contrib import admin
from django.views.static import serve

import settings
import welcome

admin.autodiscover()

urlpatterns = patterns('',
    (r'^$', "nimbusweb.portal.welcome.views.index"),
    (r'^admin/', include(admin.site.urls)),
    (r'^nimbus/', include("nimbusweb.portal.nimbus.urls")),
    #(r'^usercreate/', include("nimbusweb.portal.usercreate.urls")),
)

_media_url = settings.MEDIA_URL
if _media_url.startswith('/'):
    _media_url = _media_url[1:]
urlpatterns += patterns('',
                        (r'^%s(?P<path>.*)$' % _media_url,
                        serve,
                        {'document_root': settings.MEDIA_ROOT}))
del(_media_url, serve)
