import traceback
import sys
import uuid
import pycb
import logging
from xml.dom.minidom import Document

class cbException(Exception):

    panicError = '<?xml version="1.0" encoding="UTF-8"?><Error><Code>NoSuchKey</Code><Message>The resource you requested does not exist</Message><Resource>/mybucket/myfoto.jpg</Resource><RequestId>4442587FB7D0A2F9</RequestId></Error>'

    errorsCode = {}
    errorsHttpCode = {}
    errorsHttpMsg = {}

    # errror type 0 AccessDenied
    errorsCode['AccessDenied'] = 'Access Denied'
    errorsHttpCode['AccessDenied'] = 403
    errorsHttpMsg['AccessDenied'] = 'Forbidden'

    # errror type 1 AccountProblem
    errorsCode['AccountProblem'] = 'There is a problem with your AWS account that prevents the operation from completing successfully. Please contact customer service at webservices@amazon.com.'
    errorsHttpCode['AccountProblem'] = 403
    errorsHttpMsg['AccountProblem'] = 'Forbidden'

    # errror type 2 AmbiguousGrantByEmailAddress
    errorsCode['AmbiguousGrantByEmailAddress'] = 'The e-mail address you provided is associated with more than one account.'
    errorsHttpCode['AmbiguousGrantByEmailAddress'] = 400
    errorsHttpMsg['AmbiguousGrantByEmailAddress'] = 'Bad Request'

    # errror type 3 BadDigest
    errorsCode['BadDigest'] = 'The Content-MD5 you specified did not match what we received.'
    errorsHttpCode['BadDigest'] = 400
    errorsHttpMsg['BadDigest'] = 'Bad Request'

    # errror type 4 BucketAlreadyExists
    errorsCode['BucketAlreadyExists'] = 'The requested bucket name is not available. The bucket namespace is shared by all users of the system. Please select a different name and try again.'
    errorsHttpCode['BucketAlreadyExists'] = 409
    errorsHttpMsg['BucketAlreadyExists'] = 'Conflict'

    # errror type 5 BucketAlreadyOwnedByYou
    errorsCode['BucketAlreadyOwnedByYou'] = 'Your previous request to create the named bucket succeeded and you already own it.'
    errorsHttpCode['BucketAlreadyOwnedByYou'] = 409
    errorsHttpMsg['BucketAlreadyOwnedByYou'] = 'Conflict'

    # errror type 6 BucketNotEmpty
    errorsCode['BucketNotEmpty'] = 'The bucket you tried to delete is not empty.'
    errorsHttpCode['BucketNotEmpty'] = 409
    errorsHttpMsg['BucketNotEmpty'] = 'Conflict'

    # errror type 7 CredentialsNotSupported
    errorsCode['CredentialsNotSupported'] = 'This request does not support credentials.'
    errorsHttpCode['CredentialsNotSupported'] = 400
    errorsHttpMsg['CredentialsNotSupported'] = 'Bad Request'

    # errror type 8 CrossLocationLoggingProhibited
    errorsCode['CrossLocationLoggingProhibited'] = 'Cross location logging not allowed. Buckets in one geographic location cannot log information to a bucket in another location.'
    errorsHttpCode['CrossLocationLoggingProhibited'] = 403
    errorsHttpMsg['CrossLocationLoggingProhibited'] = 'Forbidden'

    # errror type 9 EntityTooSmall
    errorsCode['EntityTooSmall'] = 'Your proposed upload is smaller than the minimum allowed object size.'
    errorsHttpCode['EntityTooSmall'] = 400
    errorsHttpMsg['EntityTooSmall'] = 'Bad Request'

    # errror type 10 EntityTooLarge
    errorsCode['EntityTooLarge'] = 'Your proposed upload exceeds the maximum allowed object size.'
    errorsHttpCode['EntityTooLarge'] = 400
    errorsHttpMsg['EntityTooLarge'] = 'Bad Request'

    # errror type 11 ExpiredToken
    errorsCode['ExpiredToken'] = 'The provided token has expired.'
    errorsHttpCode['ExpiredToken'] = 400
    errorsHttpMsg['ExpiredToken'] = 'Bad Request'

    # errror type 12 IncompleteBody
    errorsCode['IncompleteBody'] = 'You did not provide the number of bytes specified by the Content-Length HTTP header'
    errorsHttpCode['IncompleteBody'] = 400
    errorsHttpMsg['IncompleteBody'] = 'Bad Request'

    # errror type 13 IncorrectNumberOfFilesInPostRequest
    errorsCode['IncorrectNumberOfFilesInPostRequest'] = 'POST requires exactly one file upload per request.'
    errorsHttpCode['IncorrectNumberOfFilesInPostRequest'] = 400
    errorsHttpMsg['IncorrectNumberOfFilesInPostRequest'] = 'Bad Request'

    # errror type 14 InlineDataTooLarge
    errorsCode['InlineDataTooLarge'] = 'Inline data exceeds the maximum allowed size.'
    errorsHttpCode['InlineDataTooLarge'] = 400
    errorsHttpMsg['InlineDataTooLarge'] = 'Bad Request'

    # errror type 15 InternalError
    errorsCode['InternalError'] = 'We encountered an internal error. Please try again.'
    errorsHttpCode['InternalError'] = 500
    errorsHttpMsg['InternalError'] = 'Internal Server Error'

    # errror type 16 InvalidAccessKeyId
    errorsCode['InvalidAccessKeyId'] = 'The AWS Access Key Id you provided does not exist in our records.'
    errorsHttpCode['InvalidAccessKeyId'] = 403
    errorsHttpMsg['InvalidAccessKeyId'] = 'Forbidden'

    # errror type 18 InvalidArgument
    errorsCode['InvalidArgument'] = 'Invalid Argument'
    errorsHttpCode['InvalidArgument'] = 400
    errorsHttpMsg['InvalidArgument'] = 'Bad Request'

    # errror type 19 InvalidBucketName
    errorsCode['InvalidBucketName'] = 'The specified bucket is not valid.'
    errorsHttpCode['InvalidBucketName'] = 400
    errorsHttpMsg['InvalidBucketName'] = 'Bad Request'

    # errror type 20 InvalidDigest
    errorsCode['InvalidDigest'] = 'The Content-MD5 you specified was an invalid.'
    errorsHttpCode['InvalidDigest'] = 400
    errorsHttpMsg['InvalidDigest'] = 'Bad Request'

    # errror type 21 InvalidLocationConstraint
    errorsCode['InvalidLocationConstraint'] = 'The specified location constraint is not valid.'
    errorsHttpCode['InvalidLocationConstraint'] = 400
    errorsHttpMsg['InvalidLocationConstraint'] = 'Bad Request'

    # errror type 22 InvalidPayer
    errorsCode['InvalidPayer'] = 'All access to this object has been disabled.'
    errorsHttpCode['InvalidPayer'] = 403
    errorsHttpMsg['InvalidPayer'] = 'Forbidden'

    # errror type 23 InvalidPolicyDocument
    errorsCode['InvalidPolicyDocument'] = 'The content of the form does not meet the conditions specified in the policy document.'
    errorsHttpCode['InvalidPolicyDocument'] = 400
    errorsHttpMsg['InvalidPolicyDocument'] = 'Bad Request'

    # errror type 24 InvalidRange
    errorsCode['InvalidRange'] = 'The requested range cannot be satisfied.'
    errorsHttpCode['InvalidRange'] = 416
    errorsHttpMsg['InvalidRange'] = 'Requested Range Not Satisfiable'

    # errror type 25 InvalidSecurity
    errorsCode['InvalidSecurity'] = 'The provided security credentials are not valid.'
    errorsHttpCode['InvalidSecurity'] = 403
    errorsHttpMsg['InvalidSecurity'] = 'Forbidden'

    # errror type 26 InvalidSOAPRequest
    errorsCode['InvalidSOAPRequest'] = 'The SOAP request body is invalid.'
    errorsHttpCode['InvalidSOAPRequest'] = 400
    errorsHttpMsg['InvalidSOAPRequest'] = 'Bad Request'

    # errror type 27 InvalidStorageClass
    errorsCode['InvalidStorageClass'] = 'The storage class you specified is not valid.'
    errorsHttpCode['InvalidStorageClass'] = 400
    errorsHttpMsg['InvalidStorageClass'] = 'Bad Request'

    # errror type 28 InvalidTargetBucketForLogging
    errorsCode['InvalidTargetBucketForLogging'] = 'The target bucket for logging does not exist, is not owned by you, or does not have the appropriate grants for the log-delivery group.'
    errorsHttpCode['InvalidTargetBucketForLogging'] = 400
    errorsHttpMsg['InvalidTargetBucketForLogging'] = 'Bad Request'

    # errror type 29 InvalidToken
    errorsCode['InvalidToken'] = 'The provided token is malformed or otherwise invalid.'
    errorsHttpCode['InvalidToken'] = 400
    errorsHttpMsg['InvalidToken'] = 'Bad Request'

    # errror type 30 InvalidURI
    errorsCode['InvalidURI'] = 'Couldn\'t parse the specified URI.'
    errorsHttpCode['InvalidURI'] = 400
    errorsHttpMsg['InvalidURI'] = 'Bad Request'

    # errror type 31 KeyTooLong
    errorsCode['KeyTooLong'] = 'Your key is too long.'
    errorsHttpCode['KeyTooLong'] = 400
    errorsHttpMsg['KeyTooLong'] = 'Bad Request'

    # errror type 32 MalformedACLError
    errorsCode['MalformedACLError'] = 'The XML you provided was not well-formed or did not validate against our published schema.'
    errorsHttpCode['MalformedACLError'] = 400
    errorsHttpMsg['MalformedACLError'] = 'Bad Request'

    # errror type 33 MalformedACLError
    errorsCode['MalformedACLError'] = 'The XML you provided was not well-formed or did not validate against our published schema.'
    errorsHttpCode['MalformedACLError'] = 400
    errorsHttpMsg['MalformedACLError'] = 'Bad Request'

    # errror type 34 MalformedPOSTRequest
    errorsCode['MalformedPOSTRequest'] = 'The body of your POST request is not well-formed multipart/form-data.'
    errorsHttpCode['MalformedPOSTRequest'] = 400
    errorsHttpMsg['MalformedPOSTRequest'] = 'Bad Request'

    # errror type 35 MaxMessageLengthExceeded
    errorsCode['MaxMessageLengthExceeded'] = 'Your request was too big.'
    errorsHttpCode['MaxMessageLengthExceeded'] = 400
    errorsHttpMsg['MaxMessageLengthExceeded'] = 'Bad Request'

    # errror type 36 MaxPostPreDataLengthExceededError
    errorsCode['MaxPostPreDataLengthExceededError'] = 'Your POST request fields preceding the upload file were too large.'
    errorsHttpCode['MaxPostPreDataLengthExceededError'] = 400
    errorsHttpMsg['MaxPostPreDataLengthExceededError'] = 'Bad Request'

    # errror type 37 MetadataTooLarge
    errorsCode['MetadataTooLarge'] = 'Your metadata headers exceed the maximum allowed metadata size.'
    errorsHttpCode['MetadataTooLarge'] = 400
    errorsHttpMsg['MetadataTooLarge'] = 'Bad Request'

    # errror type 38 MethodNotAllowed
    errorsCode['MethodNotAllowed'] = 'The specified method is not allowed against this resource.'
    errorsHttpCode['MethodNotAllowed'] = 405
    errorsHttpMsg['MethodNotAllowed'] = 'Method Not Allowed'

    # errror type 40 MissingContentLength
    errorsCode['MissingContentLength'] = 'You must provide the Content-Length HTTP header.'
    errorsHttpCode['MissingContentLength'] = 411
    errorsHttpMsg['MissingContentLength'] = 'Length Required'

    # errror type 41 MissingSecurityElement
    errorsCode['MissingSecurityElement'] = 'The SOAP'
    errorsHttpCode['MissingSecurityElement'] = 1.1
    errorsHttpMsg['MissingSecurityElement'] = 'request is missing a security element. 400 Bad Request'

    # errror type 42 MissingSecurityHeader
    errorsCode['MissingSecurityHeader'] = 'Your request was missing a required header.'
    errorsHttpCode['MissingSecurityHeader'] = 400
    errorsHttpMsg['MissingSecurityHeader'] = 'Bad Request'

    # errror type 43 NoLoggingStatusForKey
    errorsCode['NoLoggingStatusForKey'] = 'There is no such thing as a logging status sub-resource for a key.'
    errorsHttpCode['NoLoggingStatusForKey'] = 400
    errorsHttpMsg['NoLoggingStatusForKey'] = 'Bad Request'

    # errror type 44 NoSuchBucket
    errorsCode['NoSuchBucket'] = 'The specified bucket does not exist.'
    errorsHttpCode['NoSuchBucket'] = 404
    errorsHttpMsg['NoSuchBucket'] = 'Not Found'

    # errror type 45 NoSuchKey
    errorsCode['NoSuchKey'] = 'The specified key does not exist.'
    errorsHttpCode['NoSuchKey'] = 404
    errorsHttpMsg['NoSuchKey'] = 'Not Found'

    # errror type 46 NotImplemented
    errorsCode['NotImplemented'] = 'A header you provided implies functionality that is not implemented.'
    errorsHttpCode['NotImplemented'] = 501
    errorsHttpMsg['NotImplemented'] = 'Not Implemented'

    # errror type 47 NotSignedUp
    errorsCode['NotSignedUp'] = 'Your account is not signed up for the Amazon S3 service. You must sign up before you can use Amazon S3. You can sign up at the following URL: http://aws.amazon.com/s3'
    errorsHttpCode['NotSignedUp'] = 403
    errorsHttpMsg['NotSignedUp'] = 'Forbidden'

    # errror type 48 OperationAborted
    errorsCode['OperationAborted'] = 'A conflicting conditional operation is currently in progress against this resource. Please try again.'
    errorsHttpCode['OperationAborted'] = 409
    errorsHttpMsg['OperationAborted'] = 'Conflict'

    # errror type 49 PermanentRedirect
    errorsCode['PermanentRedirect'] = 'The bucket you are attempting to access must be addressed using the specified endpoint. Please send all future requests to this endpoint.'
    errorsHttpCode['PermanentRedirect'] = 301
    errorsHttpMsg['PermanentRedirect'] = 'Moved Permanently'

    # errror type 50 PreconditionFailed
    errorsCode['PreconditionFailed'] = 'At least one of the pre-conditions you specified did not hold.'
    errorsHttpCode['PreconditionFailed'] = 412
    errorsHttpMsg['PreconditionFailed'] = 'Precondition Failed'

    # errror type 51 Redirect
    errorsCode['Redirect'] = 'Temporary redirect.'
    errorsHttpCode['Redirect'] = 307
    errorsHttpMsg['Redirect'] = 'Moved Temporarily'

    # errror type 52 RequestIsNotMultiPartContent
    errorsCode['RequestIsNotMultiPartContent'] = 'Bucket POST must be of the enclosure-type multipart/form-data.'
    errorsHttpCode['RequestIsNotMultiPartContent'] = 400
    errorsHttpMsg['RequestIsNotMultiPartContent'] = 'Bad Request'

    # errror type 53 RequestTimeout
    errorsCode['RequestTimeout'] = 'Your socket connection to the server was not read from or written to within the timeout period.'
    errorsHttpCode['RequestTimeout'] = 400
    errorsHttpMsg['RequestTimeout'] = 'Bad Request'

    # errror type 54 RequestTimeTooSkewed
    errorsCode['RequestTimeTooSkewed'] = 'The difference between the request time and the server\'s time is too large.'
    errorsHttpCode['RequestTimeTooSkewed'] = 403
    errorsHttpMsg['RequestTimeTooSkewed'] = 'Forbidden'

    # errror type 55 RequestTorrentOfBucketError
    errorsCode['RequestTorrentOfBucketError'] = 'Requesting the torrent file of a bucket is not permitted.'
    errorsHttpCode['RequestTorrentOfBucketError'] = 400
    errorsHttpMsg['RequestTorrentOfBucketError'] = 'Bad Request'

    # errror type 56 SignatureDoesNotMatch
    errorsCode['SignatureDoesNotMatch'] = 'The request signature we calculated does not match the signature you provided. Check your AWS Secret Access Key and signing method. For more information, see Authenticating REST Requests and Authenticating SOAP Requests for details.'
    errorsHttpCode['SignatureDoesNotMatch'] = 403
    errorsHttpMsg['SignatureDoesNotMatch'] = 'Forbidden'

    # errror type 57 SlowDown
    errorsCode['SlowDown'] = 'Please reduce your request rate.'
    errorsHttpCode['SlowDown'] = 503
    errorsHttpMsg['SlowDown'] = 'Service Unavailable'

    # errror type 58 TemporaryRedirect
    errorsCode['TemporaryRedirect'] = 'You are being redirected to the bucket while DNS updates.'
    errorsHttpCode['TemporaryRedirect'] = 307
    errorsHttpMsg['TemporaryRedirect'] = 'Moved Temporarily'

    # errror type 59 TokenRefreshRequired
    errorsCode['TokenRefreshRequired'] = 'The provided token must be refreshed.'
    errorsHttpCode['TokenRefreshRequired'] = 400
    errorsHttpMsg['TokenRefreshRequired'] = 'Bad Request'

    # errror type 60 TooManyBuckets
    errorsCode['TooManyBuckets'] = 'You have attempted to create more buckets than allowed.'
    errorsHttpCode['TooManyBuckets'] = 400
    errorsHttpMsg['TooManyBuckets'] = 'Bad Request'

    # errror type 61 UnexpectedContent
    errorsCode['UnexpectedContent'] = 'This request does not support content.'
    errorsHttpCode['UnexpectedContent'] = 400
    errorsHttpMsg['UnexpectedContent'] = 'Bad Request'

    # errror type 62 UnresolvableGrantByEmailAddress
    errorsCode['UnresolvableGrantByEmailAddress'] = 'The e-mail address you provided does not match any account on record.'
    errorsHttpCode['UnresolvableGrantByEmailAddress'] = 400
    errorsHttpMsg['UnresolvableGrantByEmailAddress'] = 'Bad Request'

    # errror type 63 UserKeyMustBeSpecified
    errorsCode['UserKeyMustBeSpecified'] = 'The bucket POST must contain the specified field name. If it is specified, please check the order of the fields.'
    errorsHttpCode['UserKeyMustBeSpecified'] = 400
    errorsHttpMsg['UserKeyMustBeSpecified'] = 'Bad Request'

    def __init__(self, code, ex=None):
        self.code = code 
        self.ex = ex
        self.custom_xml = {}
        try:
            self.httpCode = cbException.errorsHttpCode[code]
            self.httpDesc = cbException.errorsHttpMsg[code]
            self.eMsg = cbException.errorsCode[code]
        except:
            self.httpCode = 400
            self.httpDesc = 'ERROR'
            self.eMsg = 'something bad happened when creating the error message'

    def __str__(self):
        return self.eMsg

    def getCode(self):
        return self.code

    def add_custom_xml(self, k, v):
        self.custom_xml[k] = v

    def make_xml_string(self, path, requestId):
        doc = Document()

        # Create the <wml> base element
        xError = doc.createElement("Error")
        doc.appendChild(xError)

        # Create the main <card> element
        xCode = doc.createElement("Code")
        xCodeText = doc.createTextNode(self.code)
        xCode.appendChild(xCodeText)
        xError.appendChild(xCode)

        xMsg = doc.createElement("Message")
        xMsgText = doc.createTextNode(self.eMsg)
        xMsg.appendChild(xMsgText)
        xError.appendChild(xMsg)

        xRsc = doc.createElement("Resource")
        xRscText = doc.createTextNode(path)
        xRsc.appendChild(xRscText)
        xError.appendChild(xRsc)

        xRId = doc.createElement("RequestId")
        xRIdText = doc.createTextNode(str(requestId))
        xRId.appendChild(xRIdText)
        xError.appendChild(xRId)

        for k in self.custom_xml.keys():
            xId = doc.createElement(k)
            xText = doc.createTextNode(self.custom_xml[k])
            xId.appendChild(xText)
            xError.appendChild(xId)

        return doc.toxml()


    def sendErrorResponse(self, request, requestId):

        try:
            request.setHeader('x-amz-request-id', str(requestId))
            request.setHeader('x-amz-id-2:', str(uuid.uuid1()))

            request.setResponseCode(self.httpCode, self.httpDesc)
            # Create the minidom document
            xml = self.make_xml_string(request.path, requestId)
            request.write(xml)
            request.finish()

            return xml
        except:
            # XXX LOG ERROR 
            pycb.log(logging.ERROR, sys.exc_info()[0], traceback)
            return cbException.panicError    
