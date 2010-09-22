Nimbus Build and Test
=====================

This document is for developers of Nimbus.  It describes how to use the 
automated build, configure, and test system.  This system is NOT intended
for users and is not supported.  It touches some very important files 
on your system and thus should be considered dangerous.

---------
Important
---------
This system may destroy your ~/.ssh ~/.globus and ~/.nimbus directories!
While it makes every effort to back them up and reestablish them, this is
developer code and it does touch these files!  It is best run under a 
new and clean user.  Please back all of these directories up before using
this system!

Testing
=======

simply run the program ./bt-nimbus.sh with no arguments.  This will 
do the following:
    1) Nimbus from the github master branch
    2) build and install
        -- Nimbus
        -- workspace control in propagate only mode
        -- cloud-client
    3) run all tests in the current directory
        -- any file that that matches '.*test.{sh,py} will be run
    4) clean up

Results
-------

All output is sent to the console.  Within the last few lines there will
be a summary line similar to: 

    4 parent tests passed (many more subtests were run)
    3 parent tests failed
         cc1-test.sh ec2-test.sh cc-list-test.py

Log files are also created.  The main build log can be found in the source
directory at 'bandt.log'.  Each test also creates a log file <test name>.log.


Adding New Tests
----------------

To add a new test simply create an executable bash or python script and
name it according to the convention <your test name>test.{py,sh}.  The 
script will be run with the following environment:

    NIMBUS_HOME=<Nimbus service installation>
    NIMBUS_TEST_USER=<a configured Nimbus user name>
    CLOUD_CLIENT_HOME=<location of the cloud client install>
    NIMBUS_WORKSPACE_CONTROL_HOME=<location of the workspace control install>

All tests must return 0 for success.  All other return codes are considered
failure.  Tests may log whatever they want to stdout/err.

Propagation Only
----------------

It should be noted that all tests are run in 'propagation only' mode.  This
means that no VMs are actually started.  However the service, storage,  
propagation code, and cloud client code is tested.  While this is not 
exhaustive, it does provided a good sanity check of the codebase.
