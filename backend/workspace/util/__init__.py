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

import os

def bashEscape(cmd):
    """returns \ escapes for some bash special characters"""
    if not cmd:
        return cmd
    escs = "\\'`|;()?#$^&*="
    for e in escs:
        idx = 0
        ret = 0
        while ret != -1:
            ret = cmd.find(e, idx)
            if ret >= 0:
                cmd = "%s\%s" % (cmd[:ret],cmd[ret:])
                idx = ret + 2
    return cmd

def removeDir(path, expectfiles=False, onerror=None):
    """Recursively remove a directory.  If expectfiles is False,
       will stop if it finds any non-directory files"""
       
    if not os.path.isdir(path):
        raise OSError("'%s' is not a dir, aborting" % path)
       
    for root, dirs, files in os.walk(path, topdown=False, onerror=onerror):
        if files and not expectfiles:
            raise OSError("files found, aborting")
        for name in files:
            os.remove(os.path.join(root, name))
        for name in dirs:
            os.rmdir(os.path.join(root, name))
    
    os.rmdir(path)



