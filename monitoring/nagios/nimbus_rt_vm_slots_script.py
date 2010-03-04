#!/usr/bin/python

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


__VERSION__ = '0.01'

import sys
import commands
import os
import logging
from cStringIO import StringIO
from optparse import OptionParser
import subprocess
import commands
import re
import socket
import time
import ConfigParser
import syslog
# NAGIOS Plug-In API return code values

RET_OK = 0
RET_CRITICAL = -1

SQL_IP_SCRIPT = "/usr/local/nagios/libexec/nimbus_derby_used_ips.sql"
SQL_RUNNING_VM_SCRIPT = "/usr/local/nagios/libexec/nimbus_derby_running_vms.sql"

#The "NIMBUS_" entries are relative to the ENV_GLOBUS_LOC var
NIMBUS_CONF = "/etc/nimbus/workspace-service"
NIMBUS_NET_CONF = "/network-pools"
NIMBUS_PHYS_CONF = "/vmm-pools"

CONF_FILE = "/usr/local/nagios/libexec/monitoring_config.cfg"
NM_CONF_FILE_SECTION = "Nimbus_Monitoring"
NIMBUS_ADDRESS = "Nimbus_Server_Address"
NIMBUS_LOCATION = "Nimbus_Install_Location"
GLOBUS_LOCATION = "Globus_Install_Location"
SERVER_TMP_LOCATION = "Server_Tmp_Location"
NAGIOS_LOCATION = "Nagios_Location"
JAVA_LOCATION = "Java_Location"
IJ_LOCATION = "IJ_Location"
DERBY_LOCATION = "Derby_Location"

CONF_FILE_SECTION= "Real_Time_Monitoring"
REALTIME_XML_LOCATION = "RealTime_XML_Output_Location"
REALTIME_UPDATE_INTERVAL = "RealTime_Update_Interval"

ConfigMapping = {}


# This global method loads all the user configured options from the configuration file and saves them
# into the ConfigMapping dictionary

def loadNimbusConfig(logger):

    cfgFile = ConfigParser.ConfigParser()
    if(os.path.exists(CONF_FILE)):
        cfgFile.read(CONF_FILE)
        try:
            ConfigMapping[NIMBUS_ADDRESS] = cfgFile.get(NM_CONF_FILE_SECTION,NIMBUS_ADDRESS,0)
            ConfigMapping[NIMBUS_LOCATION] = cfgFile.get(NM_CONF_FILE_SECTION,NIMBUS_LOCATION,0)
            ConfigMapping[SERVER_TMP_LOCATION] = cfgFile.get(NM_CONF_FILE_SECTION, SERVER_TMP_LOCATION,0)
            ConfigMapping[NAGIOS_LOCATION] = cfgFile.get(NM_CONF_FILE_SECTION, NAGIOS_LOCATION,0)
            ConfigMapping[JAVA_LOCATION] = cfgFile.get(NM_CONF_FILE_SECTION, JAVA_LOCATION,0)
            ConfigMapping[IJ_LOCATION] = cfgFile.get(NM_CONF_FILE_SECTION,IJ_LOCATION,0)
            ConfigMapping[GLOBUS_LOCATION] = cfgFile.get(NM_CONF_FILE_SECTION,GLOBUS_LOCATION,0)
            ConfigMapping[DERBY_LOCATION] = cfgFile.get(NM_CONF_FILE_SECTION,DERBY_LOCATION,0)

            ConfigMapping[REALTIME_XML_LOCATION] = cfgFile.get(CONF_FILE_SECTION, REALTIME_XML_LOCATION,0)
            ConfigMapping[REALTIME_UPDATE_INTERVAL] = cfgFile.get(CONF_FILE_SECTION, REALTIME_UPDATE_INTERVAL,0)

        except ConfigParser.NoSectionError:
            logger.error("Unable to locate "+CONF_FILE_SECTION+" section in conf file - Malformed config file?")
            sys.exit(RET_CRITICAL)
        except ConfigParser.NoOptionError, nopt:
            logger.error( nopt.message+" of configuration file")
            sys.exit(RET_CRITICAL)
    else:
        logger.error( "Configuration file not found in Nagios Plug-ins directory")
        sys.exit(RET_CRITICAL)

# This is a recursive function to write out an XML doc from the pluginOutput data structure
def _createXMLWorker(data, currentOutput):

    if (type(data) == type(dict())):
        # DNP == Do No Print - that is there's no "wrapping tag", just print the text itself
        if "DNP" in data.keys():
            currentOutput.write(str(data["DNP"]))
            return

        for key in data.keys():
            currentOutput.write("<"+str(key)+">")
            _createXMLWorker(data[key], currentOutput)
            currentOutput.write("</"+str(key)+">")

    elif (type(data) == type(list())):
        for key in range(len(data)):
            _createXMLWorker(data[key], currentOutput)

    else:
        currentOutput.write(str(data))
        return


def pluginExitN(logger, messageIdentifier, pluginInfo, returnCode):

    # This method should be the only exit point for all the plug-ins. This ensures that all appropriate
    # data and a proper XML output is generated and printed

    outputString = StringIO()
    outputString.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")

    localIP = (socket.gethostbyaddr( socket.gethostname() ))[2][0]
    outputString.write("<RealTime>")

    outputString.write("<"+messageIdentifier+">")
    _createXMLWorker(pluginInfo, outputString)   

    outputString.write("</"+messageIdentifier+">")
    outputString.write("</RealTime>")
    try:
        outputFile = open(ConfigMapping[REALTIME_XML_LOCATION],"w")
        outputFile.write(outputString.getvalue())
        outputFile.close()
    except IOError, err:
        logger.error("Output to "+ConfigMapping[REALTIME_XML_LOCATION]+" failed - "+str(err))
    
    print (outputString.getvalue())   

class PluginObject:    
    """ This base class sets up appropriate logging mechanisms. 
    """

    def __init__(self, callingClass):
          
        self.logger = logging.getLogger(callingClass)
        self.logger.setLevel(logging.INFO)
        formatter = logging.Formatter('%(asctime)s ; %(name)s ; %(levelname)s ; %(message)s')

        errorOutputHndlr = logging.FileHandler("rt_vm_slots.log")
        errorOutputHndlr.setFormatter(formatter)
        errorOutputHndlr.setLevel(logging.ERROR)

        self.logger.addHandler(errorOutputHndlr)
        self.pluginOutput = {}

class HeadNodeVMSlots(PluginObject):
    """ The class parses the Nimbus Network Pools configuration files to determine how many IP address slots are 
        configured for Nimbus to make use of. Also calculated is the number of free IP adresses or "VM Slots" based
        on what VMs are active
    """
    def __init__(self):
        PluginObject.__init__(self, self.__class__.__name__)
        loadNimbusConfig(self.logger)
        self.resourceName = "Network-Pools"

    def __call__(self): 
 
        try:
            netPools = os.listdir(ConfigMapping[NIMBUS_LOCATION]+NIMBUS_CONF+NIMBUS_NET_CONF)
        except OSError, ose:
            self.logger.error("Error listing the Network Pools directory: "+ str(ose))
            sys.exit(RET_CRITICAL)
        totalNetPools = []
        for pool in netPools:

            # Ignore . and .. entries in the file system
            if(pool.startswith(".")):
                continue
            netPoolData = {}
            netPoolData["ID"] = pool
            try:
                fileHandle = open(ConfigMapping[NIMBUS_LOCATION]+NIMBUS_CONF+NIMBUS_NET_CONF+"/"+pool)
                VMNetConfig = []
                
                for entry in fileHandle:
                    if(entry.startswith("#") or entry.isspace()):
                        continue
                    t = entry.split()
                    # This looks for the DNS server entry and skips over it
                    # The config file stipulates that each line in the file must have
                    # 5 entries for the net config, so I can use this condition to 
                    # identify the lone DNS entry line
                    if(len(t) < 5):
                        continue
                    VMNetConfig.append(t)
                
                netPoolData["NETWORK"] = VMNetConfig                
                fileHandle.close()
                totalNetPools.append(netPoolData)
            except IOError:
                self.logger.error("Error opening network-pool: "+ConfigMapping[NIMBUS_LOCATION]+NIMBUS_CONF+NIMBUS_NET_CONF+"/"+pool)
                sys.exit(RET_CRITICAL)

        query = ConfigMapping[IJ_LOCATION]+ " "+SQL_IP_SCRIPT
        output,status = (subprocess.Popen([query],stdout = subprocess.PIPE, stderr = subprocess.PIPE, shell=True, env={'DERBY_HOME':ConfigMapping[DERBY_LOCATION],'JAVA_HOME':ConfigMapping[JAVA_LOCATION],'GLOBUS_HOME':ConfigMapping[NIMBUS_LOCATION]})).communicate()
        
        # Use a regex to find the lines containing IP addresses - this eases parsing greatly
        derbyIPs = []
        patt = re.compile(r"(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})")
        for line in output.split():
            myRe = patt.search(line)
            if(myRe):
                derbyIPs.append(line.strip())

        #reinitialize pluginOutput to an array, not a Dict (default)
        self.pluginOutput = []
        for pool in totalNetPools:
                      
            available = len(pool["NETWORK"])
            for entry in pool["NETWORK"]:
                for allocIP in derbyIPs: 
                    if (entry[1] == allocIP):
                        available -= 1
            self.pluginOutput.append({"Pool":{"Name": str(pool["ID"]), "AvailableIPs": str(available), "TotalIPs":str(len(pool["NETWORK"]))}})
            
        pluginExitN(self.logger, self.resourceName,self.pluginOutput, RET_OK)

if __name__ == '__main__':

    activeSlots = HeadNodeVMSlots()
    while True:

        activeSlots() 
        time.sleep(int(ConfigMapping[REALTIME_UPDATE_INTERVAL]))
    # This sys.exit call should NEVER be reached under normal circumstances, or any....
    sys.exit(RET_CRITICAL)
