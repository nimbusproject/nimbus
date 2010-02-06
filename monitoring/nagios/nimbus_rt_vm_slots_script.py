#!/usr/bin/python

"""*
 * Copyright 2010 University of Victoria
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
#RET_WARNING = 1
RET_CRITICAL = -1
#RET_UNKNOWN = 3

SQL_IP_SCRIPT = "/usr/local/nagios/libexec/nimbus_derby_used_ips.sql"
SQL_RUNNING_VM_SCRIPT = "/usr/local/nagios/libexec/nimbus_derby_running_vms.sql"

#The "NIMBUS_" entries are relative to the ENV_GLOBUS_LOC var
NIMBUS_CONF = "/etc/nimbus/workspace-service"
NIMBUS_NET_CONF = "/network-pools"
NIMBUS_PHYS_CONF = "/vmm-pools"

NIMBUS_PBS_CONF = "/pilot.conf"
NIMBUS_PBS_SUPPORT = "/other/resource-locator-ACTIVE.xml"

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
            sys.exit(RET_CRITICAL)
        except ConfigParser.NoOptionError, nopt:
            print nopt.message+" of configuration file"
            sys.exit(RET_CRITICAL)
    else:
        print "Configuration file not found in Nagios Plug-ins directory"
        sys.exit(RET_CRITICAL)

def pluginExitN(messageIdentifier, pluginInfo, returnCode):

    # This method should be the only exit point for all the plug-ins. This ensures that 
    # Nagios requirements are meant and performance data is properly formatted to work
    # with the rest of the code. Do NOT just call sys.exit in the code (if you want your
    # plug-in to function with the rest of the code!


    # ALRIGHT, so the log string is seperated by  my "delimiter" ';'
    # Thus, I'm going to assume the following log style format:

    # 2009-05-29 13:48:55,638 ; VMMemory ; INFO ; sl52base ; MEMORY ; 524288

    outputString = StringIO()
    outputString.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")

    localIP = (socket.gethostbyaddr( socket.gethostname() ))[2][0]
    outputString.write("<RealTime>")
    outputString.write("<HeadNode>")
    #outputString.write("<PhysicalIP>"+localIP+"</PhysicalIP>")

    outputString.write("<"+messageIdentifier+">")
    for key in pluginInfo.keys():            
            outputString.write("<"+key.strip()+">")
            if( type(pluginInfo[key]) == type(list())):
                for val in pluginInfo[key]:
                    outputString.write("<item>"+val+"</item>")
            elif (type(pluginInfo[key]) == type(dict())):
                for secKey in pluginInfo[key].keys():
                    outputString.write("<"+secKey+">")
                    outputString.write(str(pluginInfo[key][secKey]))
                    outputString.write("</"+secKey+">")
            else:
                outputString.write(pluginInfo[key])
            outputString.write("</"+key.strip()+">") 
    outputString.write("</"+messageIdentifier+">")
    outputString.write("</HeadNode>")
    outputString.write("</RealTime>")
    try:
        outputFile = open("/tmp/activeSlots.xml","w")
        outputFile.write(outputString.getvalue())
        outputFile.close()
    except IOError, err:
        syslog.syslog( "Outputting to activeSlots.xml failed!")
    
    print (outputString.getvalue())   
    #sys.exit(returnCode)

class PluginObject:    
    """The most 'senior' of the base classes. This class sets up appropriate logging mechanisms to 
    conform with Nagios' API and plug-in coding rules. The log format is also setup, and cannot
    be changed without breaking almost all the code. Don't change the log format!
    """

    def __init__(self, callingClass):
          
        syslog.openlog(callingClass,syslog.LOG_USER)    
        #self.logger = logging.getLogger(callingClass)
        #self.logger.setLevel(logging.INFO)
        #formatter = logging.Formatter('%(asctime)s ; %(name)s ; %(levelname)s ; %(message)s')

        #errorOutputHndlr = logging.StreamHandler(sys.stdout)
        #errorOutputHndlr.setFormatter(formatter)
        #errorOutputHndlr.setLevel(logging.ERROR)

        #self.logger.addHandler(errorOutputHndlr)
        
        self.pluginOutput = {}

class HeadNodeVMSlots(PluginObject):
    """ The class parses the Nimbus Network Pools configuration files to determine how many IP address slots are 
    configured for Nimbus to make use of. This is strictly a reporting feature and does not calculate how
    many free slots there are. THat functionality is handled by the querying driver program, as there is
    some non-trivial processing that needs to occur which doesn't fit into this script's architecture
    """
    def __init__(self):
        PluginObject.__init__(self, self.__class__.__name__)
        self.resourceName = "Available-VMSlots"


    def __call__(self): 
 
        try:
            netPools = os.listdir(ConfigMapping[NIMBUS_LOCATION]+NIMBUS_CONF+NIMBUS_NET_CONF)
        except OSError, ose:
            syslog.syslog("Error listing the Network Pools directory: "+ str(ose))
            sys.exit(RET_CRITICAL)
        totalNetPools = []
        for pool in netPools:

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
                syslog.syslog("Error opening network-pool: "+ConfigMapping[NIMBUS_LOCATION]+NIMBUS_CONF+NIMBUS_NET_CONF+"/"+pool)
                sys.exit(RET_CRITICAL)

        query = ConfigMapping[IJ_LOCATION]+ " "+SQL_IP_SCRIPT
        output,status = (subprocess.Popen([query],stdout = subprocess.PIPE, stderr = subprocess.PIPE, shell=True, env={'DERBY_HOME':ConfigMapping[DERBY_LOCATION],'JAVA_HOME':ConfigMapping[JAVA_LOCATION],'GLOBUS_HOME':ConfigMapping[NIMBUS_LOCATION]})).communicate()
        
        derbyIPs = []
        patt = re.compile(r"(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})")
        for line in output.split():
            myRe = patt.search(line)
            if(myRe):
                derbyIPs.append(line.strip())
        #for ip in derbyIPs:
        #    try:
        #        self.pluginOutput["AllocatedIPs"].append(str(ip))
        #    except KeyError:
        #        self.pluginOutput["AllocatedIPs"] = []
        #        self.pluginOutput["AllocatedIPs"].append(str(ip))
        for pool in totalNetPools:
            count = len(pool["NETWORK"])
                      
            self.pluginOutput["TotalIPs-"+str(pool["ID"])] = str(count)
            available = count
            for entry in pool["NETWORK"]:
             #   try:
             #       self.pluginOutput[pool["ID"]].append(str(entry[1]))
             #   except KeyError:
             #       self.pluginOutput[pool["ID"]] = []
             #       self.pluginOutput[pool["ID"]].append(str(entry[1]))
                
                try:
                    for allocIP in derbyIPs: #self.pluginOutput["AllocatedIPs"]:
                        if (entry[1] == allocIP):
                            available -= 1
                except KeyError:
                   pass
            #    self.pluginOutput["AvailableIPs-"+str(pool["ID"])] = str(available) 
        pluginExitN(self.resourceName,self.pluginOutput, RET_OK)

if __name__ == '__main__':
    loadNimbusConfig()
    #testObject = PluginCmdLineOpts()
    #testObject.validate()

    while True:

        activeSlots = HeadNodeVMSlots()
        activeSlots() #None, None, None, None)
        time.sleep(3)
    # This sys.exit call should NEVER be reached under normal circumstances, or any....
    sys.exit(RET_CRITICAL)
