#!/bin/bash

if [ "X$NIMBUS_HOME" == "X" ]; then
    echo "NIMBUS_HOME must be set to run this test"
    exit 1
fi

PYTHON_EXE="/usr/bin/env python -Wignore::DeprecationWarning"

NIMBUS_WEBDIR="$NIMBUS_HOME/web"

NIMBUS_PYLIB="$NIMBUS_WEBDIR/lib/python"
NIMBUS_PYSRC="$NIMBUS_WEBDIR/src/python"

source $NIMBUS_HOME/cumulus/env.sh
PYTHONPATH="${PYTHONPATH}:$NIMBUS_PYSRC:$NIMBUS_PYLIB:$PYTHONPATH:$NIMBUS_HOME/sbin:$NIMBUS_HOME/libexec:${PYTHONPATH}"
export PYTHONPATH

DJANGO_SETTINGS_MODULE="nimbusweb.portal.settings"
export DJANGO_SETTINGS_MODULE

source $NIMBUS_HOME/ve/bin/activate

./cc-ec2-get-image-wrapt.py
exit $?
