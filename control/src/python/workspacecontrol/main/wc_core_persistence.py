import os
import pickle
import stat
import sys
from workspacecontrol.api.exceptions import *
import workspacecontrol.main.wc_args as wc_args

class Persistence:
    def __init__(self, params, common):
        self.p = params
        self.c = common
        self.pdir = None
        
    def validate(self):
        pdir = self.p.get_conf_or_none("persistence", "persistencedir")
        if not pdir:
            raise InvalidConfig("There is no persistence->persistencedir configuration")
            
        if not os.path.isabs(pdir):
            pdir = self.c.resolve_var_dir(pdir)
            
        if not os.path.exists(pdir):
            try:
                os.mkdir(pdir)
                self.c.log.debug("created persistence directory: %s" % pdir)
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                errstr = "problem creating persistence dir '%s': %s: %s" % (pdir, str(exceptname), str(sys.exc_value))
                
                fullerrstr = "persistence directory does not have valid permissions and cannot be made to have valid permissions: '%s'" % pdir
                fullerrstr += errstr
                self.c.log.error(fullerrstr)
                raise IncompatibleEnvironment(fullerrstr)
            
        if os.access(pdir, os.W_OK | os.X_OK | os.R_OK):
            self.c.log.debug("persistence directory is rwx-able: %s" % pdir)
        else:
            try:
                os.chmod(pdir, stat.S_IRUSR | stat.S_IWUSR | stat.S_IXUSR)
            except:
                exception_type = sys.exc_type
                try:
                    exceptname = exception_type.__name__ 
                except AttributeError:
                    exceptname = exception_type
                errstr = " - problem changing persistence directory permissions '%s': %s: %s" % (pdir, str(exceptname), str(sys.exc_value))
                
                fullerrstr = "persistence directory does not have valid permissions and cannot be made to have valid permissions: '%s'" % pdir
                fullerrstr += errstr
                self.c.log.error(fullerrstr)
                raise IncompatibleEnvironment(fullerrstr)
                
            self.c.log.debug("persistence directory was made to be rwx-able: %s" % pdir)
            
        self.pdir = pdir
    
    def store_nic_set(self, vm_name, nic_set, ok_to_replace=False):
        if not self.pdir:
            raise ProgrammingError("cannot persist anything without setup/validation")
            
        if not nic_set:
            raise ProgrammingError("no nic_set")
        nics = nic_set.niclist()
        if not nics:
            raise ProgrammingError("no nics")
            
        pobject = self._derive_nicset_filepath(vm_name)
        
        if not ok_to_replace:
            if os.path.exists(pobject):
                raise UnexpectedError("The persistence file already exists: '%s'" % pobject)
        
        f = None
        try:
            f = open(pobject, 'w')
            pickle.dump(nic_set, f)
        finally:
            if f:
                f.close()
    
    def get_nic_set(self, vm_name):
        if not self.pdir:
            raise ProgrammingError("cannot use persistence without setup/validation")
        
        pobject = self._derive_nicset_filepath(vm_name)
        if not os.path.exists(pobject):
            return None
            
        f = None
        try:
            f = open(pobject, 'r')
            x = pickle.load(f)
            return x
        finally:
            if f:
                f.close()
        
    def remove_nic_set(self, vm_name):
        if not self.pdir:
            raise ProgrammingError("cannot use persistence without setup/validation")
        pobject = self._derive_nicset_filepath(vm_name)
        if not os.path.exists(pobject):
            return
        os.remove(pobject)
        
    def _derive_nicset_filepath(self, vm_name):
        if not vm_name:
            raise ProgrammingError("no vm_name")
        path = os.path.join(self.pdir, vm_name + "-nicset")
        if path.find("..") != -1:
            raise UnexpectedError("The persistence file is an invalid path: '%s'" % path)
        return path

