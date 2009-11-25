import zope.interface
import workspacecontrol.api

class ILocalFile(workspacecontrol.api.IWCObject):
    """LocalFile is one VM file, the information is consulted or edited by many
    wcmodules.
    
    In the future, a remotely mounted device that *appears* to be a local file
    (such as an iscsi import) should be able to be handled transparently as
    well.  This is one reason there is an "editable" attribute.
    """
    
    path = zope.interface.Attribute(
    """path is a local, absolute path to the particular file in question
    """)
    
    mountpoint = zope.interface.Attribute(
    """mountpoint is the "target" mountpoint for those VMs that need to be set
    up with such information (such as Xen VMs).
    """)
    
    rootdisk = zope.interface.Attribute(
    """rootdisk is True when this file should be launched as the rootdisk for
    those VMs that need to be set up with such information (such as Xen VMs).
    """)
    
    read_write = zope.interface.Attribute(
    """read_write is True when this file should be mounted as writable by the
    VM instance.  This option may only make sense for some platforms, some may
    just ignore.
    """)
    
    editable = zope.interface.Attribute(
    """editable is True when this file can be locally edited (for example using
    the mount + edit + umount technique).  This might not be true for some 
    files, for example iscsi imports that only appear as a local file/directory.
    """)