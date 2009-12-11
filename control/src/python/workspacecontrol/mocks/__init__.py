"""Mock objects for nimbuscontrol tests.

Libraries used by this module or any test files are not runtime requirements
for the program: tests are never invoked during normal operation or 
installation, they are for developers only.

Nose testing is used (easy_install nose).
"""

import os
from workspacecontrol.api.exceptions import IncompatibleEnvironment

from TestProcurement import TestProcurement
from NetworkLease import NetworkLease

def get_mock_mainconf(basename="main.conf"):
    # this can be an unintuitive value
    current = os.path.abspath(__file__)
    
    # ... so we find sanity by relying on no package name but one ever being
    # named "workspacecontrol".  While loop can start by going "up one" since
    # the original value of "current" ends with this file, not a directory.
    while True:
        current = "/".join(os.path.dirname(current+"/").split("/")[:-1])
        if os.path.basename(current) == "workspacecontrol":
            return os.path.join(current, "mocks/etc/%s" % basename)
        if not os.path.basename(current):
            raise IncompatibleEnvironment("cannot find mock confs")
    

