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
 * OR
 * Ian Gable - igable@uvic.ca
 *
 * """


__VERSION__ = '0.01'

import sys
import commands
import os
import logging
from cStringIO import StringIO
from optparse import OptionParser
from xml.sax import make_parser
from xml.sax.handler import ContentHandler
import xml
import subprocess
import commands
import re
import socket
import time
# NAGIOS Plug-In API return code values

NAGIOS_RET_OK = 0
NAGIOS_RET_WARNING = 1
NAGIOS_RET_CRITICAL = 2
NAGIOS_RET_UNKNOWN = 3

IJ_LOCATION = "/opt/sun/javadb/bin/ij"
SQL_IP_SCRIPT = "/usr/local/nagios/libexec/nimbus_derby_used_ips.sql"
SQL_RUNNING_VM_SCRIPT = "/usr/local/nagios/libexec/nimbus_derby_running_vms.sql"
# Attempting to access the environment variables from within Nagios' context errors out!
ENV_DERBY_HOME = "/opt/sun/javadb"        #os.environ["DERBY_HOME"]
ENV_GLOBUS_LOC = "/usr/local/globus-4.0.8"#os.environ["GLOBUS_LOCATION"]
ENV_JAVA_HOME = "/usr/java/latest"        #os.environ["JAVA_HOME"]
#The "NIMBUS_" entries are relative to the ENV_GLOBUS_LOC var
NIMBUS_CONF = "/etc/nimbus/workspace-service"
NIMBUS_NET_CONF = "/network-pools"
NIMBUS_PHYS_CONF = "/vmm-pools"

NIMBUS_PBS_CONF = "/pilot.conf"
NIMBUS_PBS_SUPPORT = "/other/resource-locator-ACTIVE.xml"


def pluginExit(messageString, logString, returnCode):

    # ALRIGHT, so the log string is seperated by  my "delimiter" ';'
    # Thus, I'm going to assume the following log style format:

    # 2009-05-29 13:48:55,638 ; VMMemory ; INFO ; sl52base ; MEMORY ; 524288

    # The pertinent information should be located in the "4th col" and on
    # The 3rd col lists the "logger lvl", which I'm using to indicate
    # if it's standard plug-in output or an error

    outputString = StringIO()
    outputString.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")

    localIP = (socket.gethostbyaddr( socket.gethostname() ))[2][0]

    lines = logString.splitlines()
    for line in lines:
        # If we encounter an 'error' entry in the logger, skip over it
        if (line.find("ERROR") != -1):
            returnCode = NAGIOS_RET_CRITICAL
            continue
        logStringEntries = line.split(';')
        
        outputString.write("<RES LOC=\""+localIP+"\" TYPE=\""+messageString+":" +logStringEntries[3].strip()+"\">")

        outputString.write("<ENTRY ID=\""+logStringEntries[4].strip()+"\">")
        outputString.write(logStringEntries[5].strip())
        outputString.write("</ENTRY>")
        outputString.write("</RES>")

    sys.stdout.write(messageString+" | "+ outputString.getvalue()+"\n")
    sys.exit(returnCode)

class PluginObject:    

    def __init__(self, callingClass):
        self.logString = StringIO()
    
        self.logger = logging.getLogger(callingClass)
        self.logger.setLevel(logging.INFO)
        formatter = logging.Formatter('%(asctime)s ; %(name)s ; %(levelname)s ; %(message)s')
        xmlOutputHndlr = logging.StreamHandler(self.logString)
        xmlOutputHndlr.setFormatter(formatter)
        xmlOutputHndlr.setLevel(logging.INFO)

        errorOutputHndlr = logging.StreamHandler(sys.stdout)
        errorOutputHndlr.setFormatter(formatter)
        errorOutputHndlr.setLevel(logging.ERROR)

        self.logger.addHandler(xmlOutputHndlr)
        self.logger.addHandler(errorOutputHndlr)


class PluginCmdLineOpts(PluginObject):

        def __init__(self):
                PluginObject.__init__(self,self.__class__.__name__)
                # Parse command-line options.
                parser = OptionParser()

                parser.add_option("--HNconsisten", action="callback",help="Verify internal Derby database consistency", callback=HeadNodeDBConsistent())
                parser.add_option("--HNvmmpool", action="callback",help="Publish Nimbus VMM pool information", callback=HeadNodeVMMPools())
                parser.add_option("--HNnetpool", action="callback",help="Publish Nimbus network pool information", callback=HeadNodeNetPools())         
                parser.add_option("--HNpbsmem", action="callback",help="Publish PBS/Torque available memory information", callback=HeadNodePBSMemory())
                parser.add_option("--HNpbssupport", action="callback",help="Publish support for PBS/Torque Pilot Jobs", callback=HeadNodePBSSupport())
                self.parser = parser

        # This method is also responsible for "picking" what resource to monitor via the appropriate
        # command line switches (which I need to define). I don't want a single, monolithic script
        # running for ALL the resources, since this waters down NAGIOS's monitoring capabilities
        # (since that would make only a single resource to monitor)
        # Instead, this one script will be executed multiple time with different commandline options
        # to facilitate the monitoring of the different resources independant of one another

        def validate(self):
            (options, args) = self.parser.parse_args()


class HeadNodePBSSupport(PluginObject):

    def __init__(self):
        self.resourceName = "PBS-Support"
        PluginObject.__init__(self, self.__class__.__name__)

    def __call__(self, option, opt_str, value, parser):

        try:
            fileHandle = open(ENV_GLOBUS_LOC+NIMBUS_CONF+NIMBUS_PBS_SUPPORT)
        except IOError:
            self.logger.error("IOError opening resource-locator-ACTIVE.xml for processing!")
            sys.exit(NAGIOS_RET_CRITICAL)
        else:
            pbsSupported = False
            for line in fileHandle:
                if(line.find(".pilot.PilotSlotManagement") == -1):
                    continue
                else:
                    pbsSupported = True
            fileHandle.close()    
            self.logger.info("-; PilotJobSupport; "+str(pbsSupported))
            pluginExit(self.resourceName, self.logString.getvalue(), NAGIOS_RET_OK)

class HeadNodePBSMemory(PluginObject):

    def __init__(self):
        self.resourceName = "PBS-Memory"
        PluginObject.__init__(self, self.__class__.__name__)

    def __call__(self, option, opt_str, value, parser):

        try:
            fileHandle = open(ENV_GLOBUS_LOC+NIMBUS_CONF+NIMBUS_PBS_CONF)
        except IOError:
            self.logger.error("IOError opening pilot.conf for processing!")
            sys.exit(NAGIOS_RET_CRITICAL)
        else:    
            memory = 0
            for line in fileHandle:

                if(line.find("memory.maxMB") == -1):
                    continue
                else:
                    lineSegs = line.split("=")
                    memory = int(lineSegs[1].strip())
            # Convert the megabytes to kbytes for consistency with other memory reporting 
            memory *= 1024
            fileHandle.close()
            self.logger.info("-; Available; "+str(memory))    
            pluginExit(self.resourceName, self.logString.getvalue(), NAGIOS_RET_OK)
            
            
class HeadNodeDBConsistent(PluginObject):

    def __init__(self):
        self.resourceName = "DerbyDB-Consistency"
        PluginObject.__init__(self,self.__class__.__name__)
    
    def __call__(self, option, opt_str, value, parser):
    
        isConsistent = True

        query = IJ_LOCATION+ " "+SQL_IP_SCRIPT
        output,status = (subprocess.Popen([query],stdout = subprocess.PIPE, stderr = subprocess.PIPE, shell=True, env={'DERBY_HOME':ENV_DERBY_HOME,'JAVA_HOME':ENV_JAVA_HOME,'GLOBUS_LOCATION':ENV_GLOBUS_LOC})).communicate()

        derbyIPs = []
        patt = re.compile(r"(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})") 
        for line in output.split():
            myRe = patt.search(line)
            if(myRe):
                derbyIPs.append({line.strip(): False})    
        # So now I know what IPs should be reachable    
        for remoteVM in derbyIPs:
            # There should be only 1 key in the dictionary structure
            # the '[0]' extracts the IP address as a string
            addr = remoteVM.keys()[0]
            if(self.ping(addr)):
                derbyIPs[derbyIPs.index(remoteVM)][addr] = True    

        for foundVM in derbyIPs:
            # Again, there should be only 1 'value' for a given key
            # and the 'value' is a True/False boolean
            if not (foundVM.values()[0]):
                self.logger.error("Unable to reach VM via PING - "+foundVM.keys()[0])
                isConsistent = False
        # OK, so now are their reachable VMs that should be terminated?

        query = IJ_LOCATION + " " + SQL_RUNNING_VM_SCRIPT
        output,status = (subprocess.Popen([query],stdout = subprocess.PIPE, stderr = subprocess.PIPE, shell=True, env={'DERBY_HOME':ENV_DERBY_HOME,'JAVA_HOME':ENV_JAVA_HOME,'GLOBUS_LOCATION':ENV_GLOBUS_LOC})).communicate()

        ijOutput = output.split()
        counter = 0
        VMs = []
        for line in ijOutput:

                myRe = patt.search(line)
                if(myRe):
                    network = line.split('|')[0]
                    networkEntries = network.split(';')
                    # The networking info and termination time share the same line in the returned results from the IJ query
                    # so an array is returned by the split, and the term time is in the second entry
                    # NOTE - All the times stored in the database have 3 additional #'s of precision compared to the Unix
                    # time command of 'date +%s', which gives us the time since epoch
                    # Thus, the trailing 3 numerals of all time values are stripped off

                    termTime = (line.split('|')[1])[:-3]
                    # Both 'StartTime' and 'ShutdownTime' have a pesky pipe '|' char preceding the number that needs to be
                    # stripped off

                    startTime = ((ijOutput[counter+1])[1:len(ijOutput[counter+1])])[:-3]
                    shutdownTime = ((ijOutput[counter+2])[1:len(ijOutput[counter+2])])[:-3]
                
                    VMs.append({"IP":networkEntries[5], "TermTime":termTime,"StartTime":startTime, "ShutdownTime":shutdownTime})
                counter += 1
        currentTime = int(time.time())
        #print VMs
        for entry in VMs:
            #print currentTime
            #print entry["ShutdownTime"]
            if(int(currentTime) > int(entry["ShutdownTime"] )):
                # Need to ping the VM to see if it's still alive
                if(self.ping(entry["IP"])  ):
                    self.logger.error("VM at IP address: "+entry["IP"]+" is alive and shouldn't be!")
                else:
                    self.logger.error("VM at IP address: "+entry["IP"]+" in DB but unreachable!")    
                isConsistent = False        
        if (isConsistent):
            retCode = NAGIOS_RET_OK
        else:
            retCode = NAGIOS_RET_CRITICAL
        self.logger.info("Head-Node; Consistent ; "+str(isConsistent))
        
        pluginExit(self.resourceName, self.logString.getvalue(), retCode)

    # The only "tunable" paramter for this function is the number value that comes after the '-c' in the Popen
    # command below. This is a parameter for the 'ping' command line utility and dictates the number of 'pings'
    # sent to the target. I have it set to '1' for speed reasons, as this function is somehwat slow in terms of
    # execution time. Adjust this value to send more pings if required        
    def ping(self, hostaddress):
       
        ping = subprocess.Popen(["ping","-c","1",hostaddress],stdout = subprocess.PIPE,stderr = subprocess.PIPE)
        out, error = ping.communicate()
        
        retVal = True
        if(out.find("Unreachable") != -1):
            retVal = False
        return retVal


class HeadNodeVMMPools(PluginObject):
      
    def __init__(self):
        PluginObject.__init__(self,self.__class__.__name__)
        self.resourceName = "VMM-Pools"

    def __call__(self, option, opt_str, value, parser):
        vmmPools = os.listdir(ENV_GLOBUS_LOC+NIMBUS_CONF+NIMBUS_PHYS_CONF)

        netPools = os.listdir(ENV_GLOBUS_LOC+NIMBUS_CONF+NIMBUS_NET_CONF)
        poolListing = {}
        for pool in vmmPools:
            
            # Ignore "dot" file/folders - hidden directories
            if(pool.startswith(".")):
                continue
            totalNetPools = {"ANY":0}
            for npool in netPools:
                if(npool.startswith(".")):
                    continue
                totalNetPools.update({npool:0})
            try:
                fileHandle = open(ENV_GLOBUS_LOC+NIMBUS_CONF+NIMBUS_PHYS_CONF+"/"+pool)
                workerNodes = []
                for entry in fileHandle:
                    if(entry.startswith("#") or entry.isspace()):
                        continue
                    t = entry.split()
                    workerNodes.append(t)
                fileHandle.close()
                for entry in workerNodes:
                    # IF there is only 2 entries on this given line, that means
                    # the particular workerNode has no specific network pool 
                    # configured, so it's memory count gets added to the "global"
                    # or DEFAULT count
                    keyList = []
                    if(len(entry)< 3):
                        keyList.append("ANY")
                    else:
    
                        keyList = entry[2].split(",")
                    for network in keyList:

                        if( network == "*"):
                            totalNetPools["ANY"] += int(entry[1])
                            continue
                        
                        if network in (totalNetPools.keys()):
                            totalNetPools[network] += int(entry[1])            
                        else:
                            self.logger.error("Erroneous entry in the VMM configuration: "+ network+" - Ignoring")
                poolListing[pool] = totalNetPools
            except IOError:
                self.logger.error("Error opening VMM-pool: "+ENV_GLOBUS_LOC+NIMBUS_CONF+NIMBUS_PHYS_CONF+ pool)
                sys.exit(NAGIOS_RET_CRITICAL)

        for key in poolListing.keys():
            for entry in totalNetPools.keys():
                self.logger.info(key+" ; "+entry+" ; "+str(poolListing[key][entry]))            
            
        pluginExit(self.resourceName, self.logString.getvalue(), NAGIOS_RET_OK)

class HeadNodeNetPools(PluginObject):

    def __init__(self):
        PluginObject.__init__(self, self.__class__.__name__)
        self.resourceName = "NetPools"


    def __call__(self, option, opt_str, value, parser):
        netPools = os.listdir(ENV_GLOBUS_LOC+NIMBUS_CONF+NIMBUS_NET_CONF)
        totalNetPools = []
        for pool in netPools:

            if(pool.startswith(".")):
                continue
            netPoolData = {}
            netPoolData["ID"] = pool
            try:
                fileHandle = open(ENV_GLOBUS_LOC+NIMBUS_CONF+NIMBUS_NET_CONF+"/"+pool)
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
                self.logger.error("Error opening network-pool: "+ENV_GLOBUS_LOC+NIMBUS_CONF+NIMBUS_NET_CONF+"/"+pool)
                sys.exit(NAGIOS_RET_ERROR)

        query = IJ_LOCATION+ " "+SQL_IP_SCRIPT
        output,status = (subprocess.Popen([query],stdout = subprocess.PIPE, stderr = subprocess.PIPE, shell=True, env={'DERBY_HOME':ENV_DERBY_HOME,'JAVA_HOME':ENV_JAVA_HOME,'GLOBUS_LOCATION':ENV_GLOBUS_LOC})).communicate()
        
        derbyIPs = []
        patt = re.compile(r"(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})")
        for line in output.split():
            myRe = patt.search(line)
            if(myRe):
                derbyIPs.append(line.strip())
        uniqueID = 0
        for ip in derbyIPs:
            self.logger.info("Used:"+str(uniqueID)+ " ; Used; "+ str(ip))
            uniqueID = uniqueID+1
        uniqueID = 0
        for dict in totalNetPools:
            count = len(dict["NETWORK"])
                      
                      
            self.logger.info("Totals ;"+dict["ID"]+";"+ str(count))
            for entry in dict["NETWORK"]:
                self.logger.info(str(uniqueID)+";"+dict["ID"]+";"+str(entry[1]))    
                uniqueID = uniqueID+1
        pluginExit(self.resourceName, self.logString.getvalue(), NAGIOS_RET_OK)

if __name__ == '__main__':
    testObject = PluginCmdLineOpts()
    testObject.validate()

# This sys.exit call should NEVER be reached under normal circumstances, or any....
sys.exit(NAGIOS_RET_CRITICAL) 
