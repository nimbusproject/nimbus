import traceback
import sys
import uuid
import pycb
import logging
from xml.dom.minidom import Document

class cbToolsException(Exception):

    errors = {}
    rcs = {}

    errors['UNKNOWN_USER'] = "The user %s is unknown"
    rcs['UNKNOWN_USER'] = 1
    errors['CMDLINE'] = "poorly formed command line : %s"
    rcs['CMDLINE'] = 2

    def __init__(self, code, fmt_set, ex=None):
        self.code = code 
        self.ex = ex
        self.msg = cbToolsException.errors[self.code] % fmt_set
        self.rc = cbToolsException.rcs[self.code]

    def get_code(self):
        return self.code

    def get_msg(self):
        return self.msg

    def get_exception(self):
        return self.ex

    def get_rc(self):
        return self.rc

    def __str__(self):
        msg = self.msg
        if self.ex != None:
            msg = msg + " : " + str(self.ex)
        return msg

