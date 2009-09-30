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

class WorkspaceError(Exception):
    pass
    
class UsageError(WorkspaceError):
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg
        
class EnvironmentProblem(WorkspaceError):
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg
        
class NoValue(WorkspaceError):
    pass
    
class NoInfo(WorkspaceError):
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg
        
class RuntimeProblem(WorkspaceError):
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg