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
 * or Ian Gable - igable@uvic.ca
 *
 * """


import os
import sys
import resource
import subprocess
from subprocess import *
import time
import ConfigParser


CONF_FILE = "/usr/local/nagios/libexec/monitoring_config.cfg"
CONF_FILE_SECTION = "Nimbus_Monitoring"
NIMBUS_ADDRESS = "Nimbus_Server_Address"
NIMBUS_LOCATION = "Nimbus_Install_Location"
GLOBUS_LOCATION = "Globus_Install_Location"
SERVER_TMP_LOCATION = "Server_Tmp_Location"
NAGIOS_LOCATION = "Nagios_Location"
JAVA_LOCATION = "Java_Location"
IJ_LOCATION = "IJ_Location"
DERBY_LOCATION = "Derby_Location"
ConfigMapping = {}



REG_FILE = "mdsVirtReg.xml"
PID_FILE = "nimbusMDSReg.pid"

# Since this script is NOT invoked by NAGIOS as a plug-in, environment variables may
# be used in the subprocess.Popen call below without error

def loadNimbusConfig():

    cfgFile = ConfigParser.ConfigParser()
    if(os.path.exists(CONF_FILE)):
        cfgFile.read(CONF_FILE)
        try:
            ConfigMapping[NIMBUS_ADDRESS] = cfgFile.get(CONF_FILE_SECTION,NIMBUS_ADDRESS,0)
            ConfigMapping[NIMBUS_LOCATION] = cfgFile.get(CONF_FILE_SECTION,NIMBUS_LOCATION,0)
            ConfigMapping[SERVER_TMP_LOCATION] = cfgFile.get(CONF_FILE_SECTION, SERVER_TMP_LOCATION,0)
            ConfigMapping[NAGIOS_LOCATION] = cfgFile.get(CONF_FILE_SECTION, NAGIOS_LOCATION,0)
            ConfigMapping[JAVA_LOCATION] = cfgFile.get(CONF_FILE_SECTION, JAVA_LOCATION,0)
            ConfigMapping[IJ_LOCATION] = cfgFile.get(CONF_FILE_SECTION,IJ_LOCATION,0)
            ConfigMapping[GLOBUS_LOCATION] = cfgFile.get(CONF_FILE_SECTION,GLOBUS_LOCATION,0)
            ConfigMapping[DERBY_LOCATION] = cfgFile.get(CONF_FILE_SECTION,DERBY_LOCATION,0)
        except ConfigParser.NoSectionError:
            print "Unable to locate "+CONF_FILE_SECTION+" section in conf file - Malformed config file?"
            sys.exit(-1)
        except ConfigParser.NoOptionError, nopt:
            print nopt.message+" of configuration file"
            sys.exit(-1)
    else:
        print "Configuration file not found in current directory"
        sys.exit(-1)


def mdsRegister():
    pidPath = ConfigMapping[SERVER_TMP_LOCATION] + PID_FILE
    try:
        if(os.path.exists(pidPath)):
            try:
                pidFile = open(pidPath,"r")
                pid = pidFile.readline()
                pidFile.close()
                print "A PID file exists (another instance is running) with a PID of: "+pid
                sys.exit(0)    
            except IOError:
                print >>sys.stderr, "Error opening the PID file for reading at: ",pidPath
                sys.exit(-1)
        else:
            pid = os.getpid()
            try:
                pidFile = open(pidPath,"w")
                pidFile.write(str(pid))
                pidFile.close()
            except IOError:
                print >>sys.stderr, "Error opening the PID file for writing at: ",pidPath
                sys.exit(-1)
        
            argString = ConfigMapping[NIMBUS_ADDRESS] + " " + REG_FILE 
            try:
                process = subprocess.Popen(ConfigMapping[NIMBUS_LOCATION]+"/bin/mds-servicegroup-add -s "+argString , shell=True,stderr=PIPE, stdout=PIPE)
                                                 
                output = process.communicate()
                # The above statement never finishes executing (such is the nature of mds-servicegroup-add)
                print ConfigMapping[NIMBUS_LOCATION]+"/bin/mds-servicegroup-add -s "+argString 
                # This code snippet exists in case of some sort of error in the sub-process
                # Graceful cleanup (removing a defunct pid file) is desired
                if(os.path.exists(pidPath)):
                    os.remove(pidPath)
            except OSError, e:
                print >>sys.stderr,"OSError encountered within the subprocess: ",e
                if(os.path.exists(pidPath)):
                    os.remove(pidPath)
                sys.exit(-1)

    except KeyboardInterrupt:
        print "KeyboardInterrupt encountered - Terminating gracefully"
        if(os.path.exists(pidPath)):
                    os.remove(pidPath)
loadNimbusConfig()
mdsRegister()
sys.exit(0)
