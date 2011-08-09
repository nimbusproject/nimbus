try:
    from cStringIO import StringIO
except:
    from StringIO import StringIO

from jinja2 import BaseLoader, TemplateNotFound
from os.path import join, exists, getmtime
from jinja2 import Template
from jinja2 import Environment, PackageLoader
import os    
import sys
import string
import re
    
class WSCTemplateLoader(BaseLoader):

    def __init__(self, path):
        self.path = path

    def get_source(self, environment, template):
        path = self.path
        if not exists(path):
            raise Exception("libvirt template not found %s" % (path))
        mtime = getmtime(path)

        f = None
        try:
            f = open(path)
            source = f.read().decode('utf-8')
        finally:
            if f:
                f.close()
        return source, path, lambda: mtime == getmtime(path)

def _xml_normalize_pretty(s):
    import xml.dom.minidom
    for c in string.whitespace:
        s = s.replace(c, " ")
    doc = xml.dom.minidom.parseString(s)
    uglyXml = doc.toprettyxml(indent='  ')
    text_re = re.compile('>\n\s+([^<>\s].*?)\n\s+</', re.DOTALL)
    prettyXml = text_re.sub('>\g<1></', uglyXml)
    text_re = re.compile(os.linesep + ' *' + os.linesep)
    prettyXml = text_re.sub(os.linesep, prettyXml)
    return prettyXml

# definitely don't want to parse XML by hand, but creating it with strings
# is not the end of the world

LINE_ONE = """<?xml version="1.0" encoding="utf-8"?>\n"""

def L(indent, content):
    spaces = ""
    if indent > 0:
        spaces = indent * "    "
    return spaces + content + "\n"

class Domain:
    def __init__(self, template=None):
        
        self._type = None # e.g. 'xen' or 'qemu'  <domain type='xen'>
        
        # see http://libvirt.org/formatdomain.html#elements
        self.name = None # string <name>     # (common)
        self.bootloader = None # string <bootloader>
        self.os = None # object <os>
        self.memory = 0 # integer <memory>   # (common)
        self.vcpu = 0 # integer <vcpu>       # (common)
        
        # see http://libvirt.org/formatdomain.html#elementsLifecycle
        self.on_poweroff = None # string     # (common)
        self.on_reboot = None # string       # (common)
        self.on_crash = None # string        # (common)
        
        # see http://libvirt.org/formatdomain.html#elementsDevices
        self.devices = None # object <devices>
        self._template = template

    def toXML(self):
        x = self._toXML_template()
        return x

    def _toXML_template(self):
        env = Environment(loader=WSCTemplateLoader(self._template))
        template = env.get_template(self._template)
        xml1 = template.render(domain=self, os=self.os, devices=self.devices)
        return _xml_normalize_pretty(xml1)


class OS:
    
    # Three Xen boot types:
    #
    #   BOOT_PARAV: Direct kernel boot (traditional Xen, kernels on host FS)
    #   BOOT_PYGRB: Host bootloader (pygrub)
    #   BOOT_HVM:   BIOS bootloader (hvm)
    #
    # Not supporting BOOT_HVM yet.
    #
    # TODO: qemu(kvm).. 
    # TODO: could support cdrom here eventually (if they auto-start SSHd)
    
    def __init__(self):
        
        # BOOT_PYGRB only needs 'type' ("xen")
        self.type = None # string
        # but it needs 'bootloader' in another level (...)
        
        # BOOT_PARAV needs 'type' "xen" with kernel
        # initrd and kernel cmdline args are optional
        self.kernel = None # string
        self.initrd = None # string
        self.cmdline = None # string
        
        # BOOT_HVM uses type 'hvm', 'loader' path, and a 'boot' element
        # No support for this currently
        
    def toXML(self):
        x = StringIO()
        x.write(L(1, "<os>"))
        
        x.write(L(2, "<type>%s</type>" % self.type))
        
        if self.kernel:
            x.write(L(2, "<kernel>%s</kernel>" % self.kernel))
        if self.initrd:
            x.write(L(2, "<initrd>%s</initrd>" % self.initrd))
        if self.cmdline:
            x.write(L(2, "<cmdline>%s</cmdline>" % self.cmdline))
        
        x.write(L(1, "</os>"))
        content = x.getvalue()
        x.close()
        return content

class Devices:
    def __init__(self):
        self.disks = [] # list of <disk> objects
        self.interfaces = [] # list of <interface> objects
        
    def toXML(self):
        x = StringIO()
        x.write(L(1, "<devices>"))
        
        for disk in self.disks:
            x.write(disk.toXML())
            
        for interface in self.interfaces:
            x.write(interface.toXML())
        
        x.write(L(1, "</devices>"))
        content = x.getvalue()
        x.close()
        return content
        
        
class Disk:
    def __init__(self):
        self._type = "file" # string (type="file")
        
        # <driver name='tap' type='aio'/>
        # will be set to either "tap:aio" or "file" by the lvrt_adapter*
        # If missing, assumed to be "file" (which triggers no <driver> element) 
        self.driver = None
        
        # <source file='/path/to/some.img'/>
        # <source dev='/dev/sdx1'/>
        self.source = None
        
        # <target dev='sda1'/>
        # <target dev='xvda' bus='xen'/>
        self.target = None
        
        self.readonly = False
        
    def toXML(self):
        x = StringIO()
        x.write(L(2, "<disk type='%s'>" % self._type))
        
        if self.driver:
            # file is ignored at the moment
            if self.driver == "tap:aio":
                x.write(L(3, "<driver name='tap' type='aio' />"))
        
        if self._type == "block":
            x.write(L(3, "<source dev='%s' />" % self.source))
        else:
            x.write(L(3, "<source file='%s' />" % self.source))
        x.write(L(3, "<target dev='%s' />" % self.target))
        if self.readonly:
            x.write(L(3, "<readonly/>"))
        
        x.write(L(2, "</disk>"))
        content = x.getvalue()
        x.close()
        return content
        
class Interface:
    def __init__(self):
        self._type = "bridge" # string (type="bridge")
        
        # <source bridge='xenbr0'/>
        self.source = None
        
        # <mac address='aa:00:00:00:00:11'/>
        self.mac = None
        
        # <target dev='wrksp-40-0'/>
        self.target = None
        
        # <script path='/etc/xen/scripts/vif-bridge'/>
        self.script_path = None
        
    def toXML(self):
        x = StringIO()
        x.write(L(2, "<interface type='%s'>" % self._type))
        
        x.write(L(3, "<source bridge='%s' />" % self.source))
        x.write(L(3, "<mac address='%s' />" % self.mac))
        
        if self.target:
            x.write(L(3, "<target dev='%s' />" % self.target))
        
        if self.script_path:
            x.write(L(3, "<script path='%s' />" % self.script_path))
        
        x.write(L(2, "</interface>"))
        content = x.getvalue()
        x.close()
        return content
