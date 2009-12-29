from commands import getstatusoutput
import os
import string
import zope.interface

import workspacecontrol.api.modules
from workspacecontrol.api.exceptions import *
import workspacecontrol.main.wc_args as wc_args

class DefaultAsyncNotification:
    """AsyncNotification is the wcmodule responsible for notifying the service
    of events (both successes and failures of various tasks).
    """
    
    zope.interface.implements(workspacecontrol.api.modules.IAsyncNotification)
    
    def __init__(self, params, common):
        self.p = params
        self.c = common
        self.ssh = None
        self.sshargs = None
        self.notifypath = None
        
    def validate(self):
        
        self.ssh = self.p.get_conf_or_none("notifications", "ssh")
        if not self.ssh:
            raise InvalidConfig("no notifications->ssh configuration")
            
        if os.path.isabs(self.ssh):
            if not os.access(self.ssh, os.F_OK):
                raise InvalidConfig("SSH is configured with an absolute path, but it does not seem to exist: '%s'" % self.ssh)
                
            if not os.access(self.ssh, os.X_OK):
                raise InvalidConfig("SSH is configured with an absolute path, but it does not seem executable: '%s'" % self.ssh)

        self.c.log.debug("SSH configured: %s" % self.ssh)
        
        notifyspec = self.p.get_arg_or_none(wc_args.NOTIFY)
        
        if not notifyspec:
            return
        
        # Notify argument needs to take the form username@hostname:port/path
        # (where hostname is a fqdn).  todo: more validation could happen
        if string.find(notifyspec, ':') == -1:
            raise InvalidInput("no port configured for notify")
        if string.find(notifyspec, '/') == -1:
            raise InvalidInput("no path configured for notify")
            
        i1 = string.find(notifyspec, ':')
        userhost = notifyspec[:i1]
        i2 = string.find(notifyspec, '/')
        port = int(notifyspec[i1+1:i2])
        self.notifypath = notifyspec[i2:]
        self.c.log.debug("notifypath: %s" % self.notifypath)
        self.sshargs = '-p ' + str(port) + ' ' + userhost
        self.c.log.debug("sshargs: %s" % self.sshargs)
        
    
    def notify(self, name, actiondone, code, error):
        """
        name -- handle of the VM this is about
        
        actiondone -- event name
        
        code -- status code ('exit' code essentially)
        
        error -- error text for nonzero status codes
        """
        
        if not self.notifypath:
            raise UnexpectedError("cannot run notification without notify argument")
        
        errtxt = ""
        if code:
            if actiondone in ["propagate", "unpropagate"]:
                errtxt = "TRANSFER FAILED "
                if error:
                    # remove newlines, replace with another token
                    lines = error.splitlines()
                    a = lambda x: x + " ]eol[ "
                    errtxt += ''.join(map(a, lines))
                    errtxt = "'" + errtxt + "'"
                else:
                    errtxt += "No error output is available"
                
            elif actiondone == "start":
                errtxt = "CREATE FAILED "
                if error:
                    # remove newlines, replace with another token
                    lines = error.splitlines()
                    a = lambda x: x + " ]eol[ "
                    errtxt += ''.join(map(a, lines))
                    errtxt = "'" + errtxt + "'"
                else:
                    errtxt += "No error output is available"
                    
            else:
                raise ProgrammingError("unknown actiondone for notification")
        
            errtxt = self._bashEscape(errtxt)


        exeargs = [self.notifypath, 'write', name, actiondone, str(code), errtxt]
        
        cmd = self.ssh + " " + self.sshargs + " " + ' '.join(exeargs)

        self.c.log.debug("running notification command '%s'" % cmd)
        if self.c.dryrun:
            self.c.log.debug("just kidding, dryrun")
            return
        
        (notif_errcode, notif_output) = getstatusoutput(cmd)
        self.c.log.info("notification command exit code = %d" % notif_errcode)
        if notif_errcode:
            err = "notification command error output:\n%s\n" % notif_output
            self.c.log.error(err)
            raise UnexpectedError(err)
            
    def _bashEscape(self, cmd):
        """returns \ escapes for some bash special characters"""
        if not cmd:
            return cmd
        escs = "\\'`|;()?#$^&*="
        for e in escs:
            idx = 0
            ret = 0
            while ret != -1:
                ret = cmd.find(e, idx)
                if ret >= 0:
                    cmd = "%s\%s" % (cmd[:ret],cmd[ret:])
                    idx = ret + 2
        return cmd
        