import commands
import os
import shutil
import zope.interface

import workspacecontrol.api.modules
from workspacecontrol.api.exceptions import *

TMPDIR_PREFIX = "/tmp/workspace-control-tests__delete-me__"

class TestProcurement:
    """Implementation of procurement that 'obtains' a fake image set, intended
    to be used for tests that don't involve real VMM interactions.
    
    It can however be used for some VM interactions, e.g. if code were to
    mount and edit the filesystem, that would work.  The files created are
    actual ext3 filesystem images.
    
    Needs access to /tmp, it will create a directory (see module constant)
    during a test.  That directory will be deleted on teardown.
    """
    
    zope.interface.implements(workspacecontrol.api.modules.IImageProcurement)
    
    def __init__(self, params, common):
        self.p = params
        self.c = common
    
    def validate(self):
        pass
    
    def obtain(self):
        
        # Create /tmp/___ dir
        uuid = commands.getoutput("uuidgen")
        tmpdir = TMPDIR_PREFIX + uuid
        
        if os.access(tmpdir, os.F_OK):
            raise IncompatibleEnvironment("unit test tmp directory exists, somehow: '%s'" % tmpdir)
            
        os.mkdir(tmpdir)
        self.c.log.debug("created temporary test directory '%s'" % tmpdir)
        
        lf_cls = self.c.get_class_by_keyword("LocalFile")
        
        lf1 = lf_cls()
        lf1.path = os.path.join(tmpdir, "root.img")
        lf1.mountpoint = "sda1"
        lf1.rootdisk = True
        lf1.read_write = True
        lf1.editable = True
        
        lf2 = lf_cls()
        lf2.path = os.path.join(tmpdir, "home.img")
        lf2.mountpoint = "sda2"
        lf2.rootdisk = False
        lf2.read_write = True
        lf2.editable = True
        
        lf3 = lf_cls()
        lf3.path = os.path.join(tmpdir, "usr.img")
        lf3.mountpoint = "sda3"
        lf3.rootdisk = False
        lf3.read_write = False
        lf3.editable = False
        
        lfs_cls = self.c.get_class_by_keyword("LocalFileSet")
        local_file_set = lfs_cls([lf1,lf2,lf3])
        
        for lf in local_file_set.flist():
            self._mk_new_ext3(lf.path)
        return local_file_set
        
    def _mk_new_ext3(self, path):
        if self.c.trace:
            self.c.log.debug("------------------------------------------------")
            self.c.log.debug("creating new test image: '%s'" % path)
        
        if not os.path.isabs(path):
            raise ProgrammingError("this only works on absolute paths")
        
        if os.access(path, os.F_OK):
            raise IncompatibleEnvironment("unit test tmp file exists, somehow: '%s'" % path)
        
        # touch file
        f = None
        try:
            f = open(path, 'w')
        finally:
            if f:
                f.close()
                del f
        if self.c.trace:
            self.c.log.debug("created zero byte file")
        
        cmd = "dd if=/dev/zero of=%s bs=1M count=5" % path
        self.c.log.info("running '%s'" % cmd)
        
        (status, output) = commands.getstatusoutput(cmd)
        if status != 0:
            raise UnexpectedError("could not create test image file; return code %d\ncommand run '%s'\noutput: '%s'" % (status, cmd, output))
            
        if self.c.trace:
            self.c.log.debug("created file of zeroes")
            
        cmd = "/sbin/mke2fs -F -j %s" % path
        self.c.log.info("running '%s'" % cmd)
        
        (status, output) = commands.getstatusoutput(cmd)
        if status != 0:
            raise UnexpectedError("could not create filesystem on test image; return code %d\ncommand run '%s'\noutput: '%s'" % (status, cmd, output))
    
        if self.c.trace:
            self.c.log.debug("created ext3 filesystem, done.")
            self.c.log.debug("------------------------------------------------")
    
    def process_after_shutdown(self, local_file_set):
        pass
        
    def process_after_destroy(self, local_file_set):
        
        # only delete tmp dir if all the files in this set have the same
        # prefix as in the module constant
        
        if not local_file_set:
            raise ProgrammingError("no file set")
            
        flist = local_file_set.flist()
        if not flist:
            raise ProgrammingError("no file set in local_file_set wrapper")
            
        if len(flist) == 0:
            raise ProgrammingError("no files local file set's list")
        
        for lf in flist:
            if not lf.path:
                raise ProgrammingError("no path to image in local file set")
            if not lf.path.startswith(TMPDIR_PREFIX):
                raise UnexpectedError("file set cannot be processed after destroy: unknown source, not managed by this image procurement module.  Unknown path: '%s'" % lf.path)
        
        commondir = self._jump_up_dir(flist[0].path)
        for lf in flist:
            if self._jump_up_dir(lf.path) != commondir:
                raise UnexpectedError("file set cannot be processed after destroy: unknown source, not managed by this image procurement module.  Paths to images are in multiple directories: '%s' but commondir is '%s'" % (lf.path, commondir))
        
        self.c.log.info("removing temp image dir '%s'" % commondir)
        shutil.rmtree(commondir)
    
    def _jump_up_dir(self, path):
        return "/".join(os.path.dirname(path+"/").split("/")[:-1])
        