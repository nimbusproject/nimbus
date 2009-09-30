#Copyright 1999-2006 University of Chicago
#
#Licensed under the Apache License, Version 2.0 (the "License"); you may not
#use this file except in compliance with the License. You may obtain a copy
#of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
#Unless required by applicable law or agreed to in writing, software
#distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
#WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
#License for the specific language governing permissions and limitations
#under the License.

class parameters:
    """all implementations provide a registration method for cmdline options.
    If they are no-ops, they just don't override.  Later, we can do the object
    hierarchy 'correctly'."""
    
    def __init__(self, conffile, action):
        pass
        
    def set_name(self, name):
        pass
        
    def set_images(self, img):
        pass
        
    def set_imagemounts(self, imgmnt):
        pass
        
    def set_kernel(self, kernel):
        pass
        
    def set_kernelarguments(self, args):
        pass
        
    def set_ramdisk(self, ramdisk):
        pass
        
    def set_persistencedir(self, persistencedir):
        pass
        
    def set_networking(self, networking):
        pass
        
    def set_memory(self, memory):
        pass
        
    def set_checkshutdown(self, shut):
        pass
        
    def set_checkshutdownpause(self, shutpause):
        pass
        
    def set_notify(self, notify):
        pass
        
    def set_logfile(self, logfilepath, logfilehandler):
        pass
        
    def set_startpaused(self, startpaused):
        pass
        
    def set_deleteall(self, delete):
        pass
    
    def set_mnttasks(self, mnttasks):
        pass
    
    def set_unproptargets(self, unproptargets):
        pass
    
        
