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

# this next line is added to test github issue 44
touch $NIMBUS_HOME/services/etc/nimbus/workspace-service/group-authz/group03.properties.XXX
nosetests user_tests.py user_failures_tests.py "$@"
exit $?
