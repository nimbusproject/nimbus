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

import os
from urlparse import urljoin

import httplib2
import json

from nimbusrest.error import NimbusServerError, NotFoundError

class Connection(object):
    """
    Connection to Nimbus REST API. Abstracts away
    Authentication business.
    """
   
    def __init__(self, uri, key=None, secret=None):
        if key:
            self.key = key
        elif os.environ.has_key('NIMBUS_KEY'):
            self.key = os.environ['NIMBUS_KEY']
        else:
            self.key = None

        if secret:
            self.secret = secret
        elif os.environ.has_key('NIMBUS_SECRET'):
            self.secret = os.environ['NIMBUS_SECRET']
        else:
            self.secret = None

        if self.key is None or self.secret is None:
            raise ValueError("Key and secret must be set.")

        self.client = httplib2.Http()
        self.client.add_credentials(self.key, self.secret)

        #TODO better url normalization/validation
        self.uri = uri+'/'

    def post_json(self, path, body, headers=None):
        """
        Makes a POST request with a json content type.
        If the body is not already a string, it is
        json-encoded.
        """

        if headers == None:
            headers = {}
        else:
            headers = headers.copy()
        headers['Content-Type'] = 'application/json'

        if not isinstance(body, basestring):
            body = json.dumps(body)

        return self.request('POST', path, body, headers)

    def request(self, method, path, body=None, headers=None):
        """
        Makes a request to the service. The path argument is
        appended to self.uri.

        Return value is a tuple: (response, body)
        Errors responses are raised as a NimbusServerError

        """

        uri = urljoin(self.uri, path)

        (resp, body) = self.client.request(uri, method, body, headers)

        if resp.status >= 400:
            return self.handle_error_response(resp, body)
        else:
            return self.handle_ok_response(resp, body)


    def handle_ok_response(self, response, body):
        """
        Creates appropriate return value from server response.

        if content type is application/json, deserializes.
        """

        if (response.has_key('content-type') and 
            response['content-type'] == 'application/json'):

            obj = json.loads(body)
            return obj

        return body


    def handle_error_response(self, response, body):
        """
        Encapsulates error into a NimbusServerError and raises it.

        Override this in subclasses to raise specific errors.
        """

        if response.status == 404:
            err = NotFoundError(response.status, response.reason, body)
        else:
            err = NimbusServerError(response.status, response.reason, body)

        raise err
