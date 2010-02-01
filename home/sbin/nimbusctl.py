#! /usr/bin/env python

import os
import sys

import ProcessManager
from ProcessManager import Process

NIMBUS_HOME = os.getenv("NIMBUS_HOME")

if not NIMBUS_HOME:
    sys.exit("The NIMBUS_HOME environment variable is not set!")

if not os.path.isdir(NIMBUS_HOME):
    sys.exit("$NIMBUS_HOME does not exist: "+ NIMBUS_HOME)

NIMBUS_RUN_DIR = os.path.join(NIMBUS_HOME, 'var/run/')
if not os.path.isdir(NIMBUS_RUN_DIR):
    sys.exit("The run directory ($NIMBUS_HOME/var/run/) does not exist: "+
            NIMBUS_RUN_DIR)

NIMBUS_SERVICES_EXE = os.path.join(NIMBUS_HOME, 'sbin/run-services.sh')
if not os.path.exists(NIMBUS_SERVICES_EXE):
    sys.exit("The services executable does not exist: " + NIMBUS_SERVICES_EXE)

NIMBUS_WEB_EXE = os.path.join(NIMBUS_HOME, 'sbin/run-web.sh')
if not os.path.exists(NIMBUS_WEB_EXE):
    sys.exit("The web executable does not exist: " + NIMBUS_WEB_EXE)

ProcessManager.init(dataDir = NIMBUS_RUN_DIR)

ProcessManager.add( Process(
  name = "services",
  desc = "Nimbus IaaS Services",
  program = NIMBUS_SERVICES_EXE,
  args = [],
  workingDir = NIMBUS_HOME,
  postStartDelay=5
  ))

ProcessManager.add( Process(
  name = "web",
  desc = "Nimbus Web Application",
  program = NIMBUS_WEB_EXE,
  args = [],
  workingDir = NIMBUS_HOME,
  postStartDelay=3
  ))

ProcessManager.main()
