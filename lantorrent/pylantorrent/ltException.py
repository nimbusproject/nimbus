import sys
import socket
try:
    import json
except ImportError:
    import simplejson as json
import os

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
    errorsCode[510] = "Incorrect checksum %s"

    def __init__(self, code, msg, host=None, port=None, reqs=None, md5sum=""):
        self.code = code
        self.host = host
        self.port = port
        self.reqs = reqs
        self.msg = LTException.errorsCode[code] % (msg)
        self.md5sum = md5sum

    def __str__(self):
         return "%d %s:%s%s %s\r\n" % (self.code, str(self.host), str(self.port), str(self.reqs), self.msg)

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
    def get_json(self, rid=None, filename=None):
        header = {}
        header['code'] = self.code
        header['host'] = self.host
        header['port'] = self.port
        header['file'] = filename
        header['id'] = rid
        header['message'] = self.msg
        header['md5sum'] = self.md5sum

        return header


    def get_printable(self):
        if self.reqs == None:
            s = self.get_json()
            return json.dumps(s)

        str_out = ""
        for req in self.reqs:
            s = self.get_json(rid=req['id'], filename=req['filename'])
            s = json.dumps(s)
            str_out = str_out + s + os.linesep

        return str_out
