#Copyright 1999-2007 University of Chicago
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

class partition:
    def __init__(self):
        self.givenpath = None
        # just the path after scheme
        self.path = None
        # scheme does not include "://"
        self.scheme = None
        
        self.relativepath = False
        self.needspropagation = False
        self.blankspace = 0
        self.isreadonly = False
        self.fromcache = False

