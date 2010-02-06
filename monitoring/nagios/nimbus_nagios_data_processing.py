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




from cStringIO import StringIO
import xml
import xml.dom.pulldom
from xml.dom.pulldom import parseString, parse, START_ELEMENT
import sys
import logging
import libxml2
import libxslt
import time
import os

TARGET_XML_FILE = "xmlCapture.xml"#"/tmp/mdsresource.xml"
DUPLICATE_REM_XSL = "dupRemove.xsl"
OUTPUT_XML_FILE = "outputXML.xml"
TIME_WINDOW = 300

PERFORMANCE_DATA_LOC = "/tmp/service-perfdata"
TARGET_XML_FILE = "/tmp/mdsresource.xml"

class PluginObject:

    def __init__(self, callingClass):

        self.logger = logging.getLogger(callingClass)
        self.logger.setLevel(logging.INFO)
        formatter = logging.Formatter('%(asctime)s ; %(name)s ; %(levelname)s ; %(message)s')

        errorOutputHndlr = logging.StreamHandler(sys.stdout)
        errorOutputHndlr.setFormatter(formatter)
        errorOutputHndlr.setLevel(logging.ERROR)

        self.logger.addHandler(errorOutputHndlr)

class NagiosXMLAggregator(PluginObject):
    
    def __init__(self):
        PluginObject.__init__(self,self.__class__.__name__)
        self.xmlFile = None
        self.readXML = None


    def aggregateServiceDataToXML(self):

        retXML = StringIO()
        retXML.write("<WRAPPER>")
        if  (os.path.exists(PERFORMANCE_DATA_LOC)):
            try:
                fileHandle = open(PERFORMANCE_DATA_LOC,"r")
              
            except IOError, err:
                self.logger.error("IOError encountered opening "+PERFORMANCE_DATA_LOC+" for reading")

            for line in fileHandle.readlines():
                
                lineEntries = line.split()
                serviceTime = int(lineEntries[1])
                systemTime = int(time.time())
                
                delta = abs(serviceTime - systemTime)
                if(delta > TIME_WINDOW):
                    #print "Outside TIME WINDOW" 
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
                    retXML.write(resourceXMLEntry.strip())
            retXML.write("</WRAPPER>") 
            return retXML.getvalue().strip()

    def outputXMLToFile(self, xmlToOutput):

        try:

            fileHandle = open(TARGET_XML_FILE,"w")
            fileHandle.write(xmlToOutput)
            fileHandle.close()
        except IOError, err:
            self.logger.error("IOError thrown trying to output XML to: "+TARGET_XML_FILE+" - "+str(err))
            sys.exit(-1)

    def removeDuplicateIPs(self, xmlToProcess):

        try:
            styledoc = libxml2.parseFile(DUPLICATE_REM_XSL)
        except libxml2.parserError, err:
            self.logger.error("Unable to parse XSLT: "+DUPLICATE_REM_XSL+" "+str(err))
            sys.exit(-1)

        style = libxslt.parseStylesheetDoc(styledoc)
        try:
            doc = libxml2.parseDoc(xmlToProcess)
        except libxml2.parserError, err:
            self.logger.error("Unable to parse desired XML in removeDuplcateIPs: "+str(err))
            sys.exit(-1)

        result = style.applyStylesheet(doc, None)
        #style.saveResultToFilename(OUTPUT_XML_FILE, result, 0)
        retXML = style.saveResultToString(result)
        
        style.freeStylesheet()
        doc.freeDoc()
        result.freeDoc()
        
        return str(retXML.strip())

    def __call__(self):

        self.readXML = self.aggregateServiceDataToXML()
        #print self.readXML
        doc = parseString(self.readXML)

        finalXML = StringIO()
        finalXML.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        finalXML.write("<Cluster>")
        finalXML.write("<HeadNode>")

        for event, node in doc:
            if event == xml.dom.pulldom.START_ELEMENT and node.localName == "HeadNode":
                doc.expandNode(node)
                tempString = node.toxml()

                finalXML.write(tempString[10:-11])
        finalXML.write("</HeadNode>")
        finalXML.write("<WorkerNodes>")
        doc = parseString(self.readXML)
        for event, node in doc:
            if event == xml.dom.pulldom.START_ELEMENT and node.localName =="WorkerNode":
                doc.expandNode(node)
                tempString = node.toxml()
                finalXML.write(tempString) 
        finalXML.write("</WorkerNodes>")
        finalXML.write("</Cluster>")

        return self.removeDuplicateIPs(finalXML.getvalue())


if __name__ == '__main__':
    
    xmlAggr = NagiosXMLAggregator()
    xml = xmlAggr()
    #print xml
    xmlAggr.outputXMLToFile(xml)

    sys.exit(0)    
