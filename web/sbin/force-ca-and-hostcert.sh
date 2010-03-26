#!/bin/bash

PYTHON_EXE="/usr/bin/env python -Wignore::DeprecationWarning"

NIMBUS_WEBDIR_REL="`dirname $0`/.."
NIMBUS_WEBDIR=`cd $NIMBUS_WEBDIR_REL; pwd`

NIMBUS_PYLIB="$NIMBUS_WEBDIR/lib/python"
NIMBUS_PYSRC="$NIMBUS_WEBDIR/src/python"

PYTHONPATH="$NIMBUS_PYSRC:$NIMBUS_PYLIB:$PYTHONPATH"
export PYTHONPATH

NIMBUS_WEBCONF="$NIMBUS_WEBDIR/nimbusweb.conf"
if [ ! -f "$NIMBUS_WEBCONF" ]; then
    echo ""
    echo "Cannot find conf file, exiting. (expected @ '$NIMBUS_WEBCONF')"
    exit 1
fi

# The following script expands the tarballs in lib/ if necessary
$NIMBUS_WEBDIR/sbin/install-deps.sh $DEBUG
if [ $? -ne 0 ]; then
    echo ""
    echo "Dependencies are not set up properly, exiting."
    exit 3
fi

$PYTHON_EXE $NIMBUS_PYSRC/nimbusweb/setup/setup.py --forcessl --conf $NIMBUS_WEBCONF --basedir $NIMBUS_WEBDIR $@

exit $?

