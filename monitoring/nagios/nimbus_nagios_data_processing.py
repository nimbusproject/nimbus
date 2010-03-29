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


from cStringIO import StringIO
import xml
import ConfigParser
from xml.dom.pulldom import parseString, parse, START_ELEMENT
import sys
#from nimbus_nagios_logger import Logger
import logging
import libxml2
import libxslt
import time
import os


__VERSION__ = '1.0'

CONF_FILE = "/usr/local/nagios/libexec/monitoring_config.cfg"

RET_CRITICAL = -1

# 1024 kB = 1 MB ; This size was picked arbitrarily
LARGE_FILE_SIZE = 1024

# Must specify the full path as Nagios doesn't setup a complete environment
# when it calls this script
REMOVE_DUP_XSL = "libexec/nimbus_nagios_rem_dup_nodes.xsl"
MERGE_NODES_XSL = "libexec/nimbus_nagios_merge_nodes.xsl"
ATTRIBUTE_STRIP_XSL = "libexec/nimbus_nagios_rem_attrib.xsl"
XSD = "cloud.xsd"

NM_CONF_FILE_SECTION = "Nimbus_Monitoring"
SERVER_TMP_LOCATION = "Server_Tmp_Location"
NAGIOS_LOCATION = "Nagios_Location"

CONF_FILE_SECTION = "Nagios_Data_Processing"
PERFORMANCE_DATA_LOC = "Nagios_Performance_Data_Loc"
TARGET_XML_FILE = "Target_XML_File"
TIME_WINDOW = "Time_Window"
CLOUD_NAME = "Cloud_Name"

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

        # Since the Cloud_Name entry is optional, load it with its own exception handling wrappers so that we can ignore the exception generated if we try to load it and it is not present in the config file

        try:
            ConfigMapping[CLOUD_NAME] = cfgFile.get(CONF_FILE_SECTION, CLOUD_NAME,0)
        except ConfigParser.NoSectionError:
            logger.error("Unable to locate "+CONF_FILE_SECTION+" section in conf file - Malformed config file?")
            sys.exit(RET_CRITICAL)
        except ConfigParser.NoOptionError:
            ConfigMapping[CLOUD_NAME] = ""
    else:
        logger.error("Configuration file not found in Nagios Plug-ins directory")
        sys.exit(RET_CRITICAL)


class Logger:
    """ A class to encapsulate useful logging features and setup

    """
    def __init__(self, name, errorLogFile):

        self.logger = logging.getLogger(name)

        self.logger.setLevel(logging.INFO)
        formatter = logging.Formatter('%(asctime)s : %(name)s : %(levelname)s : %(message)s')

        nagiosObservableHndlr = logging.StreamHandler(sys.stdout)
        nagiosObservableHndlr.setLevel(logging.INFO)
        nagiosObservableHndlr.setFormatter(formatter)

        fileOutputHndlr = logging.FileHandler(errorLogFile)
        fileOutputHndlr.setFormatter(formatter)
        fileOutputHndlr.setLevel(logging.DEBUG)

        self.logger.addHandler(fileOutputHndlr)
        self.logger.addHandler(nagiosObservableHndlr)

    def warning(self, msg):
        self.logger.warning(msg)

    def info(self, msg):
        self.logger.info(msg)

    def error(self, msg):
        self.logger.error(msg)

    def debug(self, msg):
        self.logger.debug(msg)


class NagiosXMLAggregator:
    
    def __init__(self):
        self.logger = Logger("NagiosXMLAggregator","nimbus_nagios_data_processing.log")
        loadConfig(self.logger)
        self.xmlFile = None
        self.readXML = None

    # This method runs through the Nagios service-performance-data file and extracts all the pertinent XML snippets from it
    # A time window is defined to cut down on the number of duplicate entries "extracted". Duplicates can/will be removed
    # by an XSLT later in the code
    def aggregateServiceDataToXML(self):

        retXML = StringIO()
        # The "<WRAPPER>...</WRAPPER>" tags are used to ensure the XML is well formed for parsing and are temporary.
        # These tags will NOT appear in any XML and are only used internally in this file
        retXML.write("<WRAPPER>")
        if  (os.path.exists(ConfigMapping[PERFORMANCE_DATA_LOC])):

            # getsize returns the size of the file in bytes
            curFileSize =  os.path.getsize(ConfigMapping[PERFORMANCE_DATA_LOC])/(1024)
            #print curFileSize
            if(curFileSize > LARGE_FILE_SIZE):
                self.logger.warning("The service performance data file: "+ConfigMapping[PERFORMANCE_DATA_LOC]+" has grown to "+str(curFileSize)+" kB")
                #self.logger.warning("This will slow down the data processing script")    
            try:
                fileHandle = open(ConfigMapping[PERFORMANCE_DATA_LOC],"r")
              
            except IOError, err:
                self.logger.error("IOError encountered opening "+ConfigMapping[PERFORMANCE_DATA_LOC]+" for reading")
                sys.exit(RET_CRITICAL)


            # Since "old" data accumulates in the service-perf-data file generated by Nagios, a sliding time window 
            # is used to only process "recent" data. The default configured time window is set in the config file, 
            # so this can be modified depending on how often the Nagios plug-ins are configured to report. Duplicates
            # should they exist will be handled by XSL transforms later in this code
            for line in fileHandle.readlines():
                
                serviceTime = int(line.split()[1])
                systemTime = int(time.time())
                
                delta = abs(serviceTime - systemTime)
                if(delta > int(ConfigMapping[TIME_WINDOW])):
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
                    retXML.write(line[tagIndex:].strip())
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
    # The order and functionality was gathered from the libxml2 python examples included with the libxml2 package
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
            sys.exit(RET_CRITICAL)

        result = style.applyStylesheet(doc, None)
        retXML = style.saveResultToString(result)
        
        style.freeStylesheet()
        doc.freeDoc()
        result.freeDoc()
        
        del style
        del doc
        del result
        return str(retXML.strip())

    # XML Validation occurs against the configured XSD file according to the libxml2 examples included with the
    # python packages
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

    # The main function of this file, it handles all the various tasks of aggregation and validation  
    def __call__(self):

        self.readXML = self.aggregateServiceDataToXML()
        doc = parseString(self.readXML)

        finalXML = StringIO()
        finalXML.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        finalXML.write("<Cloud xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\""+XSD+"\">")

        headNodeXML = StringIO()
        workerNodeXML = StringIO()
        # Here the pulldom API is used to extract the XML nodes under any "HeadNode" tags and write them to the finalXML for XSLT processing
        for event, node in doc:
            if event == xml.dom.pulldom.START_ELEMENT:
                
                if node.localName == "HeadNode":
                    doc.expandNode(node)
                    tempString = node.toxml()
                    # The fancy string index [10:-11] is used to eliminate the <HeadeNode></HeadNode> tags from the output
                    headNodeXML.write(tempString[10:-11])
                if node.localName =="Node":
                    doc.expandNode(node)
                    tempString = node.toxml()
                    workerNodeXML.write(tempString)

        finalXML.write("<HeadNode>") 
        # This tag is added for the "Optional Cloud Name" of the public XML schema. An 'id' attribute MUST be specified or the XSLs will remove this CloudName tag from the final XML. The 'id' is arbritrary
        finalXML.write("<CloudName id='arbitrary11235813'>"+ConfigMapping[CLOUD_NAME]+"</CloudName>")
        finalXML.write(headNodeXML.getvalue())
        finalXML.write("</HeadNode>")
        finalXML.write("<WorkerNodes>")
        finalXML.write(workerNodeXML.getvalue())
        finalXML.write("</WorkerNodes>")
        finalXML.write("</Cloud>")

        # The various stylesheets are applied "serially" to the final XML to pepare it for publishing 
        return self.applyStyleSheet(ConfigMapping[NAGIOS_LOCATION]+ATTRIBUTE_STRIP_XSL,self.applyStyleSheet(ConfigMapping[NAGIOS_LOCATION]+MERGE_NODES_XSL,self.applyStyleSheet(ConfigMapping[NAGIOS_LOCATION]+REMOVE_DUP_XSL,finalXML.getvalue())))

if __name__ == '__main__':
    
    xmlAggr = NagiosXMLAggregator()
    xml = xmlAggr()
    print xml
    xmlAggr.validateXML(xml)
    xmlAggr.outputXMLToFile(xml)

    sys.exit(0)    
