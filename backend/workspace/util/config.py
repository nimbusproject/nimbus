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

from workspace.err import * 

def valueResolve(config, family, section, key):
    """Resolves values from overriding sections of configuration file"""
    
    result = None
    
    # First try family
    if family:
        try:
            result = config.get(section+"-"+family,key)
            return result
        except:
            pass
    # If not in family specific section, try regular section
    try:
        result = config.get(section,key)
        return result
    except:
        raise NoValue()
    
def format(msg):
    """Formats config file and validation events"""
    return "  - " + msg + "\n"

