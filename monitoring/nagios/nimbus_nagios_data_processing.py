#!/usr/bin/python

from cStringIO import StringIO
import xml
import ConfigParser
from xml.dom.pulldom import parseString, parse, START_ELEMENT
import sys
import logging
import libxml2
import libxslt
import time
import os

RET_CRITICAL = -1

# Must specify the full path as Nagios doesn't setup a complete environment
# when it calls this script
REMOVE_DUP_XSL = "/usr/local/nagios/libexec/removeAllDup.xsl"
MERGE_NODES_XSL = "/usr/local/nagios/libexec/merge.xsl"
ATTRIBUTE_STRIP_XSL = "/usr/local/nagios/libexec/attribStripper.xsl"
XSD = "iaas.xsd"

#PERFORMANCE_DATA_LOC = "/tmp/service-perfdata"
#TARGET_XML_FILE = "/tmp/mdsresource.xml"

CONF_FILE = "/usr/local/nagios/libexec/monitoring_config.cfg"

NM_CONF_FILE_SECTION = "Nimbus_Monitoring"
SERVER_TMP_LOCATION = "Server_Tmp_Location"
NAGIOS_LOCATION = "Nagios_Location"


CONF_FILE_SECTION = "Nagios_Data_Processing"
PERFORMANCE_DATA_LOC = "Nagios_Performance_Data_Loc"
TARGET_XML_FILE = "Target_XML_File"
TIME_WINDOW = "Time_Window"

ConfigMapping = {}

def loadConfig(logger):

    cfgFile = ConfigParser.ConfigParser()
    if(os.path.exists(CONF_FILE)):
        cfgFile.read(CONF_FILE)
        try:
            ConfigMapping[SERVER_TMP_LOCATION] = cfgFile.get(NM_CONF_FILE_SECTION, SERVER_TMP_LOCATION,0)
            ConfigMapping[NAGIOS_LOCATION] = cfgFile.get(NM_CONF_FILE_SECTION, NAGIOS_LOCATION,0)

            ConfigMapping[PERFORMANCE_DATA_LOC] = cfgFile.get(CONF_FILE_SECTION, PERFORMANCE_DATA_LOC,0)
            ConfigMapping[TARGET_XML_FILE] = cfgFile.get(CONF_FILE_SECTION, TARGET_XML_FILE,0)
            ConfigMapping[TIME_WINDOW] = cfgFile.get(CONF_FILE_SECTION, TIME_WINDOW,0)
        except ConfigParser.NoSectionError:
            logger.error("Unable to locate "+CONF_FILE_SECTION+" section in conf file - Malformed config file?")
            sys.exit(RET_CRITICAL)
        except ConfigParser.NoOptionError, nopt:
            logger.error( nopt.message+" of configuration file")
            sys.exit(RET_CRITICAL)
    else:
        logger.error("Configuration file not found in Nagios Plug-ins directory")
        sys.exit(RET_CRITICAL)


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
        loadConfig(self.logger)
        self.xmlFile = None
        self.readXML = None

    # This method runs through the Nagios service-performance-data file and extracts all the pertinent XML snippets from it
    # A time window is defined to cut down on the number of duplicate entries "extracted". Duplicates can/will be removed
    # by an XSLT later in the code
    def aggregateServiceDataToXML(self):

        retXML = StringIO()
        retXML.write("<WRAPPER>")
        if  (os.path.exists(ConfigMapping[PERFORMANCE_DATA_LOC])):
            try:
                fileHandle = open(ConfigMapping[PERFORMANCE_DATA_LOC],"r")
              
            except IOError, err:
                self.logger.error("IOError encountered opening "+ConfigMapping[PERFORMANCE_DATA_LOC]+" for reading")
                sys.exit(RET_CRITICAL)

            for line in fileHandle.readlines():
                
                lineEntries = line.split()
                serviceTime = int(lineEntries[1])
                systemTime = int(time.time())
                
                delta = abs(serviceTime - systemTime)
                if(delta > ConfigMapping[TIME_WINDOW]):
                    #print "Outside TIME WINDOW" 
                    continue
                # Ignore lines that don't contain the xml header that
                # our client plugins include as part of the transmission
                # Presumably, only the data this script is interested in will have an XML header
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
            fileHandle = open(ConfigMapping[TARGET_XML_FILE],"w")
            fileHandle.write(xmlToOutput)
            fileHandle.close()
        except IOError, err:
            self.logger.error("IOError thrown trying to output XML to: "+ConfigMapping[TARGET_XML_FILE]+" - "+str(err))
            sys.exit(RET_CRITICAL)

    # This method applies the given stylesheet to the passed in XML and returns the results - simple
    def applyStyleSheet(self, styleSheetName, xmlToProcess):

        try:
            styledoc = libxml2.parseFile(styleSheetName)
        except libxml2.parserError, err:
            self.logger.error("Unable to parse XSLT: "+styleSheetName+" "+str(err))
            sys.exit(RET_CRITICAL)

        style = libxslt.parseStylesheetDoc(styledoc)
        try:
            doc = libxml2.parseDoc(xmlToProcess)
        except libxml2.parserError, err:
            self.logger.error("Unable to parse desired XML: "+str(err))
            sys.exit(-1)

        result = style.applyStylesheet(doc, None)
        retXML = style.saveResultToString(result)
        
        style.freeStylesheet()
        doc.freeDoc()
        result.freeDoc()
        
        return str(retXML.strip())

    def validateXML(self, xmlToProcess):

        ctxtParser = libxml2.schemaNewParserCtxt(XSD)
        ctxtSchema = ctxtParser.schemaParse()
        ctxtValid = ctxtSchema.schemaNewValidCtxt()

        doc = libxml2.parseDoc(xmlToProcess)
        retVal = doc.schemaValidateDoc(ctxtValid)
        if( retVal != 0):
            self.logger.error("Error validating against XML Schema - "+XSD)
            sys.exit(RET_CRITICAL)
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
