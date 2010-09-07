import os
import sys
from pycb.cbException import cbException
import pycb
import stat
import urllib
import glob
import errno
import logging
import threading
import tempfile
import hashlib
import traceback
import time

class cbBackendInterface(object):

    # create and return a DataObject for writing
    # bucketName and objectName will likely just be used for seeding
    # internal names, the plug should not need these values
    def put_object(self, bucketName, objectName):
        return obj


    # copy an object, not yet implemented
    def copy_object(self, srcObjectName, dstObjectName, moveOrCopy, httpHeaders):
        pass


    # find and return a dataobject with the given key for reading
    def get_object(self, data_key):
        obj = cbPosixData(data_key, "r")
        return obj


    # delete the data in the given datakey
    def delete_object(self, data_key):
        pass

    # return the size of the dataset associated with the given key
    def get_size(self, data_key):
        pass

    # get the modification time
    def get_mod_time(self, data_key):
        pass

    # get the md5 sum of the file
    def get_md5(self, data_key):
        pass

class cbDataObject(object):

    def get_data_key(self):
        pass

    # this is part of the work around for twisted
    def set_delete_on_close(self, delete_on_close):
        pass

    # implement file-like methods
    def close(self):
        pass

    def flush(self):
        pass

    #def fileno(self):
    #def isatty(self):

    def next(self):
        pass

    def read(self, size=None):
        pass

#    def readline(self, size=None):
#    def readlines(self, size=None):
#    def xreadlines(self):

    def seek(self, offset, whence=None):
        pass

#    def tell(self):
#    def truncate(self, size=None):

    def write(self, st):
        pass

    def writelines(self, seq):
        pass

