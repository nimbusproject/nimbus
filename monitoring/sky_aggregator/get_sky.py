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

CONF_FILE = "get_sky.cfg"
CONF_FILE_SECTION = "Get_Sky"
REDISDB_SERVER_HOSTNAME = "RedisDB_Server_Hostname"
REDISDB_SERVER_PORT = "RedisDB_Server_Port"
SERVER_TMP_LOCATION = "Server_Tmp_Location"
SKY_DB = "Sky_RedisDB_Num"
SKY_KEY = "Sky_Key"

ConfigMapping = {}

def loadGetSkyClientConfig(logger):


    cfgFile = ConfigParser.ConfigParser()
    if(os.path.exists(CONF_FILE)):
        cfgFile.read(CONF_FILE)
        try:
            ConfigMapping[SERVER_TMP_LOCATION] = cfgFile.get(CONF_FILE_SECTION, SERVER_TMP_LOCATION,0)
            ConfigMapping[REDISDB_SERVER_HOSTNAME] = cfgFile.get(CONF_FILE_SECTION, REDISDB_SERVER_HOSTNAME,0)
            ConfigMapping[REDISDB_SERVER_PORT] = cfgFile.get(CONF_FILE_SECTION, REDISDB_SERVER_PORT,0)
            ConfigMapping[SKY_DB] = cfgFile.get(CONF_FILE_SECTION, SKY_DB,0)
            ConfigMapping[SKY_KEY] = cfgFile.get(CONF_FILE_SECTION, SKY_KEY, 0)
        except ConfigParser.NoSectionError:
            logger.error( "Unable to locate "+CONF_FILE_SECTION+" section in conf file - Malformed config file?")
            sys.exit(RET_CRITICAL)
        except ConfigParser.NoOptionError, nopt:
            logger.error(nopt.message+" of configuration file")
            sys.exit(RET_CRITICAL)
    else:
        logger.error("Configuration file not found in Nagios Plug-ins directory")
        sys.exit(RET_CRITICAL)

class Loggable:
    """ A simple base class to encapsulate useful logging features - Meant to be derived from

    """
    def __init__(self, callingClass):

        self.logString = StringIO()

        self.logger = logging.getLogger(callingClass)
        self.logger.setLevel(logging.INFO)
        formatter = logging.Formatter('%(asctime)s ; %(name)s ; %(levelname)s ; %(message)s')

        errorOutputHndlr = logging.FileHandler("get_sky.log")
        errorOutputHndlr.setFormatter(formatter)
        errorOutputHndlr.setLevel(logging.ERROR)

        self.logger.addHandler(errorOutputHndlr)

class getSkyClient(Loggable):
    """
    This class is responsible for querying the RedisDB for the Sky XML and binding it back into 
     a usable data structure. This data structure format is a nested Dictionary of dictionaries and lists. The 
     exact format and names for various keys mimics the public XML format. This means that this class is dependant
     on the public XML format - If the XML changes this class needs to be updated accordingly.
     This dependancy is unavoidable conceptually, and the use of the Amara utility to provide the binding mechanism
     requires this knowledge.
    
    """
    def __init__(self):
        Loggable.__init__(self,self.__class__.__name__)
        loadGetSkyClientConfig(self.logger)

        self.db = Redis(db=ConfigMapping[SKY_DB])

        try:
           self.db.ping()
        except ConnectionError, err:
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

    def getSkyView(self):

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

    temp = getSkyClient()
    thing = temp.getSkyView()

    print thing
