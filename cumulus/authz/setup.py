#!/usr/bin/env python

try:
    from setuptools import setup
except ImportError:
    from distutils.core import setup

import sys
from pynimbusauthz import Version

if float("%d.%d" % sys.version_info[:2]) < 2.5:
    sys.stderr.write("Your Python version %d.%d.%d is not supported.\n" % sys.version_info[:3])
    sys.stderr.write("pynimbusauthz requires Python 2.5 or newer.\n")
    sys.exit(1)

setup(name='pynimbusauthz',
      version=Version,
      description='An Open Source User Management Library for Nimbus.',
      author='John Bresnahan',
      author_email='bresnaha@mcs.anl.gov',
      url='http://www.nimbusproject.org/',
      packages=[ 'pynimbusauthz', 'pynimbusauthz.tests' ],
       entry_points = {
        'console_scripts': [
            'nimbusauthz-add-user = pynimbusauthz.add_user:main',
            'nimbusauthz-list-users = pynimbusauthz.list_user:main',
            'nimbusauthz-touch = pynimbusauthz.touch:main',
            'nimbusauthz-quota = pynimbusauthz.quota:main',
            'nimbusauthz-stat = pynimbusauthz.stat:main',
            'nimbusauthz-ls = pynimbusauthz.ls:main',
            'nimbusauthz-chmod = pynimbusauthz.chmod:main',
            'nimbusauthz-rebase = pynimbusauthz.rebase:main',
        ]
      },
      include_package_data = True,
      package_data = { '' : ['*.sql']},

      long_description="""
This library makes use of a database to create user accounts suitable
for the cumulus s3 transfer service and the nimbus IaaS system.

A database needs to be setup and configured outside of this package.
""",
      license="Apache2",
      install_requires = ["nose"],
     )
