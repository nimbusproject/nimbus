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
 * OR    
 * HEPNet Technical Manager - Ian Gable - igable@uvic.ca
 *
 * """


__VERSION__ = '0.01'

import sys
import commands
import os
import logging
from cStringIO import StringIO
import libvirt
from optparse import OptionParser
import socket

# NAGIOS Plug-In API return code values

NAGIOS_RET_OK = 0
NAGIOS_RET_WARNING = 1
NAGIOS_RET_CRITICAL = 2
NAGIOS_RET_UNKNOWN = 3


def pluginExit(messageString, logString, returnCode):
    """ This method should be the only exit point for all the plug-ins. This ensures that 
    Nagios requirements are meant and performance data is properly formatted to work
    with the rest of the code. Do NOT just call sys.exit in the code (if you want your
    plug-in to function with the rest of the code!
    """
    # ALRIGHT, so the log string is seperated by  my "delimiter" ';'
    # Thus, I'm going to assume the following log style format:

    # 2009-05-29 13:48:55,638 ; VMMemory ; INFO ; sl52base ; MEMORY ; 524288

    # The pertinent information should be located in the "4th col" and on
    # The 3rd col lists the "logger lvl", which I'm using to indicate
    # if it's standard plug-in output or an error


    outputString = StringIO()
    outputString.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
    
    # I need to check if ANY 'ERROR' log entries exist. If that's the case
    # then a different XML string should be formatted and sent out

    # There doesn't seem to be any error handling I can do here... Ugly
    localIP = (socket.gethostbyaddr( socket.gethostname() ))[2][0]
    
    resourceString = "<RES LOC=\""+ localIP+"\" TYPE=\""+messageString+"\">"
    outputString.write(resourceString)
    lines = logString.splitlines()
    for line in lines:
        # If we encounter an 'error' entry in the logger, make sure to alert the NAGIOS admin
        if (line.find("ERROR") != -1):
            returnCode = NAGIOS_RET_WARNING
            continue
        logStringEntries = line.split(';')
        
        outputString.write("<ENTRY ID=\""+logStringEntries[3].strip()+"\">")

        outputString.write(logStringEntries[5].strip())
        outputString.write("</ENTRY>")
        outputString.write("</RES>")


    print messageString+" | "+ outputString.getvalue()
    sys.exit(returnCode)


class PluginObject:    
    """ The most 'senior' of the base classes. This class sets up appropriate logging mechanisms to 
    conform with Nagios' API and plug-in coding rules. The log format is also setup, and cannot
    be changed without breaking almost all the code. Don't change the log format!
    """
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

class Virtualized(PluginObject):
    """ This class is designed to be a "Abstract Base Class" in C++ terms, meaning it is intended to
    be derived from to complete functionality. All the libvirt lookups take place here to centralize
    logic
    """
    # Lookup the available "domains" from libvirt, and other usefull stuff
    def __init__(self):
        PluginObject.__init__(self,self.__class__.__name__)
        #Establish a connection to the local VM Hypervisor (XEN)
        self.VMConnection  = libvirt.openReadOnly(None)
        if self.VMConnection == None:
            self.logger.error('Unable to open a Read-Only connection to local XEN Hypervisor')
            pluginExit("Virtualized - Base Class",self.logString.getvalue(), NAGIOS_RET_ERROR)
        

        self.VMDomainsID = self.VMConnection.listDomainsID()
        self.VMs={}
        for id in self.VMDomainsID:
            #So, the VMs 'dictionary' stores virDomain (libvirt) objects
            try:
            # Skip over Dom0 or Domain-0 (hypervisor)
                if(id == 0):
                    continue
                self.VMs[id] = (self.VMConnection.lookupByID(id))
            except:
                self.logger.error("Failed to lookup a VM from a VMConnection by \'id\'")
                pluginExit("Virtualized - Base Class",self.logString.getvalue(), NAGIOS_RET_ERROR)

# I thought I'd have to define the __call__ function, but it seems like the 
# ctr is doing what I need it to for the 'Callback' functionality
class VMMemory(Virtualized):
    """ This class will look up the amount of memory/RAM currently allocated to each VM on the 
    local node. This is done through libvirt calls in the 'Virtualized' base class
    Used memory is reported back in kB.
    """
    #This ctr's interface is as such to match the 'callback' interface of the optionParser
    def __init__(self): #,option, opt_str, value, parser):
        Virtualized.__init__(self)
        self.resourceName = 'VM-Memory'

    def __call__(self, option, opt_str, value, parser):
        for vm in self.VMs.values():
            self.logger.info(vm.name()+' ; '+self.resourceName+ " ; %d", vm.maxMemory())

        pluginExit(self.resourceName, self.logString.getvalue(), NAGIOS_RET_OK)

class VMVirt(Virtualized):
    """ This class will determine what virtualization technology is being used on each Virtual Machine
    running on the local node. This is done through a libvirt call in the 'Virtualized' base class
    """
    def __init__(self):
        Virtualized.__init__(self)
        self.resourceName = 'VM-Virtualization'

    def __call__(self,option, opt_str, value,parser):
        self.logger.info(self.VMConnection.getHostname()+";"+self.resourceName+";"+self.VMConnection.getType())

        pluginExit(self.resourceName, self.logString.getvalue(), NAGIOS_RET_OK)

class VMCpuCores(Virtualized):
    """ This class will determine the number of CPU cores running on the local node. This is done through a libvirt
    call done in the 'Virtualized' base class
    """
    def __init__(self):
        Virtualized.__init__(self)
        self.resourceName = 'VM-CPUCores'

    def __call__(self, option, opt_str, value, parser):

        tempRes = self.VMConnection.getInfo()
        self.logger.info(self.VMConnection.getHostname()+';'+self.resourceName+';'+str(tempRes[6]))
        pluginExit(self.resourceName, self.logString.getvalue(), NAGIOS_RET_OK)        

class VMCpuFreq(Virtualized):
    """ This class determines the CPU frequency of the local nodes processor. This was tested with a uniprocessor
    machine only, so a multi-CPU-socket machine with different core speeds may report differently. I believe that
    only the latest, most cutting edge CPUs from Intel (Nehalem) have this capability....
    """ 
    
    def __init__(self):
        Virtualized.__init__(self)
        self.resourceName = 'VM-CPUFreq'

    def __call__(self,option, opt_str, value, parser):

        tempRes = self.VMConnection.getInfo()
        self.logger.info(self.VMConnection.getHostname()+';'+self.resourceName+';'+str(tempRes[3]))       
        pluginExit(self.resourceName, self.logString.getvalue(), NAGIOS_RET_OK)

ARCH_MAP = {    "i686"  : "x86",
                        "i386"  : "x86",
                        "x86_64":"x86_64"}
           # This entry seems rebarbative but it provides a uniform interface
class VMCpuArch(Virtualized):
    """ The class determines what the underlying CPU architecture is, and uses this information to determine
    whether the machine is running in 32bit or 64bit mode. This code was only tested on 32bit Sempron
    processors, so I'm relying on the fact that a 64bit machine running a 32bit OS will report either
    'i386' or 'i686' through libvirt.
    """

    def __init__(self):
        Virtualized.__init__(self)
        self.resourceName = 'VM-CPUArch'

    def __call__(self, option, opt_str, value,parser):
        tempRes = self.VMConnection.getInfo()
        self.logger.info(self.VMConnection.getHostname()+';'+self.resourceName+";"+ARCH_MAP[tempRes[0]])
        pluginExit(self.resourceName, self.logString.getvalue(), NAGIOS_RET_OK)

class VMOs(Virtualized):
    """ This class looks up what OS is running on each virtual machine deployed on the local node. An example
    is 'linux'. This lookup is done through a libvirt call in the 'Virtualized' base class
    """
    def __init__(self):
        Virtualized.__init__(self)
        self.resourceName = "VM-OS"

    def __call__(self, option, opt_str, value, parser):
        for vm in self.VMs.values():
            self.logger.info(vm.name()+' ; '+self.resourceName+ " ; "+ vm.OSType())

        pluginExit(self.resourceName, self.logString.getvalue(), NAGIOS_RET_OK)
        
class VMFreeMem(Virtualized):
    """ This class determines how much free memory remains on the local node with all current virtual
    machines booted. This lookup is again done through a libvirt call in the 'Virtualized' base
    class. ALso, memory is reported by in kB.
    """
    def __init__(self):
        Virtualized.__init__(self)
        self.resourceName = "VM-FreeMemory"

    def __call__(self,option,opt_str,value,parser):
        usedMemory = 0
        for vm in self.VMs.values():
            usedMemory = usedMemory + vm.maxMemory()

        tempRes = self.VMConnection.getInfo()
        totalMem = int(tempRes[1])*1024
        
        availableMem =totalMem -usedMemory
        self.logger.info(self.VMConnection.getHostname()+';'+self.resourceName+';'+str(availableMem))

        pluginExit(self.resourceName, self.logString.getvalue(), NAGIOS_RET_OK)



class PluginCmdLineOpts(PluginObject):
    """ This class acts as the "central dispatcher" for determining what resource will be reported back
    to Nagios. Command line parameters act as the switches and determine which of the above classes
    gets instantianted.
    """
    def __init__(self):
        PluginObject.__init__(self,self.__class__.__name__)
        # Parse command-line options.
        parser = OptionParser()

        #The following options are parsed to conform with Nagios Plug-In "standard practices"
        parser.add_option("-V","--version",dest="version", \
            action="store_false", help="Diplay version information",default=True)
        #parser.add_option("-v","--verbose",dest="verbosity",help="Set verbosity level (0-3)",default=0)

        parser.add_option("--VMmem", help="Discover the of memory dedicated to each VM (in KB)", action="callback", callback=VMMemory())
        parser.add_option("--VMos", help="Discover the OS running on each VM", action="callback", callback=VMOs())
        parser.add_option("--VMcpuarch",help="Discover the host CPU architecture (x86 or x86_64)", action="callback", callback=VMCpuArch())        
        parser.add_option("--VMvirt", help="Discover the host virtualization technology",action="callback", callback=VMVirt())
        parser.add_option("--VMcpufreq",help="Discover the host CPU frequency (in Hz)", action="callback", callback=VMCpuFreq())
        parser.add_option("--VMcpucores",help="Discover the number of host CPU cores", action="callback", callback=VMCpuCores())
        parser.add_option("--VMfreemem",help="Discover the amount of free/unsed memory of host",action="callback",callback=VMFreeMem())

        self.parser = parser

    # This method is also responsible for "picking" what resource to monitor via the appropriate
    # command line switches (which I need to define). I don't want a single, monolithic script
    # running for ALL the resources, since this waters down NAGIOS's monitoring capabilities
    # (since that would make only a single resource to monitor)
    # Instead, this one script will be executed multiple time with different commandline options
    # to facilitate the monitoring of the different resources independant of one another
    def validate(self):

        # Parse the command line arguments and store them in 'options'
        (options, args) = self.parser.parse_args()


# The "main" code starts here & begins execution here
testObject = PluginCmdLineOpts()
testObject.validate()

# This line should never be reached in any code path
sys.exit(NAGIOS_RET_CRITICAL)
