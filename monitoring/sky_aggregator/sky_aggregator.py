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
import os
from cStringIO import StringIO
import logging
import sys
from urlparse import urlparse
import urllib2
import gzip
from BaseHTTPServer import BaseHTTPRequestHandler
from redis import Redis, ConnectionError, ResponseError
import time
import xml
import libxml2
import libxslt
#import syslog
from xml.dom.pulldom import parseString, START_ELEMENT


MONITORING_XML_LOC = "Local_Monitoring_XML_Data"
TARGET_CLUSTERS_LOC = "Clusters_Addr_File"
TARGET_XML_PATH = "Target_Monitoring_Data_Path"
TARGET_VM_SLOTS_PATH = "Target_VM_Slots_Path"

#AGGREAGATE_RT_XML_XSL = "rtaggr.xsl"

TARGET_REDIS_DB = "Target_Redis_DB_Id"
SKY_KEY = "Sky_Key"
UPDATE_INTERVAL = "Update_Interval"
REDISDB_SERVER_HOSTNAME = "RedisDB_Server_Hostname"
REDISDB_SERVER_PORT = "RedisDB_Server_Port"

CONF_FILE_LOC = "sky_aggregator.cfg"
CONF_FILE_SECTION = "SkyAggregator"

ConfigMapping = {}

def loadConfig(logger):

    cfgFile = ConfigParser.ConfigParser()
    if(os.path.exists(CONF_FILE_LOC)):
        cfgFile.read(CONF_FILE_LOC)
        try:
            ConfigMapping[MONITORING_XML_LOC] = cfgFile.get(CONF_FILE_SECTION,MONITORING_XML_LOC,0)
            ConfigMapping[TARGET_CLUSTERS_LOC] = cfgFile.get(CONF_FILE_SECTION,TARGET_CLUSTERS_LOC,0)
            ConfigMapping[TARGET_REDIS_DB] = cfgFile.get(CONF_FILE_SECTION,TARGET_REDIS_DB,0)
            ConfigMapping[SKY_KEY] = cfgFile.get(CONF_FILE_SECTION, SKY_KEY,0)            
            ConfigMapping[TARGET_XML_PATH] = cfgFile.get(CONF_FILE_SECTION,TARGET_XML_PATH,0)
            ConfigMapping[TARGET_VM_SLOTS_PATH] = cfgFile.get(CONF_FILE_SECTION,TARGET_VM_SLOTS_PATH,0)           
            ConfigMapping[UPDATE_INTERVAL] = cfgFile.get(CONF_FILE_SECTION, UPDATE_INTERVAL,0)
            ConfigMapping[REDISDB_SERVER_HOSTNAME] = cfgFile.get(CONF_FILE_SECTION, REDISDB_SERVER_HOSTNAME,0)
            ConfigMapping[REDISDB_SERVER_PORT] = cfgFile.get(CONF_FILE_SECTION, REDISDB_SERVER_PORT,0)
   
        except ConfigParser.NoSectionError: 
            logger.error("Unable to locate "+CONF_FILE_SECTION+" section in "+CONF_FILE_LOC+" - Malformed config file?")
            sys.exit(-1)
        except ConfigParser.NoOptionError, nopt:
            logger.error( nopt.message+" of configuration file")
            sys.exit(-1)
    else:
        logger.error( "Configuration file not found in this file's directory")
        sys.exit(-1)

class Loggable:
    """ A simple base class to encapsulate useful logging features - Meant to be derived from

    """
    def __init__(self, callingClass):

#        syslog.openlog(callingClass, syslog.LOG_USER)

        self.logString = StringIO()

        self.logger = logging.getLogger(callingClass)
        self.logger.setLevel(logging.ERROR)
        formatter = logging.Formatter('%(asctime)s ; %(name)s ; %(levelname)s ; %(message)s')

        errorOutputHndlr = logging.FileHandler("sky_aggregator.log")
        errorOutputHndlr.setFormatter(formatter)
        errorOutputHndlr.setLevel(logging.DEBUG)

        self.logger.addHandler(errorOutputHndlr)


class SkyAggregatorHTTPRequest(Loggable):
    """
    This class provides very basic HTTP GET functionality. It was created as a helper class for Perceptor to use
    to query remote Clouds. It implements some basic HTTP protocol functionality including gzip support.

    """

    # This can be almost anything, this was chosen arbitrarily. One could masquerade as another browser or
    # agent, but I don't see the need for this currently
    defaultUserAgent = "SkyAggregator/1.0"

    def __init__(self):
        Loggable.__init__(self,self.__class__.__name__)

    # Request the data from the url passed in. This is done with a HTTP GET and the return code is checked to ensure
    # a 200 (sucess) was received

    def request(self, url):

        results = self._req(url)
        if results != None:  
            if results['rStatus'] != 200:
                self.logger.error("Received HTTP Code: "+str(results['rStatus'])+" - "+ BaseHTTPRequestHandler.responses[results['rStatus']][0])      
            return results['rData']
        
        return ""

    # A helper method that handles the Python urllib2 code and basic HTTP protocol handling
    def _req(self, url):
       
        if (urlparse(url)[0] != 'http'):
            syslog.syslog("Invalid HTTP url passed to 'request' method")
            return

        httpReq = urllib2.Request(url)
        httpReq.add_header("User-Agent", self.__class__.defaultUserAgent)
        httpReq.add_header("Accept-encoding","gzip")
        try:
            httpOpener = urllib2.urlopen(httpReq)
        except urllib2.URLError, err:
            syslog.syslog(url+" "+str(err))
            return
        results = {}
        results['rData'] = httpOpener.read()
 
        if hasattr(httpOpener, 'headers'):
            if(httpOpener.headers.get('content-encoding','') == 'gzip'):
                results['rData'] = gzip.GzipFile(fileobj = StringIO(results['rData'])).read()
        if hasattr(httpOpener, 'url'):
            results['rUrl'] = httpOpener.url
            results['rStatus'] = 200
        if hasattr(httpOpener, 'status'):
            results['rStatus'] = httpOpener.status

        return results

class SkyAggregator(Loggable):

    """
    This class is responsible for querying remote Cloud sites, retrieving their resource and real time XML,
    aggregate and validate the retrieved XML and then finally storing the aggregated XML into a RedisDB

    """

    # Since this is the "primary" class, it is designed to be instantiated first and thus will load the 
    # global ConfigMapping data
    def __init__(self):
        Loggable.__init__(self, self.__class__.__name__)
        loadConfig(self.logger) 
        
        #Connect to the RedisDB
        self.perceptDb = Redis(db=ConfigMapping[TARGET_REDIS_DB], host=ConfigMapping[REDISDB_SERVER_HOSTNAME], port=int(ConfigMapping[REDISDB_SERVER_PORT]))

        self.clusterXML = None

        # Verify the DB is up and running
        try:
           self.perceptDb.ping()
           self.logger.debug("DB Alive")
        except ConnectionError, err:
            print str(err)
            self.logger.error("ConnectionError pinging DB - redis-server running on desired port?")
            sys.exit(-1)
   
    def aggregateRealTimeData(self, cloudXML, rtXML):

        rtStartIndex = rtXML.find("<RealTime>")
        if(rtStartIndex == -1):
            self.logger.error("Unable to find <RealTime> tag in aggregateRealTimeData method")
            return cloudXML 
        rtStartIndex += len("<RealTime>") 
        rtEndIndex = rtXML.find("</RealTime>") 
#        doc = parseString(cloudXML)
#        for event, node in doc:
#            if event == xml.dom.pulldom.START_ELEMENT and node.localName == "Cloud":
#                doc.expandNode(node)
 #               print node.toxml()
            #print node
        insertIndex = cloudXML.find("</Cloud>")
        if(insertIndex == -1):
            self.logger.error("Unable to find </Cloud> tag in aggregateRealTimeData method")
            return cloudXML

        topSlice = cloudXML[:insertIndex]
        bottomSlice = cloudXML[insertIndex:]

        retXML = StringIO()
        retXML.write(topSlice)
            
        snippet = rtXML[rtStartIndex:rtEndIndex]
        rtXML = rtXML.replace(rtXML[rtStartIndex:rtEndIndex],"",1)
        retXML.write(snippet)
        retXML.write(bottomSlice)

        return retXML.getvalue()
 
    def queryRemoteClouds(self):

        addrList = self.loadTargetAddresses()
        tempDict = {}

        dataReq = SkyAggregatorHTTPRequest()

        for entry in addrList:
            #try:
            tempDict[entry] = {}
            if TARGET_XML_PATH in ConfigMapping:
                tempDict[entry][ConfigMapping[TARGET_XML_PATH]] = dataReq.request(entry+ConfigMapping[TARGET_XML_PATH])
            #except KeyError:
                # Note how this is first in the code listing
             #   tempDict[entry] = {}
             #   tempDict[entry][ConfigMapping[TARGET_XML_PATH]] = dataReq.request(entry+ConfigMapping[TARGET_XML_PATH])
            #try:
            if TARGET_VM_SLOTS_PATH in ConfigMapping:
                tempDict[entry][ConfigMapping[TARGET_VM_SLOTS_PATH]] = dataReq.request(entry+ConfigMapping[TARGET_VM_SLOTS_PATH])
            #except KeyError:
              #  tempDict[entry] = {}
              #  tempDict[entry][ConfigMapping[TARGET_VM_SLOTS_PATH]] = dataReq.request(entry+ConfigMapping[TARGET_VM_SLOTS_PATH])  
            
        return tempDict

    def validateXML(self, xmlToProcess):

        ctxtParser = libxml2.schemaNewParserCtxt("sky.xsd")
        ctxtSchema = ctxtParser.schemaParse()
        ctxtValid = ctxtSchema.schemaNewValidCtxt()

        doc = libxml2.parseDoc(xmlToProcess)
        retVal = doc.schemaValidateDoc(ctxtValid)
        if( retVal != 0):
            self.logger.error("Error validating against XML Schema - sky.xsd")
            sys.exit(-1)
        
        doc.freeDoc()
        del ctxtValid
        del ctxtSchema
        del ctxtParser
        libxml2.schemaCleanupTypes()
        libxml2.cleanupParser()

    def persistData(self, clusterDict, cloud):

        cloudXML = StringIO()
        cloudXML.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        cloudXML.write("<Sky>")
 
        for key in clusterDict.keys():
            for subKey in clusterDict[key].keys():
            
               tempString = clusterDict[key][subKey]
               tagIndex = tempString.find("?>") + 2
               #print clusterDict[key][tagIndex:]
               #keyData = urlparse(key)
 
               # Persist the individual, aggregated cloud XML data into the DB with the path used to find
               # the XML as the key
               self.perceptDb.set(key+subKey, clusterDict[key][subKey][tagIndex:], preserve=False)
        if(cloud != None): 
            cloudXML.write(cloud[cloud.find("?>")+2:])
        cloudXML.write("</Sky>")

        #Validate the final 'Sky' XML containing all the cloud information against the XML Schema
        self.validateXML(cloudXML.getvalue())

        # Finally, save the valid XML into the database with the well known SKY_KEY
        self.perceptDb.set(ConfigMapping[SKY_KEY], cloudXML.getvalue(), preserve=False)
        print self.perceptDb.get(ConfigMapping[SKY_KEY])

    def loadTargetAddresses(self):

        cloudAddresses = []

        if(os.path.exists(ConfigMapping[TARGET_CLUSTERS_LOC])):
            try:
                fileHandle = open(ConfigMapping[TARGET_CLUSTERS_LOC],"r")

                for addr in fileHandle:
                    cloudAddresses.append(addr.strip())
                fileHandle.close()
            except IOError, err:
                self.logger.error("IOError processing "+ConfigMapping[TARGET_CLUSTERS_LOC]+" - "+str(err))

        return cloudAddresses

if __name__ == "__main__":

   
    loader = SkyAggregator()
    while True:
        daDict = loader.queryRemoteClouds()
        for entry in daDict.keys():
         
            loader.persistData(daDict, loader.aggregateRealTimeData(daDict[entry][ConfigMapping[TARGET_XML_PATH]], daDict[entry][ConfigMapping[TARGET_VM_SLOTS_PATH]]))

        time.sleep(int(ConfigMapping[UPDATE_INTERVAL]))

