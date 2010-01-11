#! /usr/bin/python

"""*
 * Copyright 2009 University of Victoria
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * AUTHOR - Adam Bishop - ahbishop@uvic.ca
 *       
 * For comments or questions please contact the above e-mail address 
 * OR    
 * HEPNet Technical Manager - Ian Gable - igable@uvic.ca
 *
 * """


import os
import sys
import resource
import subprocess
from subprocess import *
import time

REG_FILE = "mdsVirtReg.xml"

#Update this for your respective server location/address
SERVER_ADDRESS = "https://vmcgs29.phys.uvic.ca:8443/wsrf/services/DefaultIndexService"
PID_PATH = "/tmp/nimbusMDSReg.pid"

# Since this script is NOT invoked by NAGIOS as a plug-in, environment variables may
# be used in the subprocess.Popen call below without error

def mdsRegister():
    try:
        if(os.path.exists(PID_PATH)):
            try:
                pidFile = open(PID_PATH,"r")
                pid = pidFile.readline()
                pidFile.close()
                print "A PID file exists (another instance is running) with a PID of: "+pid
                sys.exit(0)    
            except IOError:
                print >>sys.stderr, "Error opening the PID file for reading at: ",PID_PATH
                sys.exit(-1)
        else:
            pid = os.getpid()
            try:
                pidFile = open(PID_PATH,"w")
                pidFile.write(str(pid))
                pidFile.close()
            except IOError:
                print >>sys.stderr, "Error opening the PID file for writing at: ",PID_PATH
                sys.exit(-1)
        
            argString = SERVER_ADDRESS + " " + REG_FILE 
            try:
                process = subprocess.Popen("$GLOBUS_LOCATION/bin/mds-servicegroup-add -s "+argString , shell=True,stderr=PIPE, stdout=PIPE)
                                
                output = process.communicate()
                # The above statement never finishes executing (such is the nature of mds-servicegroup-add)
                
                # This code snippet exists in case of some sort of error in the sub-process
                # Graceful cleanup (removing a defunct pid file) is desired
                if(os.path.exists(PID_PATH)):
                    os.remove(PID_PATH)
            except OSError, e:
                print >>sys.stderr,"OSError encountered within the subprocess: ",e
                if(os.path.exists(PID_PATH)):
                    os.remove(PID_PATH)
                sys.exit(-1)

    except KeyboardInterrupt:
        print "KeyboardInterrupt encountered - Terminating gracefully"
        if(os.path.exists(PID_PATH)):
                    os.remove(PID_PATH)

mdsRegister()
sys.exit(0)
