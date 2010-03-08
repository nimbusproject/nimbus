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
Nimbus exception types. Subclassed for specific errors
"""

try:
    import json
except ImportError:
    try:
        import simplejson as json
    except ImportError:
        import django.utils.simplejson as json

class NimbusClientError(Exception):
    """
    General client error
    """

    def __init__(self, reason):
        self.reason = reason
        Exception.__init__(self, reason)

    def __repr__(self):
        return '%s: %s' % (self.__class__.__name__, self.reason)

    def __str__(self):
        return '%s: %s' % (self.__class__.__name__, self.reason)

class NimbusServerError(Exception):
    """
    Error response from Nimbus service
    """

    def __init__(self, status, reason, body=None):
        self.status = status
        self.reason = reason
        
        if body:
            data = json.loads(body)
            if data.has_key('message'):
                self.msg = data['message']
            if data.has_key('requestId'):
                self.request_id = data['requestId']
        
        Exception.__init__(self, reason)

    def __repr__(self):
        return '%s: %s (Request ID: %s)' % (self.__class__.__name__, 
                (self.msg or self.reason), self.request_id)
        
    def __str__(self):
        return '%s: %s (Request ID: %s)' % (self.__class__.__name__, 
                (self.msg or self.reason), self.request_id)

class NotFoundError(NimbusServerError):
    """
    Server couldn't find requested resource
    """
    pass

class ConflictError(NimbusServerError):
    """
    Server encountered conflict performing operation (duplicate key, perhaps)
    """
    pass
