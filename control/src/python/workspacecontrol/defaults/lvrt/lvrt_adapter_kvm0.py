import os
import struct

from lvrt_adapter import PlatformAdapter, PlatformInputAdapter
from workspacecontrol.api.exceptions import *
import lvrt_model


class vmmadapter(PlatformAdapter):

    def __init__(self, params, common):
        PlatformAdapter.__init__(self, params, common)
        other_uri = self.p.get_conf_or_none("libvirt_connections", "kvm0")
        if other_uri:
            self.connection_uri = other_uri
        else:
            self.connection_uri = "qemu:///system"
        self.c.log.debug("KVM libvirt URI: '%s'" % self.connection_uri)

    def validate(self):
        self.c.log.debug("validating libvirt kvm adapter")


class intakeadapter(PlatformInputAdapter):
    def __init__(self, params, common):
        PlatformInputAdapter.__init__(self, params, common)

    def fill_model(self, dom, local_file_set, nic_set, kernel):
        dom._type = "kvm"
        dom.os.type = "hvm"

        for disk in dom.devices.disks:
            imagepath = disk.source
            f = open(imagepath, 'r')
            magic = f.read(4)

            # Version number (1 or 2) is in big endian format.
            # We only support version 2 (qcow2).
            be_version = f.read(4)
            version = struct.unpack('>I', be_version)[0]
            f.close()

            if magic[0:3] == 'QFI' and version == 2:
                # The partition is a qcow2 image
                disk.driver = "qemu:qcow2"
