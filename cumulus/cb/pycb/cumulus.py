#!/usr/bin/env python

from twisted.web import server, resource, http
from twisted.internet import reactor, ssl
from cbPosixBackend import cbPosixBackend
from ConfigParser import SafeConfigParser
import hashlib
from pycb.cbException import cbException
from pycb.cbRequest import cbGetService
from pycb.cbRequest import cbGetBucket
from pycb.cbRequest import cbGetObject
from pycb.cbRequest import cbDeleteBucket
from pycb.cbRequest import cbDeleteObject
from pycb.cbRequest import cbPutBucket
from pycb.cbRequest import cbPutObject
from pycb.cbRequest import cbHeadObject
from pycb.cbRequest import cbCopyObject
from pycb.cbRedirector import *
from datetime import date, datetime
from xml.dom.minidom import Document
import uuid
import urllib
import traceback
import sys
import os
import socket
import logging
import pycb
import threading
import tempfile
import threading

def end_redirector(result, request):
    pycb.config.redirector.end_connection(request)

def init_redirector(req, bucketName, objectName):
    redir_host = pycb.config.redirector.new_connection(req)
    req.notifyFinish().addBoth(end_redirector, req)

    if redir_host:
        pycb.log(logging.INFO, "REDIRECT %s" % (redir_host))
        ex = cbException('TemporaryRedirect')
        req.setHeader('location', "http://%s%s" % (redir_host, req.uri))
        ex.add_custom_xml("Bucket", bucketName)
        ex.add_custom_xml("Endpoint", redir_host)
        raise ex

def path_to_bucket_object(path):
    path = urllib.unquote(path)
    if path == "/":
        return (path, None)
    # extract out the bucket name
    while path[0] == '/':
        path = path[1:]
    p_a = path.split("/", 1)
    bucketName = p_a[0].strip()
    objectName = None
    # if no object do bucket operations
    if len(p_a) > 1:
        objectName = p_a[1].strip()
        if objectName == "":
            objectName = None
    return (bucketName, objectName)

def createPath(headers, path):

    host = headers['host']

    h_a = host.split(':')
    if len(h_a) > 0:
        host = h_a[0]

    return path

def authorize(headers, message_type, path, uri):

    sent_auth = headers['authorization']
    auth_A = sent_auth.split(':')
    auth_hash = auth_A[1]
    id = auth_A[0].split()[1].strip()
    user = pycb.config.auth.get_user(id)
    key = user.get_password()

    pycb.log(logging.INFO, "AUTHORIZING %s %s %s" % (message_type, path, headers))
    b64_hmac = pycb.get_auth_hash(key, message_type, path, headers, uri)

    if auth_hash == b64_hmac:
        return user
    pycb.log(logging.ERROR, "%s %s %s" % (key, b64_hmac, auth_hash))
    ec = 'AccessDenied'
    ex = cbException(ec)
    raise ex



class CBService(resource.Resource):
    isLeaf = True

    def __init__(self):
        pass

    def get_port(self):
        return pycb.config.port

    # amazon uses some smaller num generator, might have to match theres
    # for clients that make assumptions
    def next_request_id(self):
        return str(uuid.uuid1()).replace("-", "")

    #  figure out if the operation is targeted at a service, bucket, or


    #  object
    def request_object_factory(self, request, user, path, requestId):
        pycb.log(logging.INFO, "path %s" % (path))
        # handle the one service operation
        if path == "/":
            if request.method == 'GET':
                cbR = cbGetService(request, user, requestId, pycb.config.bucket)
                return cbR
            raise cbException('InvalidArgument')

        (bucketName, objectName) = path_to_bucket_object(path)
        init_redirector(request, bucketName, objectName)

        pycb.log(logging.INFO, "path %s bucket %s object %s" % (path, bucketName, str(objectName)))
        if request.method == 'GET':
            if objectName == None:
                cbR = cbGetBucket(request, user, bucketName, requestId, pycb.config.bucket)
            else:
                cbR = cbGetObject(request, user, bucketName, objectName, requestId, pycb.config.bucket)
            return cbR
        elif request.method == 'PUT':
            if objectName == None:
                cbR = cbPutBucket(request, user, bucketName, requestId, pycb.config.bucket)
            else:
                args = request.getAllHeaders()
                if 'x-amz-copy-source' in args:
                    (srcBucketName, srcObjectName) = path_to_bucket_object(args['x-amz-copy-source'])
                    cbR = cbCopyObject(request, user, requestId, pycb.config.bucket, srcBucketName, srcObjectName, bucketName, objectName)
                else:
                    cbR = cbPutObject(request, user, bucketName, objectName, requestId, pycb.config.bucket)
            return cbR
        elif request.method == 'POST':
            pycb.log(logging.ERROR, "Nothing to handle POST")
        elif request.method == 'DELETE':
            if objectName == None:
                cbR = cbDeleteBucket(request, user, bucketName, requestId, pycb.config.bucket)
            else:
                cbR = cbDeleteObject(request, user, bucketName, objectName, requestId, pycb.config.bucket)
            return cbR
        elif request.method == 'HEAD' and objectName != None:
            cbR = cbHeadObject(request, user, bucketName, objectName, requestId, pycb.config.bucket)
            return cbR

        raise cbException('InvalidArgument')

    # everything does through here to localize access control
    def process_event(self, request):
        try:
            rPath = createPath(request.getAllHeaders(), request.path)
 
            requestId = self.next_request_id()

            pycb.log(logging.INFO, "%s %s Incoming" % (requestId, str(datetime.now())))
            pycb.log(logging.INFO, "%s %s" % (requestId, str(request)))
            pycb.log(logging.INFO, "%s %s" % (requestId, rPath))
            pycb.log(logging.INFO, "%s %s" %(requestId, request.getAllHeaders()))
            pycb.log(logging.INFO, "request URI %s method %s %s" %(request.uri, request.method, str(request.args)))

            user = authorize(request.getAllHeaders(), request.method, rPath, request.uri)
            self.allowed_event(request, user, requestId, rPath)
        except cbException, ex:
            eMsg = ex.sendErrorResponse(request, requestId)
            pycb.log(logging.ERROR, eMsg, traceback)
        except Exception, ex2:
            traceback.print_exc(file=sys.stdout)
            gdEx = cbException('InternalError')
            eMsg = gdEx.sendErrorResponse(request, requestId)
            pycb.log(logging.ERROR, eMsg, traceback)

    def allowed_event(self, request, user, requestId, path):
        pycb.log(logging.INFO, "Access granted to ID=%s requestId=%s uri=%s" % (user.get_id(), requestId, request.uri))
        cbR = self.request_object_factory(request, user, path, requestId)

        cbR.work()

    # http events.  all do the same thing
    def render_GET(self, request):
        self.process_event(request)
        return server.NOT_DONE_YET

    # not implementing any post stuff for now
    def render_POST(self, request):
        self.process_event(request)
        return server.NOT_DONE_YET

    def render_PUT(self, request):
        self.process_event(request)
        return server.NOT_DONE_YET

    def render_DELETE(self, request):
        self.process_event(request)
        return server.NOT_DONE_YET

class CumulusHTTPChannel(http.HTTPChannel):

    def getAllHeaders(self, req):
        """
        Return dictionary mapping the names of all received headers to the last
        value received for each.

        Since this method does not return all header information,
        C{self.requestHeaders.getAllRawHeaders()} may be preferred.
        """
        headers = {}
        for k, v in req.requestHeaders.getAllRawHeaders():
            headers[k.lower()] = v[-1]
        return headers

    def send_access_error(self, req):
        ex = cbException('AccessDenied')
        m_msg = "HTTP/1.1 %s %s\r\n" % (ex.httpCode, ex.httpDesc)
        self.transport.write(m_msg)
        m_msg = "%s: %s\r\n" % (('x-amz-request-id', str(uuid.uuid1())))
        self.transport.write(m_msg)
        self.transport.write('content-type: text/html')
        e_msg = ex.make_xml_string(self._path, str(uuid.uuid1()))
        self.transport.write(e_msg)
        self.transport.loseConnection()

    def send_redirect(self):
        m_msg = "HTTP/1.1 %s %s\r\n" % (ex.httpCode, ex.httpDesc)
        self.transport.write(m_msg)
        m_msg = "%s: %s\r\n" % (('x-amz-request-id', str(uuid.uuid1())))
        self.transport.write(m_msg)
        self.transport.write('content-type: text/html\r\n')
        e_msg = ex.make_xml_string(self._path, str(uuid.uuid1()))
        self.transport.write(e_msg)
        self.transport.loseConnection()
        return ex


    # intercept the key event
    def allHeadersReceived(self):
        http.HTTPChannel.allHeadersReceived(self)

        req = self.requests[-1]
        req._cumulus_killed = None
        h = self.getAllHeaders(req)
        # we can check the authorization here
        rPath = self._path
        ndx = rPath.rfind('?')
        if ndx >= 0:
            rPath = rPath[0:ndx]
        rPath = createPath(h, rPath)
#        try:
#            user = authorize(h, self._command, rPath, self._path)
#        except:
#            self.send_access_error(req)
#            return

        if 'expect' in h:
            if h['expect'].lower() == '100-continue':
                self.transport.write("HTTP/1.1 100 Continue\r\n\r\n")

        (bucketName, objectName) = path_to_bucket_object(rPath)
        # if we are putting an object
        if objectName != None and self._command == "PUT":
            #  free up the temp object that we will not be using
            req.content.close()
            # give twisted our own file like object
            req.content = pycb.config.bucket.put_object(bucketName, objectName)
            req.content.set_delete_on_close(True)



class CumulusSite(server.Site):
    protocol = CumulusHTTPChannel


class CumulusRunner(object):

    def __init__(self):
        self.done = False
        self.cb = CBService()
        self.site = CumulusSite(self.cb)

        # figure out if we need http of https 
        if pycb.config.use_https:
            pycb.log(logging.INFO, "using https")
            sslContext = ssl.DefaultOpenSSLContextFactory(
              pycb.config.https_key,
              pycb.config.https_cert)
            self.iconnector = reactor.listenSSL(self.cb.get_port(),
              self.site,
              sslContext)
        else:
            pycb.log(logging.INFO, "using http")
            self.iconnector = reactor.listenTCP(self.cb.get_port(), self.site)

    def getListener(self):
        return self.iconnector.getHost()

    def get_port(self):
        l = self.getListener()
        return l.port

    def run(self):
        reactor.suggestThreadPoolSize(10)
        reactor.run()

    def stop(self):
        self.iconnector.stopListening()


def main(argv=sys.argv[0:]):
    pycb.config.parse_cmdline(argv)

    try:
        cumulus = CumulusRunner()
    except Exception, ex:
        pycb.log(logging.ERROR, "error starting the server, check that the port is not already taken: %s" % (str(ex)), tb=traceback)
        raise ex
    pycb.log(logging.INFO, "listening at %s" % (str(cumulus.getListener())))
    cumulus.run()
    return 0

if __name__ == "__main__":
    rc = main()
    sys.exit(rc)

