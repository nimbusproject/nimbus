#!/usr/bin/python -u

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
 *
 * """

__VERSION__ = '0.01'

import sys
import logging
from cStringIO import StringIO
from xml.sax import make_parser
from xml.sax.handler import ContentHandler
import xml
import subprocess
from subprocess import *

SERVER_ADDRESS = "https://vmcgs29.phys.uvic.ca:8443/wsrf/services/DefaultIndexService"
XML_ROOT_TAG = "ROOT"

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

        
# This class implements the SAX API functions 'startElement', 'endElement' and 'characters'
# It is also intimately tied to the XML format used by the client side plugins

class ResourceHandler(ContentHandler):
    def __init__(self): 
                self.isResource = False
                self.isEntry = False
                self.collectedResources = {}
                self.repeatedResource = False
    def startElement(self,name,attr):

            if name == 'RES':
                    self.topLevelKey = attr.getValue('LOC')
                    self.secondLevelKey = attr.getValue('TYPE')

                    if(self.topLevelKey not in self.collectedResources.keys()):
                            self.collectedResources[self.topLevelKey] = {}
                    if(self.secondLevelKey not in self.collectedResources[self.topLevelKey].keys()):
                            self.collectedResources[self.topLevelKey][self.secondLevelKey] = {}
                    self.isResource = True
            elif name == 'ENTRY':
                    self.isEntry = True
                    self.thirdLevelKey = attr.getValue('ID')
                    identifiers = self.thirdLevelKey.split(":")
            
                    if(self.thirdLevelKey in self.collectedResources[self.topLevelKey][self.secondLevelKey].keys()):
                        self.repeatedResource = True
                    if(identifiers > 1):
                        self.repeatedResource = False

    def characters (self, ch):
        if (self.isEntry == True and self.repeatedResource == False):
            self.collectedResources[self.topLevelKey][self.secondLevelKey][self.thirdLevelKey] = ch
    def endElement(self, name):
                if name == 'RES':
                    self.isResource = False
                elif name == 'ENTRY':
                    self.isEntry = False
                    self.repeatedResource = False
    def getResources(self):
            return self.collectedResources


class MDSResourceException(Exception):

    def __init__(self, value):
        self.value = value
    
    def __str__(self):
        return repr(self.value)
        

class MDSResourceQuery(Loggable):
    """ This class handles all the details of querying the MDS to retrieve XML then process the text
    into a useful data structure for further use. This includes post querying transformation of 
    the Network Pools information to provide the "Available Slots" information
    """
    def __init__(self):
        Loggable.__init__(self,self.__class__.__name__)

    def __call__(self,serviceAddress, rootTag):
 
        # This argument string also contains an XPath query to extract the appropriate XML from
        # the XML/WebService response to querying the DefaultIndexService
        argString = "-s " + serviceAddress + " \"//*[local-name()='"+ rootTag +"']\""
        # The funny '.communicate()[0]' retrieves the stdout stream from the pipe so that the looked up
        # xml data can be brought into this script for processing
        try:
            
            process = subprocess.Popen("$GLOBUS_LOCATION/bin/wsrf-query "+argString, shell=True,stderr=PIPE, stdout=PIPE).communicate()
            # STDOUT from the command just executed
            retrievedXML = process[0]
            # STDERR from the command just executed
            retrievedError = process[1]
            
            # This was added since the exception handlers aren't being called
            if retrievedError != "":
                self.logger.error("Failed to execute external command :"+retrievedError)
                raise MDSResourceException("Failed to execute external command :"+retrievedError)

        #These exception handlers aren't being called... Stupid python
        except Exception, e:
            self.logger.error("An unknown Exception has occured: "+e.getMessage())
            raise MDSResourceException("Unknown Exception has occured: "+e.getMessage())
        # According to the Python API docs, the subprocess.Popen command can throw an OSError or ValueError
        # but neither of these exception could be raised in testing (with Python 2.4.3)

        #except OSError:
        #    self.logger.error("OSError occured performning subprocess.Popen - Check 'wsrf-query' location")
        #    sys.exit(1)
        #except ValueError:
        #    self.logger.error("ValueError occured performing subprocess.Popen() - Check arguments")
        #    sys.exit(1)
        
        # Check to see if there was any results returned from the MDS
        # When the aggregator is unregistered, the query returns the string:
        # "Query did not return any results."
        # Thus, 'Query' is used for the 'find' call as it is the first word in the sentence 
        # (this isn't a requirement, any word could have been used really)
        if(retrievedXML.find("Query") != -1):
            print "Should be nothing returning from the MDS"
            raise MDSResourceException("No Resources in MDS Registry")

        xmlHandler = ResourceHandler()

        try:
            xml.sax.parseString(retrievedXML.strip(), xmlHandler)
        except xml.sax.SAXException, e:
            self.logger.error("Failed to parse retrieved XML: "+e.getMessage())    
            raise MDSResourceException("Failed to parse retrieved XML: "+e.getMessage())
        self.netPoolProcessing(xmlHandler.getResources())

        return xmlHandler.getResources()


    def netPoolProcessing(self, resources):
        # OK, so I need to covert the "NetPools" information into an "availble" slots idea 

        queue = []
        totalIPs = []
        # This boolean is used so that the logic below only executes once
        foundNetPools = False

        # The top level of the resources structure is the IP addresses that reported resources
        physicalIPs = resources.keys()
        usedQueue = []

        # This loop is for finding the NetPools:Used entries only
        for ip in physicalIPs:
            uniqueIDs = resources[ip].keys()
            for secondEntry in uniqueIDs:
                # if the 'NetPools:Used' ID is found...
                if(secondEntry.find("NetPools:Used")!=-1):
                    usedQueue.append(resources[ip][secondEntry]["Used"])
        netPoolsIP = ""
        for ip in physicalIPs:
            # 'uniqueIDs' is the text description of the resource being reported
            uniqueIDs = resources[ip].keys()
            for secondEntry in uniqueIDs:
                # Since the 'NetPools' description is re-used (with additional unique IDs)
                # the 'foundNetPools' guard boolean is needed in this logic
                if(secondEntry.find("NetPools")!=-1 and secondEntry !=("NetPools:Totals")):
                    queue.append(secondEntry)
                    # Yes this assignment is evaluated multiple times, but it's fine how it is
                    foundNetPools = True
                    # This value will be needed later on when "inserting" the available addresses
                    # into the 'resources' data structure
                    netPoolsIP = ip
            # Since this code blindly loops over all entries, the boolean 'foundNetPools' guard is needed 
            # to prevent dictionary insertion errors for the physicalIPs that don't include 'NetPools' resources
            # Only 1 IP address should be reporting 'NetPools' resources
            if(foundNetPools):
                for item in queue:
                    totalIPs.append(resources[ip][item])
                    del(resources[ip][item])
                # 'toggle' the boolean off, as only 1 ip address should include 'NetPools' resources
                foundNetPools = False 
        # Using a set() to filter out duplicat 'pool names' that are encountered in the data structure
        filterSet = set()

        for entry in totalIPs:
            # There should only ever be 1 entry in the keys() list, hence the [0] to extract it
            filterSet.add(entry.keys()[0])
        
        # filterSet now contains the pool names
        pools ={}
        # Initialize lists for every pool name found
        for entry in filterSet:
            pools[entry]=[]

        for entry in totalIPs:
            # both the 'keys()' and 'values()' should return a list of 1 item, so the [0] to extract it 
            pools[entry.keys()[0]].append(entry.values()[0])    
        
        for ip in usedQueue:
            for key in pools.keys():
                if ip in pools[key]:
                    # This removes the address from ALL the pools, even Used
                    pools[key].remove(ip)

        # Remove the USED mapping as it is no longer required
                if( "Used" in pools):
                    del(pools["Used"])
        
        # Insert the "NetPoolsAvailable" resource into the 'resources' data structure
        resources[netPoolsIP]["NetPools:Available"] = {}
        for pool in pools.keys():
            resources[netPoolsIP]["NetPools:Available"][pool] = len(pools[pool])


myQuery = MDSResourceQuery()

# This print statement is just a visual way of seeing the work performed inside the driver
# and is not pivotal or even necessary for the driver to function properly
print myQuery(SERVER_ADDRESS,XML_ROOT_TAG)

sys.exit(0)
