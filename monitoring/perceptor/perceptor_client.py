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


import ConfigParser
import sys
import os
from redis import Redis, ConnectionError, ResponseError
from xml.dom.minidom import parse, parseString
from cStringIO import StringIO
import logging
import amara

RET_CRITICAL = -1

CONF_FILE = "monitoring_config.cfg"
CONF_FILE_SECTION = "Nimbus_Monitoring"
NIMBUS_ADDRESS = "Nimbus_Server_Address"
NIMBUS_LOCATION = "Nimbus_Install_Location"
GLOBUS_LOCATION = "Globus_Install_Location"
SERVER_TMP_LOCATION = "Server_Tmp_Location"
NAGIOS_LOCATION = "Nagios_Location"
JAVA_LOCATION = "Java_Location"
IJ_LOCATION = "IJ_Location"
DERBY_LOCATION = "Derby_Location"
PERCEPTOR_DB = "Perceptor_RedisDB_Num"
SKY_KEY = "Sky_Key"

ConfigMapping = {}

def loadPerceptorClientConfig(logger):


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
            
            ConfigMapping[PERCEPTOR_DB] = cfgFile.get(CONF_FILE_SECTION, PERCEPTOR_DB,0)
            ConfigMapping[SKY_KEY] = cfgFile.get(CONF_FILE_SECTION, SKY_KEY, 0)
        except ConfigParser.NoSectionError:
            print ( "Unable to locate "+CONF_FILE_SECTION+" section in conf file - Malformed config file?")
            sys.exit(RET_CRITICAL)
        except ConfigParser.NoOptionError, nopt:
            print (nopt.message+" of configuration file")
            sys.exit(RET_CRITICAL)
    else:
        print ("Configuration file not found in Nagios Plug-ins directory")
        sys.exit(RET_CRITICAL)

class Loggable:
    """ A simple base class to encapsulate useful logging features - Meant to be derived from

    """
    def __init__(self, callingClass):

        self.logString = StringIO()

        self.logger = logging.getLogger(callingClass)
        self.logger.setLevel(logging.INFO)
        formatter = logging.Formatter('%(asctime)s ; %(name)s ; %(levelname)s ; %(message)s')

        errorOutputHndlr = logging.StreamHandler(sys.stderr)
        errorOutputHndlr.setFormatter(formatter)
        errorOutputHndlr.setLevel(logging.ERROR)

        self.logger.addHandler(errorOutputHndlr)

class perceptorClient(Loggable):

    def __init__(self):
        Loggable.__init__(self,self.__class__.__name__)
        loadPerceptorClientConfig(self.logger)

        self.db = Redis(db=ConfigMapping[PERCEPTOR_DB])

        try:
           self.db.ping()
        except ConnectionError, err:
            print str(err)
            self.logger.error("ConnectionError pinging DB - redis-server running on desired port?")
            sys.exit(-1)

    def _lookupSkyXML(self):

        skyXML = self.db.get(ConfigMapping[SKY_KEY])
        amDoc = amara.parse(str(skyXML)) 

        return amDoc

    def _getWorkerNodeVirtualizationTech(self, node):

        return str(node.VirtualizationTech.Type)

    def _getWorkerNodeCPUArch(self, node):

        return str(node.CPUArch.MicroArchitecture)

    def _getWorkerNodeCPUID(self, node):

        return str(node.CPUID.Description)

    def _getWorkerNodeCPUCores(self, node):
        return str(node.CPUCores)
    
    def _getWorkerNodeMem(self, node):
       
        tDict = {}
        
        tDict["TotalMB"] = str(node.Memory.TotalMB)
        tDict["FreeMB"] = str(node.Memory.FreeMB)

        return tDict

    def _populateWorkerNode(self, node):

        tDict = {}
        tDict["CPUCores"] = self._getWorkerNodeCPUCores(node)
        tDict["Memory"] = self._getWorkerNodeMem(node)
        tDict["VirtualizationTech"] = self._getWorkerNodeVirtualizationTech(node)
        tDict["CPUID"] = self._getWorkerNodeCPUID(node)
        tDict["CPUArchitecture"] = self._getWorkerNodeCPUArch(node)

        retDict = {"Node": tDict}
        return retDict

    def _getCloudWorkerNodes(self, cloud):
        
        tempNodes = []
        for curnode in cloud.WorkerNodes.Node:
            tempNodes.append(self._populateWorkerNode(curnode))

        return tempNodes

    def _getCloudVMMemoryPools(self, cloud):
       tempNodes = []
       for curnode in cloud.VMM_Pools.Pool:
           tDict = {}
           for entry in  curnode.xml_properties.keys():
               tDict[entry] = str(curnode.xml_properties[entry])
           tempNodes.append(tDict)
       return tempNodes

    def _getCloudNetworkPools(self, cloud):

        tempNodes = []

        for curnode in cloud.Network_Pools.Pool:
            tDict = {}
            for entry in curnode.xml_properties.keys():
                tDict[entry] = str(curnode.xml_properties[entry])
            tempNodes.append(tDict)
        return tempNodes

    def _getCloudServiceData(self, cloud):
        tDict = {}
        tDict["Path"] = str(cloud.Service.Path)
        tDict["HostName"] = str(cloud.Service.HostName)
        tDict["Type"] = str(cloud.Service.Type)
        tDict["Port"] = str(cloud.Service.Port)
        tDict["IP"] = str(cloud.Service.IP)

        return tDict

    def _getCloudIaaSDiagnostics(self, cloud):
        tDict = {}
        tDict["InternalRepresentation"] = str(cloud.IaasDiagnostics.InternalRepresentation)

        return tDict

    def getCloudSeer(self):

        sky = []
        boundXML = self._lookupSkyXML()

        for cloud in boundXML.Sky.Cloud:

            cloudDescriptor = {}
            cloudDescriptor["Service"] = self._getCloudServiceData(cloud)
            cloudDescriptor["IaaSDiagnostics"] = self._getCloudIaaSDiagnostics(cloud)
            cloudDescriptor["VMMemoryPools"] = self._getCloudVMMemoryPools(cloud)
            cloudDescriptor["WorkerNodes"] = self._getCloudWorkerNodes(cloud)
            cloudDescriptor["NetworkPools"] = self._getCloudNetworkPools(cloud)
            sky.append( cloudDescriptor)
        return sky

if __name__ == '__main__':

    temp = perceptorClient()
    thing = temp.getCloudSeer()

    print thing
