#!/usr/bin/env python

from setuptools import setup, find_packages
import sys, os
from pkg_resources import Requirement, resource_filename
import pkg_resources

if float("%d.%d" % sys.version_info[:2]) < 2.5:
    sys.stderr.write("Your Python version %d.%d.%d is not supported.\n" % sys.version_info[:3])
    sys.stderr.write("cumulus requires Python 2.5 or newer.\n")
    sys.exit(1)

setup(name='cumulus',
      version='0.1',
      description='An Open Source S3 REST API look-a-like.',
      author='John Bresnahan',
      author_email='bresnaha@mcs.anl.gov',
      url='http://www.nimbusproject.org/',
      packages=find_packages(),
       entry_points = {
        'console_scripts': [
            'cumulus = pycb.cumulus:main', 
            'cumulus-add-user = pycb.tools.add_user:main',
            'cumulus-remove-user = pycb.tools.remove_user:main',
            'cumulus-list-users = pycb.tools.list_users:main',
            'cumulus-quota = pycb.tools.set_quota:main',
            'nimbusauthz-add-user = pynimbusauthz.add_user:main',
            'nimbusauthz-list-users = pynimbusauthz.list_user:main',
        ]
      },

      long_description="""
Cumulus is an open source implementation of the Amazon S3 REST API.  It
is packaged with the Nimbus (open source cloud computing software for
science) however it can be used without nimbus as well.  Cumulus allows
you to server files to users via a known and adopted REST API. You
clients will be able to access your data storaging service with the
Amazon S3 clients they already use.
""",
      license="Apache2",
      install_requires = ["twisted", "boto", "pyopenssl", "nose"],
     )
