import logging
import os
import time
import zope.interface

import workspacecontrol.api.objects
from workspacecontrol.api.exceptions import *
import workspacecontrol.main.wc_args as wc_args

# used for default object impls, modules are not required to use this:
from workspacecontrol.main import get_class_by_keyword as main_gcbk

# -----------------------------------------------------------------------------

_logleveldict = {0:logging.DEBUG,
                 1:logging.DEBUG,
                 2:logging.INFO,
                 3:logging.WARNING,
                 4:logging.ERROR}

class DefaultCommon:
    
    zope.interface.implements(workspacecontrol.api.objects.ICommon)
    
    def __init__(self, p):
        self.p = p
        if not p:
            raise InvalidConfig("parameters object may not be None")
        self.trace = False
        self.dryrun = p.get_arg_or_none(wc_args.DRYRUN)
        self.logfilehandler = None
        self.logfilepath = None
        self.log = self._configure_logging()
        
    def resolve_var_dir(self, name):
        """Return absolute path to the needed var directory
        name -- relative path to directory
        
        Does not check if path is valid/exists.
        """
        
        # If wcdirs values are relative paths, they are taken from the base
        # directory.  If the program is 'installed' the values should not be
        # relative (person writing install code needs to understand that).
        
        vardir = self.p.get_conf_or_none("wcdirs", "var")
        if not vardir:
            raise InvalidConfig("There is no wcdirs->var configuration.  This is required.")
            
        if not os.path.isabs(vardir):
            basedir = self._get_basedir()
            vardir = os.path.join(basedir, vardir)
            
        return os.path.join(vardir, name)
        
    def resolve_libexec_dir(self, name):
        """Return absolute path to the needed libexec file
        name -- relative path to file
        
        Does not check if path is valid/exists.
        """
        
        # If wcdirs values are relative paths, they are taken from the base
        # directory.  If the program is 'installed' the values should not be
        # relative!
        
        libexecdir = self.p.get_conf_or_none("wcdirs", "libexec")
        if not libexecdir:
            raise InvalidConfig("There is no wcdirs->libexec configuration.  This is required.")
            
        if not os.path.isabs(libexecdir):
            basedir = self._get_basedir()
            libexecdir = os.path.join(basedir, libexecdir)
            
        return os.path.join(libexecdir, name)
        
    def get_class_by_keyword(self, keyword):
        """Use the default 'dependency injection' mechanism.  This system is
        not a requirement to use to create objects, all that is needed is
        interface compliance (see internal.conf).
        
        As the 'ICommon' implementation is itself typically instantiated by
        the same mechanism, there is some bootstrapping that needs to occur
        at the beginning of the program.  Modules should assume this has occured
        already (it would not be sane/legal to provide a broken common instance
        to the module, every module gets a common instance via __init__).
        """
        
        # NOTE:
        # This needs to agree with workspacecontrol.main/__init__.py
        # i.e., the DI code that bootstraps Common should be bootstrapping
        # from the same configurations as we do here.
        implstr = self.p.get_conf_or_none("wcimpls", keyword)
        if not implstr:
            raise UnexpectedError("could not locate implementation class for keyword '%s'" % keyword)
        return main_gcbk(keyword, implstr=implstr)
        
# -----------------------------------------------------------------------------
        
    def close_logfile(self):
        if not self.logfilepath:
            return
        if not self.logfilehandler:
            return
        self.logfilehandler.close()
        self.logfilehandler = None
        
    def reopen_logfile(self):
        if not self.logfilepath:
            return
        return self._configure_logging_common(self.log)
        
# -----------------------------------------------------------------------------
        
    def _jump_up_dir(self, path):
        return "/".join(os.path.dirname(path+"/").split("/")[:-1])
        
    def _get_basedir(self):
        """Return value of base directory.  Not very useful under 'installation'
        situations.
        """
        current = None
        # respect environment variable first
        try:
            current = os.environ["WORKSPACE_CONTROL_BASEDIR"]
        except KeyError:
            pass
        
        if current and os.path.isabs(current):
            return current.strip()
        elif current:
            raise InvalidConfig("The WORKSPACE_CONTROL_BASEDIR environment variable was defined but did not contain an absolute path.")
        
        # figure it out programmatically from location of this source file
        current = os.path.abspath(__file__)
        while True:
            current = self._jump_up_dir(current)
            if os.path.basename(current) == "src":
                # jump up one more time
                return self._jump_up_dir(current)
            if not os.path.basename(current):
                raise IncompatibleEnvironment("cannot find base directory")
                
    def _configure_logging(self):
        log = logging.getLogger("wc")
        log.setLevel(logging.DEBUG)
        
        stdoutloglevel = self.p.get_conf_or_none("logging", "stdoutloglevel")
        try:
            n = int(stdoutloglevel)
            if n < 0 or n > 4:
                raise InvalidConfig("Stdout log level expected to be 0-4")
        except:
            raise InvalidConfig("Stdout log level expected to be an integer")
        
        # console logger
        ch = logging.StreamHandler()
        ch.setLevel(_logleveldict[n])
        if n > 1:
            formatter = logging.Formatter("%(asctime)s - %(levelname)s - %(message)s")
        else:
            formatter = logging.Formatter("%(asctime)s - %(levelname)s - %(module)s:%(lineno)d - %(message)s")
        ch.setFormatter(formatter)
        log.addHandler(ch)
        
        if n == 0:
            self.trace = True
            log.debug("trace enabled for stdout")
        
        # file logger
        logfiledir = self.p.get_conf_or_none("logging", "logfiledir")
        
        if not logfiledir:
            log.debug("no logfiledir configuration, file logging is disabled")
            return log
        
        if not os.path.isabs(logfiledir):
            logfiledir = self.resolve_var_dir(logfiledir)
            
        # base filename on time, action and name
        self.logfilepath = logfiledir + "/" + time.strftime("wclog-%Y-%m-%d")
        name = self.p.get_arg_or_none(wc_args.NAME)
        if name:
            self.logfilepath += "--" + str(name)
        action = self.p.get_arg_or_none(wc_args.ACTION)
        if action:
            self.logfilepath += "-" + str(action)
            
        self.logfilepath += time.strftime("--%H.%M.%S")
            
        f = None
        try:
            if os.path.exists(self.logfilepath):
                time.sleep(0.1)
                self.logfilepath += "-" + str(time.time())
            if os.path.exists(self.logfilepath):
                raise ProgrammingError("sleep() or time() not working?")
                
            f = file(self.logfilepath, 'w')
            f.write("\n## auto-generated @ %s\n\n" % time.ctime())
        finally:
            if f:
                f.close()
                
        return self._configure_logging_common(log)
                
    def _configure_logging_common(self, log):
    
        fileloglevel = self.p.get_conf_or_none("logging", "fileloglevel")
        try:
            n = int(fileloglevel)
            if n < 0 or n > 4:
                raise InvalidConfig("File log level expected to be 0-4")
        except:
            raise InvalidConfig("File log level expected to be an integer")
                
        # r.e. "obscure side note" in logging.conf.  Trace is set only once here
        # and used in a "if trace: log.debug()" fashion.  So if both stdout
        # and file logging have DEBUG level on, but only one of them was set
        # to trace level, they will effectively both get trace level.
        # The levels in logging module are not flexible enough.
        if n == 0:
            self.trace = True
                
        self.logfilehandler = logging.FileHandler(self.logfilepath)
        self.logfilehandler.setLevel(_logleveldict[n])
        if n > 1:
            formatter = logging.Formatter("%(asctime)s - %(levelname)s - %(message)s")
        else:
            formatter = logging.Formatter("%(asctime)s - %(module)s:%(lineno)d - %(levelname)s - %(message)s")
        self.logfilehandler.setFormatter(formatter)
        log.addHandler(self.logfilehandler)
        log.debug("file logging enabled, path = '%s'" % self.logfilepath)
        if n == 0:
            log.debug("trace enabled for fileloglevel")
            
        return log
