#!/usr/bin/python

import string
import random
import os
import sys
import nose.tools
import sys
from ConfigParser import SafeConfigParser
from pycb.cumulus import *
import time
import pycb.test_common
import pynimbusauthz
from pynimbusauthz.user import User
from pynimbusauthz.objects import File
from pynimbusauthz.db import DB
import unittest
import tempfile
import filecmp


def main(argv=sys.argv[1:]):

    try:
        repo_dir = argv[0]
        con_str = pynimbusauthz.get_db_connection_string()
        db_obj = DB(con_str=con_str)

        user = User(db_obj, uu="CumulusPublicUser")
        if user == None:
            raise Exception("No public user")

        File.create_file(db_obj, repo_dir, user, repo_dir, pynimbusauthz.alias_type_s3)
        db_obj.commit()
    except:
        raise

    return 0

if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

