#!/usr/bin/env python

try:
    from setuptools import setup
except ImportError:
    from distutils.core import setup

import sys
Version=0.1

if float("%d.%d" % sys.version_info[:2]) < 2.5:
    sys.stderr.write("Your Python version %d.%d.%d is not supported.\n" % sys.version_info[:3])
    sys.stderr.write("pycb requires Python 2.5 or newer.\n")
    sys.exit(1)

setup(name='pycb',
      version=Version,
      description='An Open Source S3 look-alike',
      author='John Bresnahan',
      author_email='bresnaha@mcs.anl.gov',
      url='http://www.nimbusproject.org/',
      packages=[ 'pycb', 'pycb.tools' ],
       entry_points = {
        'console_scripts': [
            'cumulus = pycb.cumulus:main',
            'cumulus-add-user = pycb.tools.add_user:main',
            'cumulus-remove-user = pycb.tools.remove_user:main',
            'cumulus-list-users = pycb.tools.list_users:main',
            'cumulus-quota = pycb.tools.set_quota:main',
            'cumulus-create-repo-admin = pycb.tools.base_repo:main',
        ]
      },

      long_description="""
Cumulus is an open source implementation of the Amazon S3 REST API.  It
is packaged with the Nimbus (open source cloud computing software for
science) however it can be used without nimbus as well.  Cumulus allows
you to server files to users via a known and adopted REST API. You
clients will be able to access your data storaging service with the
Amazon S3 clients they already use.

After installing this python package some additional system configuration is
needed.
""",
      license="Apache2",
      data_files=[("docs", ["docs/README.txt",])],
      install_requires = ["Twisted", "boto > 1.9", "pyOpenSSL", "pynimbusauthz"],
     )
