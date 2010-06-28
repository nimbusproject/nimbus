#!/bin/bash

export NIMBUS_HOME=/home/bresnaha/NIM
PYTHON_EXE="/usr/bin/env python -Wignore::DeprecationWarning"

NIMBUS_WEBDIR="$NIMBUS_HOME/web"

NIMBUS_PYLIB="$NIMBUS_WEBDIR/lib/python"
NIMBUS_PYSRC="$NIMBUS_WEBDIR/src/python"

source $NIMBUS_HOME/cumulus/env.sh
PYTHONPATH="${PYTHONPATH}:$NIMBUS_PYSRC:$NIMBUS_PYLIB:$PYTHONPATH:$NIMBUS_HOME/sbin:${PYTHONPATH}"
export PYTHONPATH

echo $PYTHONPATH


cd /home/bresnaha/Dev/Nimbus/nimbus/tests
${CUMULUS_VE_HOME}/bin/nosetests user_tests.py

