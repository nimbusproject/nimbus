import os
import time
from time import strftime
import stat
import pycb

class cbObject(object):

    def __init__(self, tm, size, key, display_name, user, md5sum=None, storage_class="STANDARD"):
        self.tm = tm
        self.size = size
        self.md5sum = md5sum
        self.key = key
        self.display_name = display_name
        self.user = user
        self.storage_class = storage_class

    def create_xml_element(self, doc):

        xContent = doc.createElement("Contents")

        xKey = doc.createElement("Key")
        xContent.appendChild(xKey)
        xKeyText = doc.createTextNode(str(self.key))
        xKey.appendChild(xKeyText)

        tm = self.tm
        last_mod_str = "%04d-%02d-%02dT%02d:%02d:%02d.000Z" % (tm.tm_year, tm.tm_mon, tm.tm_mday, tm.tm_hour, tm.tm_min, tm.tm_sec)
        xLastModified = doc.createElement("LastModified")
        xContent.appendChild(xLastModified)
        xLMText = doc.createTextNode(str(last_mod_str))
        xLastModified.appendChild(xLMText)
        
        xSize = doc.createElement("Size")
        xContent.appendChild(xSize)
        xSizeText = doc.createTextNode(str(self.size))
        xSize.appendChild(xSizeText)

        xStorageClass = doc.createElement("StorageClass")
        xContent.appendChild(xStorageClass)
        xStorageClassText = doc.createTextNode(self.storage_class)
        xStorageClass.appendChild(xStorageClassText)

        if self.md5sum != None:
            xETag = doc.createElement("ETag")
            xContent.appendChild(xETag)
            xETagText = doc.createTextNode(self.md5sum)
            xETag.appendChild(xETagText)

        xOwner = doc.createElement("Owner")
        xContent.appendChild(xOwner)

        xID = doc.createElement("ID")
        xOwner.appendChild(xID)
        xIDText = doc.createTextNode(str(self.user.get_id()))
        xID.appendChild(xIDText)

        xDisplayName = doc.createElement("DisplayName")
        xOwner.appendChild(xDisplayName)
        xDisplayNameText = doc.createTextNode(str(self.display_name))
        xDisplayName.appendChild(xDisplayNameText)

        return xContent

    def get_date_string(self):
        d_str = "%04d-%02d-%02dT%02d:%02d:%02d.000Z" % (self.tm.tm_year, self.tm.tm_mon, self.tm.tm_wday, self.tm.tm_hour, self.tm.tm_min, self.tm.tm_sec)
        return d_str

    def get_key(self):
        return self.key
