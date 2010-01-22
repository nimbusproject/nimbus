# Copyright 2010 University of Chicago
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy
# of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

import unittest

import nimbusrest
from nimbusrest.admin import User,AccessKey
from nimbusrest.error import NimbusServerError,NotFoundError

class NotReallyAUnitTest(unittest.TestCase):
    def runTest(self):
        admin = nimbusrest.connect_admin('https://localhost:4443/admin')

        users = admin.list_users()
        self.assertTrue(len(users) == 0)

        u = User('a fake dn')
        u = admin.add_user(u)

        print u.id

        has_key = True
        try:
            u.get_access_key()
        except NotFoundError:
            has_key = False

        self.assertFalse(has_key)

        key = u.generate_access_key()
        print "Key: %s    Secret: %s\n" % (key.key, key.secret)


if __name__ == '__main__':
    unittest.main()


