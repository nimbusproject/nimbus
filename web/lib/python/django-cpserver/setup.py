from distutils.core import setup
 
setup(
    name="django_cpserver",
    version="0.1",
    description="Management commands for serving Django via CherryPy's built-in WSGI server",
    author="Peter Baumgartner",
    author_email="pete@lincolnloop.com",
    url="http://lincolnloop.com/blog/2008/mar/25/serving-django-cherrypy/",
    packages=[
        "django_cpserver",
        "django_cpserver.management",
        "django_cpserver.management.commands",
    ],
)