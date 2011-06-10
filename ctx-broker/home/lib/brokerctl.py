#! /usr/bin/env python

import os
import sys

import ProcessManager
from ProcessManager import Process
import ConfigParser

USAGE_TEXT = """\

  nimbusctl [target] command

Omit the target to perform the command for all targets.

Targets:
%(targets)s\

Commands:
%(commands)s\
"""


NIMBUS_HOME = os.getenv("NIMBUS_HOME")

if not NIMBUS_HOME:
    sys.exit("The NIMBUS_HOME environment variable is not set!")

if not os.path.isdir(NIMBUS_HOME):
    sys.exit("$NIMBUS_HOME does not exist: "+ NIMBUS_HOME)


CONFIG_PATH = os.path.join(NIMBUS_HOME, 'nimbus-setup.conf')
_NO_CONFIG_ERROR = """
Could not find the Nimbus setup config file:
    %s
This file is created after successful completion of the nimbus-configure
program. You should try running nimbus-configure before using this program.
""" % CONFIG_PATH
config = ConfigParser.SafeConfigParser()
if not config.read(CONFIG_PATH):
    sys.exit(_NO_CONFIG_ERROR)
broker_enabled = config.getboolean('nimbussetup', 'broker.enabled')

if not (broker_enabled):
    sys.exit("Broker is not enabled. "+
            "See the '%s' config file to adjust this setting." % CONFIG_PATH)

try:
    services_wait = config.getint('nimbussetup', 'services.wait')
except ConfigParser.NoOptionError:
    services_wait = 10

NIMBUS_RUN_DIR = os.path.join(NIMBUS_HOME, 'var/run/')
if not os.path.isdir(NIMBUS_RUN_DIR):
    try:
        os.mkdir(NIMBUS_RUN_DIR)
    except:
        sys.exit("Failed to create run directory: %s" % NIMBUS_RUN_DIR)

ProcessManager.init(dataDir = NIMBUS_RUN_DIR)

if broker_enabled:
    NIMBUS_BROKER_EXE = os.path.join(NIMBUS_HOME, 'lib/run-broker.sh')
    if not os.path.exists(NIMBUS_BROKER_EXE):
        sys.exit("The broker executable does not exist: " + 
                NIMBUS_BROKER_EXE)
    ProcessManager.add( Process(
      name = "broker",
      desc = "Nimbus Context Broker",
      program = NIMBUS_BROKER_EXE,
      args = [],
      workingDir = NIMBUS_HOME,
      postStartDelay=services_wait
      ))


argv = sys.argv
if len(argv) == 2:
    argv = argv[:]
    argv.insert(1, 'all')

ProcessManager.main(argv=argv, usage=USAGE_TEXT)
