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

CONF_FILE_LOC = "sky_aggregator.cfg"
CONF_FILE_SECTION = "SkyAggregator"

ConfigMapping = {}

def loadConfig():

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
        except ConfigParser.NoSectionError: 
            syslog.syslog("Unable to locate "+CONF_FILE_SECTION+" section in "+CONF_FILE_LOC+" - Malformed config file?")
            sys.exit(-1)
        except ConfigParser.NoOptionError, nopt:
            syslog.syslog( nopt.message+" of configuration file")
            sys.exit(-1)
    else:
        syslog.syslog( "Configuration file not found in this file's directory")
        sys.exit(-1)

class Loggable:
    """ A simple base class to encapsulate useful logging features - Meant to be derived from

    """
    def __init__(self, callingClass):

#        syslog.openlog(callingClass, syslog.LOG_USER)

        self.logString = StringIO()

        self.logger = logging.getLogger(callingClass)
        self.logger.setLevel(logging.INFO)
        formatter = logging.Formatter('%(asctime)s ; %(name)s ; %(levelname)s ; %(message)s')

        errorOutputHndlr = logging.FileHandler("sky_aggregator.log")
        errorOutputHndlr.setFormatter(formatter)
        errorOutputHndlr.setLevel(logging.ERROR)

        self.logger.addHandler(errorOutputHndlr)


class SkyAggregatorHTTPRequest(Loggable):

    defaultUserAgent = "SkyAggregator/1.0"

    def __init__(self):
        Loggable.__init__(self,self.__class__.__name__)

    def request(self, url):

        results = self._req(url)
        if results != None:  
            if results['rStatus'] != 200:
                self.logger.error("Received HTTP Code: "+str(results['rStatus'])+" - "+ BaseHTTPRequestHandler.responses[results['rStatus']][0])      
            return results['rData']
        
        return ""


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
   
    def __init__(self):
        Loggable.__init__(self, self.__class__.__name__)
        loadConfig() 
        self.perceptDb = Redis(db=ConfigMapping[TARGET_REDIS_DB])

        self.clusterXML = None

        try:
           self.perceptDb.ping()
        except ConnectionError, err:
            print str(err)
            self.logger.error("ConnectionError pinging DB - redis-server running on desired port?")
            sys.exit(-1)
   
    def aggregateRealTimeData(self, cloudXML, rtXML):

        rtStartIndex = rtXML.find("<RealTime>")
        retXML = StringIO()
        
#        doc = parseString(cloudXML)
#        for event, node in doc:
#            if event == xml.dom.pulldom.START_ELEMENT and node.localName == "Cloud":
#                doc.expandNode(node)
 #               print node.toxml()
            #print node
        insertIndex = cloudXML.find("</Cloud>")
        if(insertIndex == -1):
            return
            #insertIndex += len("<Cloud>")
        #else:
        #    print "RET DAMMIT"
        #    return

        topSlice = cloudXML[:insertIndex]
        bottomSlice = cloudXML[insertIndex:]

        retXML.write(topSlice)
        
        rtStartIndex += len("<RealTime>")
        rtEndIndex = rtXML.find("</RealTime>") 
            
        snippet = rtXML[rtStartIndex:rtEndIndex]
        rtXML = rtXML.replace(rtXML[rtStartIndex:rtEndIndex],"",1)
        retXML.write(snippet)
        retXML.write(bottomSlice)

        return retXML.getvalue()
 
    def queryRemoteClusters(self):

        addrList = self.loadTargetAddresses()
        tempDict = {}

        dataReq = SkyAggregatorHTTPRequest()

        for entry in addrList:
            try:
                if TARGET_XML_PATH in ConfigMapping:
                    tempDict[entry][ConfigMapping[TARGET_XML_PATH]] = dataReq.request(entry+ConfigMapping[TARGET_XML_PATH])
            except KeyError:
                # Note how this is first in the code listing
                tempDict[entry] = {}
                tempDict[entry][ConfigMapping[TARGET_XML_PATH]] = dataReq.request(entry+ConfigMapping[TARGET_XML_PATH])
            try:
                if TARGET_VM_SLOTS_PATH in ConfigMapping:
                    tempDict[entry][ConfigMapping[TARGET_VM_SLOTS_PATH]] = dataReq.request(entry+ConfigMapping[TARGET_VM_SLOTS_PATH])
            except KeyError:
                tempDict[entry] = {}
                tempDict[entry][ConfigMapping[TARGET_VM_SLOTS_PATH]] = dataReq.request(entry+ConfigMapping[TARGET_VM_SLOTS_PATH])  
            
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
        del ctxtParser
        del ctxtSchema
        del ctxtValid
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
               self.perceptDb.set(key+subKey, clusterDict[key][subKey][tagIndex:], preserve=False)
        if(cloud != None): 
            cloudXML.write(cloud[cloud.find("?>")+2:])
        cloudXML.write("</Sky>")
        #print cloudXML.getvalue()
        #t = parseString(cloudXML.getvalue())
        #print t.toprettyxml()

        self.validateXML(cloudXML.getvalue())

        self.perceptDb.set(ConfigMapping[SKY_KEY], cloudXML.getvalue(), preserve=False)
        print self.perceptDb.get(ConfigMapping[SKY_KEY])

    def loadTargetAddresses(self):

        clusterAddresses = []

        if(os.path.exists(ConfigMapping[TARGET_CLUSTERS_LOC])):
            try:
                fileHandle = open(ConfigMapping[TARGET_CLUSTERS_LOC],"r")

                for addr in fileHandle:
                    clusterAddresses.append(addr.strip())
                fileHandle.close()
            except IOError, err:
                self.logger.error("IOError processing "+ConfigMapping[TARGET_CLUSTERS_LOC]+" - "+str(err))

        return clusterAddresses

if __name__ == "__main__":

   
    loader = SkyAggregator()
    while True:
        daDict = loader.queryRemoteClusters()
        for entry in daDict.keys():
         
            loader.persistData(daDict, loader.aggregateRealTimeData(daDict[entry][ConfigMapping[TARGET_XML_PATH]], daDict[entry][ConfigMapping[TARGET_VM_SLOTS_PATH]]))

        time.sleep(int(ConfigMapping[UPDATE_INTERVAL]))

