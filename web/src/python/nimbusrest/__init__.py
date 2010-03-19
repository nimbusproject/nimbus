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

"""
Client to the Nimbus RESTful web service
"""

VERSION = '0.1'

def connect_admin(uri, key=None, secret=None, **kwargs):
    """
    Returns a connection to the admin API.

    May not actually create a connection until an operation is performed.
    """
    
    from nimbusrest.admin.connection import AdminConnection

    return AdminConnection(uri, key, secret, **kwargs)

