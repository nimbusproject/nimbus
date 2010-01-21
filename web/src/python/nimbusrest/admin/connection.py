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

import json
from urllib import quote_plus

from nimbusrest.connection import Connection
from nimbusrest.admin import User, AccessKey

class AdminConnection(Connection):

    def __init__(self, uri, key=None, secret=None):
        Connection.__init__(self, uri, key, secret)

    def list_users(self):
        #TODO not done
        return self.request('GET', 'users/')

    def add_user(self, user):
        #TODO not done
        s = json.dumps(user)

        self.request('POST', 'users/')

    def get_user_access_key(self, user_id):
        """
        Retrieves the access key pair for a user.
        Raises error if there is not an access key.
        """
        
        s = self.request('GET', 'users/%s/access_key' % (escape_id(user_id)))
        k = json.loads(s)
        return AccessKey(k['key'], k['secret'])

    def generate_user_access_key(self, user_id):
        """
        Generates an access key pair for user and returns it.
        If a key pair already exists for user, it will be discarded
        and replaced.
        """

        s = self.request('POST', 'users/%s/access_key' % (escape_id(user_id)))
        k = json.loads(s)
        return AccessKey(k['key'], k['secret'])


def escape_id(str):
    return quote_plus(str)
