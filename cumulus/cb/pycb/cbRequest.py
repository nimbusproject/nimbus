import os
import sys
import time
from time import strftime
import stat
import hashlib
from xml.dom.minidom import Document, parseString
import uuid
from datetime import date, datetime
from twisted.internet import reactor, defer
from pycb.cbException import cbException
import time
import pycb
import logging
import traceback
import base64
import tempfile
from twisted.protocols.basic import FileSender
from twisted.python.log import err
import twisted.web.http
from pynimbusauthz.user import User


#
#  possible request types
#

perms_strings = {}
perms_strings['FULL_CONTROL'] = "WRrw"
perms_strings['WRITE'] = "w"
perms_strings['WRITE_ACP'] = "W"
perms_strings['READ_ACP'] = "R"
perms_strings['READ'] = "r"

def perm2string(p):
    global perms_strings
    for (k, v) in perms_strings.iteritems():
        if p == v:
            return k
    return None

def getText(nodelist):
    rc = ""
    for node in nodelist:
        if node.nodeType == node.TEXT_NODE:
            rc = rc + node.data
    return rc

def parse_acl_request(xml):
    dom = parseString(xml)
    grant_a = dom.getElementsByTagName("Grant")
    grants = []
    users = {}

    for g in grant_a:
        email = getText(g.getElementsByTagName("DisplayName")[0].childNodes)
        id = getText(g.getElementsByTagName("ID")[0].childNodes)
        perm_set = g.getElementsByTagName("Permission")

        perms = ""
        for p in perm_set:
            requested_perm = getText(p.childNodes)

            if requested_perm not in perms_strings:
                raise cbException('InvalidArgument')

            pv = perms_strings[requested_perm]
            ndx = perms.find(pv)
            if ndx < 0:
                perms = perms + pv

        if id in users.keys():
            (i, e, p) = users[id]
            perms = perms + p
        users[id] = (id, email, perms)

    return users.values()


class cbRequest(object):

    def __init__(self, request, user, requestId, bucketIface):
        self.bucketName = None
        self.objectName = None
        self.request = request
        self.user = user
        self.requestId = requestId
        self.bucketIface = bucketIface
        self.dataObject = None

        self.outGoingHeaders = {}
        self.responseCode = None
        self.responseMsg = None

        self.get_opts()

    def build_grant_acl(self, doc, acl_node, p, user_id, email):
        grant_node = doc.createElement("Grant")
        acl_node.appendChild(grant_node)

        grantee_node = doc.createElement("Grantee")
        grant_node.appendChild(grantee_node)
        id = doc.createElement("ID")
        grantee_node.appendChild(id)
        idText = doc.createTextNode(str(user_id))
        id.appendChild(idText)
        grantee_node.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
        grantee_node.setAttribute("xsi:type", "CanonicalUser")

        dN = doc.createElement("DisplayName")
        grantee_node.appendChild(dN)
        dNText = doc.createTextNode(email)
        dN.appendChild(dNText)
        permsN = doc.createElement("Permission")
        grant_node.appendChild(permsN)
        permsNText = doc.createTextNode(p)
        permsN.appendChild(permsNText)

    def get_acl_xml(self):
        (owner_id,owner_email) = self.user.get_owner(self.bucketName, self.objectName)
        acl = self.user.get_acl(self.bucketName, self.objectName)
        doc = Document()

        # Create the <wml> base element
        listAll = doc.createElement("AccessControlPolicy")
        doc.appendChild(listAll)

        # Create the main <card> element
        owner = doc.createElement("Owner")
        listAll.appendChild(owner)

        id = doc.createElement("ID")
        owner.appendChild(id)
        idText = doc.createTextNode(str(owner_id))
        id.appendChild(idText)

        dN = doc.createElement("DisplayName")
        owner.appendChild(dN)
        dNText = doc.createTextNode(owner_email)
        dN.appendChild(dNText)

        acl_node = doc.createElement("AccessControlList")
        listAll.appendChild(acl_node)

        for a in acl:
            user_id = a[0]
            email = a[1]
            perms = a[2]

            if len(perms) == 4 and 'w' in perms and 'r' in perms and 'R' in perms and 'W' in perms:
                self.build_grant_acl(doc,acl_node,'FULL_CONTROL',user_id,email)
            else:
                for p in perms:
                    perms_str = perm2string(p)
                    self.build_grant_acl(doc,acl_node,perms_str,user_id,email)

        x = doc.toxml();

        return x

    def get_opts(self):
        self.acl = False
        # deal with the uri to args bidnes
        opts_a = self.request.uri.split('?', 1)
        if len(opts_a) == 1:
            return
        all_opts = opts_a[1].strip()
        opts_a = all_opts.split('&')
        for opt in opts_a:
            if opt == "acl":
                self.acl = True

    def setHeader(self, request, k, v):
        request.setHeader(k, v)
        self.outGoingHeaders[k] = v

    def setResponseCode(self, request, code, msg):
        request.setResponseCode(code, msg)
        self.responseCode = code
        self.responseMsg = msg

    def finish(self, request):
        pycb.log(logging.INFO, "%s %s Reply sent %d %s" % (self.requestId, str(datetime.now()), self.responseCode, self.responseMsg))
        pycb.log(logging.INFO, str(self.outGoingHeaders))
        request.finish()

    def set_common_headers(self):
        amzid2 = str(uuid.uuid1()).replace("-", "")
        self.setHeader(self.request, 'x-amz-id-2', amzid2)
        self.setHeader(self.request,'x-amz-request-id', self.requestId)
        self.setHeader(self.request, 'Server', "cumulus")
        tm = datetime.utcnow()
        tmstr = tm.strftime("%a, %d %b %Y %H:%M:%S GMT")
        self.setHeader(self.request, 'date', tmstr)

    def send_xml(self, x):
        xLen = len(x)
        self.set_common_headers()
        self.setHeader(self.request, 'Content-Length', str(xLen))
        self.setHeader(self.request, 'Connection', 'close')
        self.setResponseCode(self.request, 200, 'OK')
        self.request.write(x)
        pycb.log(logging.INFO, "Sent %s" % (x))


    def set_no_content_header(self):
        self.set_common_headers()
        self.setHeader(self.request, 'Connection', 'close')
        self.setHeader(self.request, 'Content-Length', "0")
        self.setResponseCode(self.request, 204, 'No Content')

    # effectively there is no public user because you must be authorized to
    # do anything at all with cumulus
    def grant_public_permissions(self, bucketName, objectName):
        user = self.user
        headers = self.request.getAllHeaders()
        if 'x-amz-acl' not in headers:
            return False
        put_perms = headers['x-amz-acl']

        if put_perms == 'public-read':
            perms = "r"
            (set_user_name, fn) = (pycb.public_user_id, pycb.public_user_id)
        elif put_perms == 'public-read-write':
            perms = "rw"
            (set_user_name, fn) = (pycb.public_user_id, pycb.public_user_id)
        elif put_perms == 'authenticated-read':
            (set_user_name, fn) = (pycb.authenticated_user_id, pycb.authenticated_user_id)
            perms = "r"
        elif put_perms == 'bucket-owner-read':
            # only makes sense for an object
            if objectName == None:
                return
            perms = "r"
            (set_user_name, fn) = user.get_owner(bucketName)
        elif put_perms == 'bucket-owner-full-control':
            # only makes sense for an object
            if objectName == None:
                return
            perms = "rRwW"
            (set_user_name, fn) = user.get_owner(bucketName)
        else:
            # act all like private
            perms = "rRwW"
            set_user_name = user.get_id()
            fn = user.get_display_name()
        if set_user_name == user.get_id():
            # nothing to do in this case
            return True
        user.grant(set_user_name, bucketName, objectName, perms)
        return True

class cbGetService(cbRequest):

    def __init__(self, request, user, requestId, bucketIface):
        cbRequest.__init__(self, request, user, requestId, bucketIface)

    def work(self):
        dirL = self.user.get_my_buckets()
        request = self.request
        doc = Document()

        # Create the <wml> base element
        listAll = doc.createElement("ListAllMyBucketsResult")
        listAll.setAttribute("xmlns", "http://doc.s3.amazonaws.com/2006-03-01")
        doc.appendChild(listAll)

        # Create the main <card> element
        owner = doc.createElement("Owner")
        listAll.appendChild(owner)

        id = doc.createElement("ID")
        owner.appendChild(id)
        idText = doc.createTextNode(str(self.user.get_id()))
        id.appendChild(idText)

        dN = doc.createElement("DisplayName")
        owner.appendChild(dN)
        dNText = doc.createTextNode(str(self.user.get_display_name()))
        dN.appendChild(dNText)

        buckets = doc.createElement("Buckets")
        listAll.appendChild(buckets)

        for obj in dirL:
            bucketOne = doc.createElement("Bucket")
            buckets.appendChild(bucketOne)

            nameOne = doc.createElement("Name")
            bucketOne.appendChild(nameOne)
            nameText = doc.createTextNode(str(obj.get_key()))
            nameOne.appendChild(nameText)
            dateOne = doc.createElement("CreationDate")
            bucketOne.appendChild(dateOne)
            dateText = doc.createTextNode(str(obj.get_date_string()))
            dateOne.appendChild(dateText)

        x = doc.toxml();
        self.send_xml(x)
        self.finish(self.request)

class cbGetBucket(cbRequest):

    def __init__(self, request, user, bucket, requestId, bucketIface):
        cbRequest.__init__(self, request, user, requestId, bucketIface)
        self.bucketName = bucket

    def work(self):
        exists = self.user.exists(self.bucketName)
        if not exists:
            raise cbException('NoSuchBucket')
        (perms, data_key) = self.user.get_perms(self.bucketName)
        if self.acl:
            ndx = perms.find("R")
            if ndx < 0:
                raise cbException('AccessDenied')
            else:
                self.get_acl()
        else:
            ndx = perms.find("r")
            if ndx < 0:
                raise cbException('AccessDenied')
            else:
                self.list_bucket()

    def get_acl(self):
        payload = self.get_acl_xml()
        pycb.log(logging.INFO, "GET BUCKET ACL XML %s" % (payload))
        self.send_xml(payload)
        self.finish(self.request)

    def get_location(self):
        doc = Document()
        xList = doc.createElement("LocationConstraint")
        xList.setAttribute("xmlns", "http://doc.s3.amazonaws.com/2006-03-01")
        xNameText = doc.createTextNode(pycb.config.location)
        xList.appendChild(xNameText)
        doc.appendChild(xList)
        x = doc.toxml();
        self.send_xml(x)
        self.finish(self.request)

    def list_bucket(self):
        dirL = self.user.list_bucket(self.bucketName, self.request.args)
        doc = Document()

        # Create the <wml> base element
        xList = doc.createElement("ListBucketResult")
        xList.setAttribute("xmlns", "http://doc.s3.amazonaws.com/2006-03-01")
        doc.appendChild(xList)

        # Create the main <card> element
        xName = doc.createElement("Name")
        xList.appendChild(xName)
        xNameText = doc.createTextNode(str(self.bucketName))
        xName.appendChild(xNameText)

#  XXX add options to headers
#        if 'max-keys' in self.request.args.keys():
#

        xIsTruncated = doc.createElement("IsTruncated")
        xList.appendChild(xIsTruncated)
        xIsTText = doc.createTextNode('false')
        xIsTruncated.appendChild(xIsTText)

        for obj in dirL:
            xObj = obj.create_xml_element(doc)
            xList.appendChild(xObj)

        x = doc.toxml();

        self.send_xml(x)
        self.finish(self.request)

class cbGetObject(cbRequest):

    def __init__(self, request, user, bucketName, objName, requestId, bucketIface):
        cbRequest.__init__(self, request, user, requestId, bucketIface)
        self.bucketName = bucketName
        self.objectName = objName

    def work(self):
        exists = self.user.exists(self.bucketName, self.objectName)
        if not exists:
            raise cbException('NoSuchKey')
        (perms, data_key) = self.user.get_perms(self.bucketName, self.objectName)
        if self.acl:
            ndx = perms.find("R")
            if ndx < 0:
                raise cbException('AccessDenied')
            else:
                self.get_acl()
        else:
            ndx = perms.find("r")
            if ndx < 0:
                raise cbException('AccessDenied')
            else:
                obj = self.bucketIface.get_object(data_key)
                self.sendObject(obj)

    def get_acl(self):
        payload = self.get_acl_xml()
        pycb.log(logging.INFO, "GET BUCKET ACL XML %s" % (payload))
        self.send_xml(payload)
        self.finish(self.request)

    def calcMd5Sum(self, dataObj):
        md5obj = self.bucketIface.get_object(dataObj.getDataKey())
        md5er = hashlib.md5()
        done = False
        while not done:
            b = md5obj.read()
            if len(b) > 0:
                md5er.update(b)
            else:
                done = True
        md5obj.close()
        etag = str(md5er.hexdigest()).strip()
        return etag


    def sendFile(self, dataObj):
        try:
            etag = dataObj.get_md5()
            if etag == None:
                etag = self.calcMd5Sum(dataObj)
                dataObj.set_md5(etag)
            self.setHeader(self.request, 'ETag', '"%s"' % (etag))
            self.setResponseCode(self.request, 200, 'OK')

            fp = dataObj
            d = FileSender().beginFileTransfer(fp, self.request)
            def cbFinished(ignored):
                fp.close()
                self.request.finish()
            d.addErrback(err).addCallback(cbFinished)

        except cbException, (ex):
            ex.sendErrorResponse(self.request, self.requestId)
            traceback.print_exc(file=sys.stdout)
        except:
            traceback.print_exc(file=sys.stdout)
            gdEx = cbException('InvalidArgument')
            gdEx.sendErrorResponse(self.request, self.requestId)


    def sendObject(self, dataObj):
        request = self.request
        self.dataObj = dataObj

        self.set_common_headers()
        self.setHeader(request, 'Content-Type', 'binary/octet-stream')
        self.setHeader(request, 'Content-Length', str(dataObj.get_size()))

        reactor.callInThread(self.sendFile, dataObj)

class cbDeleteBucket(cbRequest):

    def __init__(self, request, user, bucketName, requestId, bucketIface):
        cbRequest.__init__(self, request, user, requestId, bucketIface)
        self.bucketName = bucketName

    def work(self):
        exists = self.user.exists(self.bucketName)
        if not exists:
            raise cbException('NoSuchBucket')
        (perms, data_key) = self.user.get_perms(self.bucketName)
        ndx = perms.find("w")
        if ndx < 0:
            raise cbException('AccessDenied')
        self.deleteIt(data_key)

    def deleteIt(self, data_key):
        self.set_no_content_header()
        self.user.delete_bucket(self.bucketName)
        self.finish(self.request)

class cbDeleteObject(cbRequest):

    def __init__(self, request, user, bucketName, objName, requestId, bucketIface):
        cbRequest.__init__(self, request, user, requestId, bucketIface)
        self.bucketName = bucketName
        self.objectName = objName

    def work(self):
        exists = self.user.exists(self.bucketName, self.objectName)
        if not exists:
            raise cbException('NoSuchKey')

        (perms, data_key) = self.user.get_perms(self.bucketName, self.objectName)
        ndx = perms.find("w")
        if ndx < 0:
            raise cbException('AccessDenied')
        else:
            self.deleteIt(data_key)

    def deleteIt(self, data_key):
        request = self.request
        self.set_no_content_header()
        self.bucketIface.delete_object(data_key)
        self.user.delete_object(self.bucketName, self.objectName)
        self.finish(request)

class cbPutBucket(cbRequest):

    def __init__(self, request, user, bucketName, requestId, bucketIface):
        cbRequest.__init__(self, request, user, requestId, bucketIface)
        self.bucketName = bucketName

    def work(self):
        request = self.request
        exists = self.user.exists(self.bucketName)

        if self.acl:
            if not exists:
                raise cbException('NoSuchBucket')
            (perms, data_key) = self.user.get_perms(self.bucketName)
            ndx = perms.find("W")
            if ndx < 0:
                raise cbException('AccessDenied')

            rc = self.grant_public_permissions(self.bucketName, self.objectName)
            if not rc:
                xml = self.request.content.read()
                grants = parse_acl_request(xml)
                for g in grants:
                    pycb.log(logging.INFO, "granting %s to %s" % (g[2], g[0]))
                    self.user.grant(g[0], self.bucketName, perms=g[2])
        else:
            if exists:
                raise cbException('BucketAlreadyExists')
            self.user.put_bucket(self.bucketName)
            self.grant_public_permissions(self.bucketName, self.objectName)

        self.set_common_headers()
        self.setHeader(request, 'Content-Length', 0)
        self.setHeader(request, 'Connection', 'close')
        self.setHeader(request, 'Location', "/" + self.bucketName)
        self.setResponseCode(request, 200, 'OK')

        self.finish(request)

class cbPutObject(cbRequest):

    def __init__(self, request, user, bucketName, objName, requestId, bucketIface):
        cbRequest.__init__(self, request, user, requestId, bucketIface)
        ndx = objName.find("cumulus:/")
        if ndx >= 0:
            pycb.log(logging.ERROR, "someone tried to make a key named cumulus://... why would someone do that? %d" % (ndx))
            raise cbException('InvalidURI')

        self.checkMD5 = None
        self.bucketName = bucketName
        self.objectName = objName

    def work(self):
        dataObj = self.request.content
        exists = self.user.exists(self.bucketName, self.objectName)
        if self.acl:
            if not exists:
                raise cbException('NoSuchKey')
            (perms, data_key) = self.user.get_perms(self.bucketName, self.objectName)
            ndx = perms.find("W")
            if ndx < 0:
                raise cbException('AccessDenied')

            rc = self.grant_public_permissions(self.bucketName, self.objectName)
            if not rc:
                xml = self.request.content.read()
                grants = parse_acl_request(xml)
                for g in grants:
                    pycb.log(logging.INFO, "granting %s to %s" % (g[2], g[0]))
                    self.user.grant(g[0], self.bucketName, self.objectName, perms=g[2])

            self.set_common_headers()
            self.setHeader(self.request, 'Content-Length', 0)
            self.setHeader(self.request, 'Connection', 'close')
            self.setHeader(self.request, 'Location', "/" + self.bucketName)
            self.setResponseCode(self.request, 200, 'OK')
            self.finish(self.request)
        else:
            (bperms, bdata_key) = self.user.get_perms(self.bucketName)
            ndx = bperms.find("w")
            if ndx < 0:
                raise cbException('AccessDenied')

            file_size = 0
            if exists:
                (perms, data_key) = self.user.get_perms(self.bucketName, self.objectName)
                ndx = perms.find("w")
                if ndx < 0:
                    raise cbException('AccessDenied')

                # make sure they can write to the bucket
                (perms, data_key) = self.user.get_perms(self.bucketName)
                ndx = perms.find("w")
                if ndx < 0:
                    raise cbException('AccessDenied')
                (file_size, ctm, md5) = self.user.get_info(self.bucketName, self.objectName)

            # gotta decide quota, if existed should get credit for the
            # existing size
            remaining_quota = self.user.get_remaining_quota()
            if remaining_quota != User.UNLIMITED:
                new_file_len = int(self.request.getHeader('content-length'))
                if remaining_quota + file_size < new_file_len:
                    pycb.log(logging.INFO, "user %s did not pass quota.  file size %d quota %d" % (self.user, new_file_len, remaining_quota))
                    raise cbException('AccountProblem')

            obj = self.request.content
            self.recvObject(self.request, obj)


    def endGet(self, dataObj):
        try:
            eTag = dataObj.get_md5()
            mSum = base64.encodestring(base64.b16decode(eTag.upper()))
            self.checkMD5 = mSum

            if self.checkMD5 != mSum:
                raise cbException('InvalidDigest')

            self.setHeader(self.request, 'ETag', '"%s"' % (eTag))

            # now that we have the file set delete on close to false
            # it will now be safe to deal with dropped connections
            # without having large files left around
            dataObj.set_delete_on_close(False)
            # dataObj.close() do no need to close for now.  twisted will
            # do this for us
            self.user.put_object(dataObj, self.bucketName, self.objectName)
            self.grant_public_permissions(self.bucketName, self.objectName)

            self.finish(self.request)
        except cbException, (ex):
            ex.sendErrorResponse(self.request, self.requestId)
        except:
            traceback.print_exc(file=sys.stdout)
            gdEx = cbException('InvalidArgument')
            gdEx.sendErrorResponse(self.request, self.requestId)

    def recvObject(self, request, dataObj):
        self.set_common_headers()
        self.setHeader(request, 'Connection', 'close')
        self.setHeader(request, 'Content-Length', 0)
        self.setResponseCode(request, 200, 'OK')
        self.dataObj = dataObj
        self.block_size = 1024*256

        headers = request.getAllHeaders()
        for (k,v) in headers.iteritems():
            kl = k.lower()
            if kl == 'content-md5':
                self.checkMD5 = v
            elif kl == 'x-amz-acl':
                self.put_perms = v

        self.endGet(dataObj)


class cbHeadObject(cbGetObject):

    def __init__(self, request, user, bucketName, objName, requestId, bucketIface):
        cbRequest.__init__(self, request, user, requestId, bucketIface)
        self.bucketName = bucketName
        self.objectName = objName

    def work(self):
        (perms, data_key) = self.user.get_perms(self.bucketName, self.objectName)

        ndx = perms.find("r")
        if ndx < 0:
            raise cbException('AccessDenied')
        (sz, tm, md5) = self.user.get_info(self.bucketName, self.objectName)

        d_str = "%04d-%02d-%02dT%02d:%02d:%02d.000Z" % (tm.tm_year, tm.tm_mon, tm.tm_wday, tm.tm_hour, tm.tm_min, tm.tm_sec)

        self.set_common_headers()
        self.setHeader(self.request, 'Content-Type', 'binary/octet-stream')
        self.setHeader(self.request, 'Last-Modified', d_str)
        self.setHeader(self.request, 'ETag', '"%s"' % (str(md5)))
        self.setHeader(self.request, 'Content-Length', str(sz))


#Content-Type: text/plain


        self.setResponseCode(self.request, 200, 'OK')
        self.finish(self.request)

