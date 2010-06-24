#!/bin/bash

PYTHON_EXE="/usr/bin/env python -Wignore::DeprecationWarning"

NIMBUS_WEBDIR="$NIMBUS_HOME/web"

NIMBUS_PYLIB="$NIMBUS_WEBDIR/lib/python"
NIMBUS_PYSRC="$NIMBUS_WEBDIR/src/python"

PYTHONPATH="$NIMBUS_PYSRC:$NIMBUS_PYLIB:$PYTHONPATH:$NIMBUS_HOME/sbin"
export PYTHONPATH

source $NIMBUS_HOME/cumulus/env.sh

cd /home/bresnaha/Dev/Nimbus/nimbus/tests
nosetests user_tests.py

