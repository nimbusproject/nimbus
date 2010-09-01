import sys
import socket
import json

class LTException(Exception):

    errorsCode = {}

    errorsCode[0] = "Success %s"
    errorsCode[500] = "An unknown error happened %s"
    errorsCode[501] = "The header is too long %s"
    errorsCode[502] = "The header does not contain a needed value %s"
    errorsCode[503] = "The output file could not be opened %s"
    errorsCode[504] = "The header contains an invalid destination entry %s"
    errorsCode[505] = "Failed to connect to the next endpoint %s"
    errorsCode[506] = "A connection error occured on send %s"
    errorsCode[507] = "A peer returned non json output %s"
    errorsCode[508] = "Access denied: %s"
    errorsCode[509] = "completion status never recieved %s"

    def __init__(self, code, msg, host=None, port=None, filename=None, rid=None):
        self.code = code
        self.host = host
        self.port = port
        self.filename = filename
        self.rid = rid
        self.msg = VirgaException.errorsCode[code] % (msg)

    def __str__(self):
         return "%s %d %s:%s%s %s\r\n" % (self.rid, self.code, str(self.host), str(self.port), self.filename, self.msg)

    #
    #  results json
    #
    #  {
    #     results = 
    #       [
    #           {
    #               code
    #               host
    #               port
    #               file
    #               id
    #               message
    #           }
    #       ]
    #  }
    def get_json(self):
        header = {}
        header['code'] = self.code
        header['host'] = self.host
        header['port'] = self.port
        header['file'] = self.filename
        header['id'] = self.rid
        header['message'] = self.msg

        return header

