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

from urllib import quote_plus

from nimbusrest.connection import Connection
from nimbusrest.admin import User, AccessKey

class AdminConnection(Connection):

    def __init__(self, uri, key=None, secret=None):
        Connection.__init__(self, uri, key, secret)

    def list_users(self):
        """
        Retrieves a list of users from service
        """

        r = self.request('GET', 'users/')
        return [self._user_from_data(u) for u in r]

    def add_user(self, user):
        """
        Adds a new user and returns it with server-generated
        fields populated.
        """

        #TODO validate user?

        udict = {'dn' : user.dn}

        u = self.post_json('users/', udict)
        return self._user_from_data(u)

    def get_user(self, user_id):
        """
        Retrieves a single user from service
        """

        u = self.request('GET', 'users/%s' % (escape_id(user_id)))

        return self._user_from_data(u)

    def _user_from_data(self, data):
        return User(data['dn'], conn=self, user_id=data['id']) 

    def get_user_access_key(self, user_id):
        """
        Retrieves the access key pair for a user.
        Raises error if there is not an access key.
        """
        
        k = self.request('GET', 'users/%s/access_key' % escape_id(user_id))
        return AccessKey(k['key'], k['secret'])

    def generate_user_access_key(self, user_id):
        """
        Generates an access key pair for user and returns it.
        If a key pair already exists for user, it will be discarded
        and replaced.
        """

        k = self.request('POST', 'users/%s/access_key' % escape_id(user_id))
        return AccessKey(k['key'], k['secret'])


def escape_id(user_id):
    return quote_plus(user_id)
