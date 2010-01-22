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

from nimbusrest.error import NimbusClientError

class User(object):
    """
    A single Nimbus user
    """
    def __init__(self, dn, conn=None, user_id=None):
        self.dn = dn
        self.conn = conn
        self.user_id = id

    def get_access_key(self):
        """
        Retrieves access key pair for this user.
        Raises error if there is not an access key.
        """

        self._assure_connection()
        return self.conn.get_user_access_key(self.user_id)
    
    def generate_access_key(self):
        """
        Generates an access key pair for user and returns it.
        If a key pair already exists for user, it will be discarded
        and replaced.
        """

        self._assure_connection()
        return self.conn.generate_user_access_key(self.user_id)

    def _assure_connection(self):
        """
        Ensures that user has a connection and can make requests
        """
        if not self.conn:
            raise NimbusClientError('User object has no associated connection')
        if not self.user_id:
            raise NimbusClientError('User object has no ID')


class AccessKey(object):
    """
    A user's access key and secret
    """
    def __init__(self, key, secret):
        self.key = key
        self.secret = secret
