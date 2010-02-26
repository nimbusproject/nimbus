#!/usr/bin/python

from cStringIO import StringIO
import xml
#import xml.dom.pulldom
from xml.dom.pulldom import parseString, parse, START_ELEMENT
import sys
import logging
import libxml2
import libxslt
import time
import os


# Must specify the full path as Nagios doesn't setup a complete environment
# when it calls this script
REMOVE_DUP_XSL = "/usr/local/nagios/libexec/removeAllDup.xsl"
MERGE_NODES_XSL = "/usr/local/nagios/libexec/merge.xsl"
ATTRIBUTE_STRIP_XSL = "/usr/local/nagios/libexec/attribStripper.xsl"

TIME_WINDOW = 600

PERFORMANCE_DATA_LOC = "/tmp/service-perfdata"
TARGET_XML_FILE = "/tmp/mdsresource.xml"

class PluginObject:

    def __init__(self, callingClass):

        self.logger = logging.getLogger(callingClass)
        self.logger.setLevel(logging.INFO)
        formatter = logging.Formatter('%(asctime)s ; %(name)s ; %(levelname)s ; %(message)s')

        errorOutputHndlr = logging.FileHandler("nimbus_nagios_data_processor.log")
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

    def applyStyleSheet(self, styleSheetName, xmlToProcess):

        try:
            styledoc = libxml2.parseFile(styleSheetName)
        except libxml2.parserError, err:
            self.logger.error("Unable to parse XSLT: "+styleSheetName+" "+str(err))
            sys.exit(-1)

        style = libxslt.parseStylesheetDoc(styledoc)
        try:
            doc = libxml2.parseDoc(xmlToProcess)
        except libxml2.parserError, err:
            self.logger.error("Unable to parse desired XML: "+str(err))
            sys.exit(-1)

        result = style.applyStylesheet(doc, None)
        #style.saveResultToFilename(OUTPUT_XML_FILE, result, 0)
        retXML = style.saveResultToString(result)
        
        style.freeStylesheet()
        doc.freeDoc()
        result.freeDoc()
        
        return str(retXML.strip())


    def validateXML(self, xmlToProcess):

       # theDTD = libxml2.parseDTD(None, "iaas.dtd")
       # dtdCtx = libxml2.newValidCtxt()

       # doc = libxml2.parseDoc(xmlToProcess)
       # retVal = doc.validateDtd(dtdCtx, theDTD)
       # if( retVal != 1):
       #     self.logger.error("Error validating XML against DTD!")
       #     sys.exit(-1)
       # doc.freeDoc()
       # theDTD.freeDtd()
       # del theDTD
       # del dtdCtx
       # libxml2.cleanupParser()

        ctxtParser = libxml2.schemaNewParserCtxt("iaas.xsd")
        ctxtSchema = ctxtParser.schemaParse()
        ctxtValid = ctxtSchema.schemaNewValidCtxt()

        doc = libxml2.parseDoc(xmlToProcess)
        retVal = doc.schemaValidateDoc(ctxtValid)
        if( retVal != 0):
            self.logger.error("Error validating against XML Schema - iaas.xsd")
            sys.exit(-1)
        doc.freeDoc()
        del ctxtParser
        del ctxtSchema
        del ctxtValid
        libxml2.schemaCleanupTypes()
        libxml2.cleanupParser() 


    def __call__(self):

        self.readXML = self.aggregateServiceDataToXML()
        doc = parseString(self.readXML)

        finalXML = StringIO()
        finalXML.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        #finalXML.write("<!DOCTYPE Cloud SYSTEM \"iaas.dtd\">")
        finalXML.write("<Cloud xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"iaas.xsd\">")
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
            if event == xml.dom.pulldom.START_ELEMENT and node.localName =="Node":
                doc.expandNode(node)
                tempString = node.toxml()
                finalXML.write(tempString) 
        finalXML.write("</WorkerNodes>")
        finalXML.write("</Cloud>")
        #print finalXML.getvalue()
 
        return self.applyStyleSheet(ATTRIBUTE_STRIP_XSL,self.applyStyleSheet(MERGE_NODES_XSL,self.applyStyleSheet(REMOVE_DUP_XSL,finalXML.getvalue())))

if __name__ == '__main__':
    
    xmlAggr = NagiosXMLAggregator()
    xml = xmlAggr()
    print xml
    xmlAggr.validateXML(xml)
    xmlAggr.outputXMLToFile(xml)

    sys.exit(0)    
