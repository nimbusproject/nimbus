# ----------------------------------------------------------------------------
# Copyright (c) 2006, Humanized, Inc.
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
#
#   * Redistributions of source code must retain the above copyright
#     notice, this list of conditions and the following disclaimer.
#
#   * Redistributions in binary form must reproduce the above
#     copyright notice, this list of conditions and the following
#     disclaimer in the documentation and/or other materials provided
#     with the distribution.
#
#   * Neither the name of Humanized, Inc. nor the names of its
#     contributors may be used to endorse or promote products derived
#     from this software without specific prior written permission.
# 
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
# FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
# COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
# BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
# LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
# ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
#
#   ProcessManager.py
#   Author: Atul Varma <atul@humanized.com>
#
#   Python Version - 2.4
#
# ----------------------------------------------------------------------------

"""
    A simple module for process management. Please see the file
    README.txt, included with this distribution, for more
    information. This file is also available at the following
    location:

      http://www.humanized.com/ProcessManager
"""

# ----------------------------------------------------------------------------
# TODO's
#
# * Document the public methods better.
#
# * Don't require ProcessManager to be run as root, but do raise
#   exceptions if the user tries to control a process that requires
#   changing the user ID and it can't be done.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Imports
# ----------------------------------------------------------------------------

import os
import sys
import time


# ----------------------------------------------------------------------------
# Public Names and Version Information
# ----------------------------------------------------------------------------

__all__ = [
    "Process",
    "init",
    "add",
    "rcScriptMain",
    "main"
    ]

__version__ = "0.0.4"


# ----------------------------------------------------------------------------
# Constants
# ----------------------------------------------------------------------------

# Amount of time we wait in seconds after starting a process to see if
# it's still alive.
POST_PROCESS_START_DELAY = 5

# Amount of time we wait in seconds after killing a process to see if
# it's dead.
POST_PROCESS_STOP_DELAY = 2

# A list of all valid commands, accessible from the command-line; they
# map directly to public instance methods of the Process class.
COMMANDS = {
    "stop" : "stop the target",
    "start" : "start the target",
    "restart" : "restart (stop, then start) the target",
    "status" : "show status of the target"
    }

# Usage string when running the module's main() function.
USAGE_TEXT = """\

  %(scriptName)s <target> <command> [options]

targets:
%(targets)s\
  all                  (this target applies the command to all
                       of the above targets)

commands:
%(commands)s\
"""

# Usage string when running the module's rcScriptMain() function.
RC_SCRIPT_USAGE_TEXT = """\

  %(scriptName)s <command> [options]

This script controls %(targetDesc)s.

commands:
%(commands)s\
"""

# ----------------------------------------------------------------------------
# Module Variables
# ----------------------------------------------------------------------------

# Directory where all intermediate data files are kept.
_dataDir = None

# Our process registry; keys are the name identifiers for processes,
# and the values are Process objects.
_processes = {}

# OptionParser object representing command-line options parser.
_parser = None

# object storing command-line options, created by an OptionParser
# object.
_options = None


# ----------------------------------------------------------------------------
# Process Class
# ----------------------------------------------------------------------------

class Process:
    """
    Encapsulates a process that can be stopped, started, and
    restarted.
    """
    
    def __init__( self,
                  name,
                  desc,
                  program,
                  args,
                  workingDir,
                  uid = None,
                  gid = None,
                  stopSignal = None,
                  postStartDelay = POST_PROCESS_START_DELAY):
        """
        Creates a process with the given name/identifier, description,
        program executable path, argument tuple, and working
        directory.  When it is run, it will run with the given user
        and group ID privileges.  When it is stopped, the given signal
        will be sent to tell it to do so.
        """

        if stopSignal == None:
            import signal
            stopSignal = signal.SIGKILL

        self.name = name
        self.desc = desc
        self.program = program
        self.args = [ program ]
        self.args.extend( args )
        self.workingDir = workingDir
        self.stopSignal = stopSignal
        self.postStartDelay = postStartDelay
        
        if gid: 
            import grp
            self.gid = grp.getgrnam( gid )[2]
        else:
            self.gid = None
        
        if uid:
            import pwd
            self.uid = pwd.getpwnam( uid )[2]
        else:
            self.uid = None

    def _pidfile( self ):
        """
        Returns the filename of the pid file for this process. A pid
        file just contains the pid of the process, if it's believed to
        be currently running.
        """
        
        return os.path.join( _dataDir, "%s.pid" % self.name )

    def _readpid( self ):
        """
        Opens the pid file for this process and gets the pid for
        it. If the pid file doesn't exist, this method returns None.
        """
        
        if not os.path.exists( self._pidfile() ):
            return None
        f = open( self._pidfile(), "r" )
        pid = int( f.read() )
        f.close()
        return pid

    def status( self ):
        """
        Public method that prints out what this process' status is
        (running, stopped, etc).
        """
        status = self._getStatus()
        print "%-30s%s" % ( self.desc, status )
        if status != 'running':
            raise ProcessStatusError()

    def _getStatus( self ):
        """
        Returns a single word indicating the status of this process.
        """
        
        pid = self._readpid()
        if pid == None:
            return "stopped"
        elif _isPidRunning( pid ):
            return "running"
        else:
            return "crashed"

    def start( self, warnCrashed = False ):
        """
        Public method that starts the process. If the process is
        already deemed to be running, nothing happens.

        If the process fails to launch, raise a
        ProcessStartupError exception.
        """
        
        pid = self._readpid()
        if pid != None:
            if _isPidRunning( pid ):
                print "Process '%s' is already running!" % self.name
                return
            elif warnCrashed:
                print "Process '%s' may have died prematurely." % self.name

        # Start the process now.
        leftColumnText = "Launching %s..." % self.desc
        print "%-30s" % leftColumnText,
        sys.stdout.flush()

        self._doStart()

    def _doStart( self ):
        """
        Protected implementation method that starts the actual
        process.
        """
        
        forkResult = os.fork()
        if forkResult == 0:
            # We're the child process.

            if self.gid:
                os.setgid( self.gid )
            
            if self.uid:
                os.setuid( self.uid )

            os.chdir( self.workingDir )

            nullFile = os.open( "/dev/null", os.O_RDWR )

            # Replace stdin.
            os.dup2( nullFile, 0 )

            # Replace stdout
            os.dup2( nullFile, 1 )

            # Replace stderr
            os.dup2( nullFile, 2 )

            os.close( nullFile )
            
            # Launch the program.
            os.execv( self.program, self.args )
        else:
            # We're the parent process.
            pid = forkResult
            f = open( self._pidfile(), "w" )
            f.write( "%d" % pid )
            f.close()

            if self.postStartDelay:
                time.sleep(self.postStartDelay)

            retVal = os.waitpid( pid, os.WNOHANG )
            if retVal == (0, 0):
                print "OK"
            else:
                print "FAILED"
                try:
                    os.remove(self._pidfile())
                except:
                    pass
                raise ProcessStartupError()

    def stop( self, warnCrashed = True ):
        """
        Public method that stops the process if it's currently
        running.
        """
        
        pid = self._readpid()
        if pid != None:
            if _isPidRunning( pid ):
                leftColumnText = "Stopping %s..." % self.desc
                print "%-30s" % leftColumnText,
                sys.stdout.flush()

                os.kill( pid, self.stopSignal )

                time.sleep( POST_PROCESS_STOP_DELAY )

                if not _isPidRunning( pid ):
                    print "OK"
                else:
                    print "FAILED"
            elif warnCrashed:
                print "Process '%s' may have died prematurely." % self.name
            os.remove( self._pidfile() )
        else:
            print "Process '%s' is not running." % self.name

    def restart( self ):
        """
        Public method that stops the process and then starts it again.
        """
        
        self.stop( warnCrashed = False )
        self.start()

class ProcessStartupError( Exception ):
    """
    Exception raised when a process fails to start.
    """
    
    pass

class ProcessStatusError( Exception ):
    """
    Exception raised when a process is not running.
    """
    pass

# ----------------------------------------------------------------------------
# Module Functions
# ----------------------------------------------------------------------------

def init( dataDir ):
    """
    Initializes the module.
    
    dataDir is the directory where all intermediate data files are
    stored (e.g., pidfiles).
    """

    global _dataDir
    
    _dataDir = dataDir

def _isPidRunning( pid ):
    """
    Returns whether or not a process with the given pid is running.
    """
    try:
        os.kill(pid, 0)
    except OSError:
        return False
    else:
        return True

def add( process ):
    """
    Adds the given Process object as a target for the registry of
    processes to manage.
    """
    
    if _processes.has_key( process.name ):
        raise TargetAlreadyExistsError()
    _processes[process.name] = process

class TargetAlreadyExistsError( Exception ):
    """
    Exception raised when a target is added to the ProcessManager
    whose name already exists.
    """

    pass

def _runCommandOnTarget( command, target ):
    """
    Runs the given command on the given target.
    """

    if _dataDir == None:
        print "Error! ProcessManager not initialized."
        print "Please use ProcessManager.init()."
        sys.exit( -1 )

    errorOccurred = False
    
    if target == "all":
        for process in _processes.values():
            method = getattr( process, command )
            try:
                method()
            except (ProcessStartupError, ProcessStatusError):
                errorOccurred = True
    else:
        method = getattr( _processes[target], command )
        try:
            method()
        except (ProcessStartupError, ProcessStatusError):
            errorOccurred = True

    if errorOccurred:
        sys.exit(1)

def _checkPrivileges():
    """
    Checks to ensure that the current user has the proper privileges
    to run the ProcessManager; exits the program if not.
    """

    needRoot = False
    for process in _processes.values():
        if process.gid or process.uid:
            needRoot = True
            break
    
    if not needRoot:
        return

    if os.getuid() != 0:
        print "ERROR: This script must be run as root."
        sys.exit( -1 )

def _generateTargetHelpText():
    """
    Returns a string containing a list of available targets with their
    descriptions.
    """
    
    targets = ""
    for key in _processes.keys():
        targets += "  %-21s%s\n" % ( key, _processes[key].desc )
    return targets

def _generateCommandHelpText():
    """
    Returns a string containing a list of available commands with a
    description of what they do.
    """
    
    commands = ""
    for command in COMMANDS.keys():
        commands += "  %-21s%s\n" % ( command, COMMANDS[command] )
    commands = commands[:-1]
    return commands

def rcScriptMain():
    """
    The main function of the rc-script use of the Process Manager,
    whereby the name of the script determines the target, and the
    first command-line parameter determines the command.
    """

    _checkPrivileges()

    target = os.path.split( sys.argv[0] )[1]
    if not _processes.has_key( target ):
        # If we're in a rc.d directory, we may have 3 characters
        # prepended to our name, such as "S01foo".  So let's try
        # stripping off the first 3 characters of our name and seeing
        # if that works as a target.
        if target[0] in ["K", "S"]:
            ordering = target[1:3]
            try:
                # See if these characters constitute a number.
                int( ordering )
                # If so, let's try reinterpreting our target.
                target = target[3:]
            except ValueError:
                pass

    if not _processes.has_key( target ):
        print "ERROR: Target '%s' does not exist!" % target
        print "Consider renaming this script to match one"
        print "of the following targets:"
        print
        print _generateTargetHelpText()
        sys.exit( -1 )

    usageTextDict = {
        "scriptName" : target,
        "targetDesc" : _processes[ target ].desc,
        "commands" : _generateCommandHelpText(),
        }

    usageText = RC_SCRIPT_USAGE_TEXT % usageTextDict

    _processCmdLineOptions( usageText )

    if len( sys.argv ) == 1:
        command = ""
    else:
        command = sys.argv[1]

    if not command in COMMANDS.keys():
        _parser.print_help()
        sys.exit( -1 )

    _runCommandOnTarget( command, target )

def _processCmdLineOptions( usageText, args=None ):
    """
    Parses and processes standard command-line options.
    """

    import optparse
    
    global _parser
    global _options
    global _args
    
    _parser = optparse.OptionParser( usage = usageText )

    ( _options, _args ) = _parser.parse_args(args=args)

def main(argv=sys.argv, usage=USAGE_TEXT):
    """
    The main function of the Process Manager which processes
    command-line arguments and acts on them.
    """

    _checkPrivileges()
    
    usageTextDict = {
        "scriptName" : os.path.split( argv[0] )[1],
        "targets" : _generateTargetHelpText(),
        "commands" : _generateCommandHelpText(),
        }

    usageText = usage % usageTextDict

    _processCmdLineOptions( usageText, args=argv[1:] )
    
    if len( _args ) < 2:
        _parser.print_help()
        sys.exit( -1 )

    target = _args[0]
    command = _args[1]

    if target not in _processes.keys() and target != "all":
        print "Invalid target: '%s'" % target
        sys.exit( -1 )
    if command not in COMMANDS.keys():
        print "Invalid command: '%s'" % command
        sys.exit( -1 )

    _runCommandOnTarget( command, target )
    
