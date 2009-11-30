#!/bin/bash

PYTHON_EXE="/usr/bin/env python"

NIMBUS_CONTROL_DIR_REL="`dirname $0`/.."
NIMBUS_CONTROL_DIR=`cd $NIMBUS_CONTROL_DIR_REL; pwd`

NIMBUS_CONTROL_MAINCONF="$NIMBUS_CONTROL_DIR/etc/workspace-control/main.conf"

if [ ! -f "$NIMBUS_CONTROL_MAINCONF" ]; then
    echo ""
    echo "Cannot find main conf file, exiting. (expected at '$NIMBUS_CONTROL_MAINCONF')"
    exit 1
fi

NIMBUS_CONTROL_PYLIB="$NIMBUS_CONTROL_DIR/lib/python"
NIMBUS_CONTROL_PYSRC="$NIMBUS_CONTROL_DIR/src/python"
PYTHONPATH="$NIMBUS_CONTROL_PYSRC:$NIMBUS_CONTROL_PYLIB:$PYTHONPATH"
export PYTHONPATH

# -----------------------------------------------------------------------------

$PYTHON_EXE $NIMBUS_CONTROL_PYSRC/workspacecontrol/main/wc_cmdline.py -c $NIMBUS_CONTROL_MAINCONF $@
