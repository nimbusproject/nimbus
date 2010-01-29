# Django settings for nimbusweb.portal project.  Some things are overridden
# by generated_settings.py

import os
import sys

here = lambda x: os.path.join(os.path.abspath(os.path.dirname(__file__)), x)

# ------------------------------------------------------------------------------

ADMINS = ( )
MANAGERS = ADMINS

# Language code for this installation. All choices can be found here:
# http://www.i18nguy.com/unicode/language-identifiers.html
LANGUAGE_CODE = 'en-us'

SITE_ID = 1

# If you set this to False, Django will make some optimizations so as not
# to load the internationalization machinery.
USE_I18N = True

# Absolute path to the directory that holds media.
# Example: "/home/media/media.lawrence.com/"
MEDIA_ROOT = here('static')


# URL that handles the media served from MEDIA_ROOT. Make sure to use a
# trailing slash if there is a path component (optional in other cases).
# Examples: "http://media.lawrence.com", "http://example.com/media/"
MEDIA_URL = '/static/'

# URL prefix for admin media -- CSS, JavaScript and images. Make sure to use a
# trailing slash.
# Examples: "http://foo.com/media/", "/media/".
ADMIN_MEDIA_PREFIX = '/static/admin/'

# List of callables that know how to import templates from various sources.
TEMPLATE_LOADERS = (
    'django.template.loaders.filesystem.load_template_source',
    'django.template.loaders.app_directories.load_template_source',
#     'django.template.loaders.eggs.load_template_source',
)

MIDDLEWARE_CLASSES = (
    'django.middleware.common.CommonMiddleware',
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
)

FILE_UPLOAD_HANDLERS = (
    'django.core.files.uploadhandler.MemoryFileUploadHandler',
)

ROOT_URLCONF = 'nimbusweb.portal.urls'

TEMPLATE_DIRS = (here('templates'))

INSTALLED_APPS = (
    'django.contrib.admin',
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.sites',
    #'cpserver',
    'nimbusweb.portal.nimbus',
)

AUTH_PROFILE_MODULE = 'nimbus.UserProfile'

SESSION_COOKIE_NAME = "nimbus-session"
# The SESSION_COOKIE_SECURE setting is configured in generated_settings.py

LOGIN_URL = "/nimbus/accounts/login/"
LOGOUT_URL = "/nimbus/accounts/logout/"

# ------------------------------------------------------------------------------
# ------------------------------------------------------------------------------

# The following settings are usually overridden via generated_settings.py 
# (it is loaded at the end of this file)

# 'postgresql_psycopg2', 'postgresql', 'mysql', 'sqlite3' or 'oracle'.
DATABASE_ENGINE = ''           
DATABASE_NAME = ''     # DB name, or path to database file if using sqlite3.
DATABASE_USER = ''     # Not used with sqlite3.
DATABASE_PASSWORD = '' # Not used with sqlite3.
DATABASE_HOST = ''     # Set to empty string for localhost. Not used with sqlite3.
DATABASE_PORT = ''     # Set to empty string for default. Not used with sqlite3.

# Local time zone for this installation. Choices can be found here:
# http://en.wikipedia.org/wiki/List_of_tz_zones_by_name
# although not all choices may be available on all operating systems.
# If running in a Windows environment this must be set to the same as your
# system time zone.
TIME_ZONE = 'America/Chicago'

# Make this unique, and don't share it with anybody.
# (actually set in generated_secrets.py)
SECRET_KEY = 'NOT_SET'

DEBUG = True
TEMPLATE_DEBUG = DEBUG

# ------------------------------------------------------------------------------
# ------------------------------------------------------------------------------

# generated_settings.py is created and replaced at will by the 
# "sbin/new-conf.sh" script under the Nimbus web directory.

try:
    from generated_settings import *
except ImportError:
    print >>sys.stderr, "WARN: Could not import generated_settings ?"
    pass

# generated_secrets.py is created and replaced at will by the 
# "sbin/new-conf.sh" script under the Nimbus web directory.  Typically
# created just once per install/upgrade.
try:
    from generated_secrets import *
except ImportError:
    print >>sys.stderr, "WARN: Could not import generated_secrets ?"
    pass
