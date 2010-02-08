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
 * or Ian Gable - igable@uvic.ca
 *
 * """


__VERSION__ = '0.01'

import sys
import commands
import os
import logging
from cStringIO import StringIO
import time
from optparse import OptionParser
from xml.sax import make_parser
from xml.sax.handler import ContentHandler
import xml

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




PERFORMANCE_DATA_LOC = "/tmp/service-perfdata"
TARGET_XML_FILE = "/tmp/mdsresource.xml"

# This value specifies 300 seconds, or 5 minutes for out "sliding window" view into the 
# performance data. Change this value accordingly to fit the rest of the Nagios reporting
# intervals, but don't make it too large or "old" data will start creeping into the MDS, 
# which may not accurately describe the cluster state
TIME_WINDOW = 300

class NagiosPerfDataProcessor(PluginObject):
    """ This class takes all the various snippets of XML from the Nagios service-performance-file
    and aggregates them into a single, well formed XML document that can be utilized by our
    Nimbus MDS Aggregator
    """
    def __init__(self):
        PluginObject.__init__(self,self.__class__.__name__)
        self.parser = make_parser()
        self.curHandler = ResourceHandler()
        self.parsedXML = ""
        self.totalResources = []
        self.parser.setContentHandler(self.curHandler)

    def output(self, outputFile):
        
        try:
            fileHandle = open(outputFile, "w")

        except IOError:
            self.logger.error("Unable to open \'"+outputFile+"\' for writing!")
            sys.exit(-1)
        
        fileHandle.write(self.parsedXML.getvalue())

        fileHandle.close()

    def parse(self):

        finalXML = StringIO()
        finalXML.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        finalXML.write("<ROOT>")
        fileHandle = None
        if not (os.path.exists(PERFORMANCE_DATA_LOC)):
            try:
                print "THere's no file, making an empty one"
                fileHandle = open(PERFORMANCE_DATA_LOC,"w")
                fileHandle.close()    
            except IOError:
                self.logger.error("Unable to create empty file: "+PERFORMANCE_DATA_LOC)

        try:

            fileHandle = open(PERFORMANCE_DATA_LOC,"r")
            for line in fileHandle.readlines():
                
                lineEntries = line.split()
                serviceTime = int(lineEntries[1])
                systemTime = int(time.time())
                
                delta = abs(serviceTime - systemTime)
                if(delta > TIME_WINDOW):
                    continue
                # Ignore lines that don't contain the xml header that
                # our client plugins include as part of the transmission
                xmlHeaderIndex = line.find("<?xml")
                if (xmlHeaderIndex == -1):
                    continue
                else:
                    # This will only print the XML string from the found line
                    # To find the 'end' of the xml header and effectively 
                    # strip it off so the XML can be aggregated into 1 source
                    tagIndex = line.find("?>") + 2
                    resourceXMLEntry = line[tagIndex:]
                    finalXML.write(resourceXMLEntry)

        except IOError:
            self.logger.error("Unable to open \'"+PERFORMANCE_DATA_LOC +"\' for reading!")
            sys.exit(-1)

        fileHandle.close()
        
        finalXML.write("</ROOT>")
        self.parsedXML=finalXML
        xml.sax.parseString(finalXML.getvalue(), self.curHandler)
        self.totalResources = self.curHandler.getResources()
        return self.totalResources



class ResourceHandler(ContentHandler):
    """
    This class implements the SAX API functions 'startElement', 'endElement' and 'characters'
    It is also intimately tied to the XML format used by the client side plugins
    """  
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
            if(self.thirdLevelKey in self.collectedResources[self.topLevelKey][self.secondLevelKey].keys()):
                self.repeatedResource = True
    
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
        
if __name__ == '__main__':

    myProc = NagiosPerfDataProcessor()
    myProc.parse()
    myProc.output(TARGET_XML_FILE)

    sys.exit(0)
