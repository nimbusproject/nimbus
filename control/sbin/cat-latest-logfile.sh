#!/bin/bash

PYTHON_EXE="/usr/bin/env python"
NIMBUS_CONTROL_DIR_REL="`dirname $0`/.."
NIMBUS_CONTROL_DIR=`cd $NIMBUS_CONTROL_DIR_REL; pwd`
NIMBUS_CONTROL_MAINCONF="$NIMBUS_CONTROL_DIR/etc/workspace-control/main.conf"

# -----------------------------------------------------------------------------

HELP_MESSAGE="This program (with no arguments) prints the newest file in the log directory, this is helpful for debugging and development.\n\nIt loads the main.conf file and resolves the log file directory just as the workspace control program will.\n\nThe main.conf in use is: $NIMBUS_CONTROL_MAINCONF"
if [ "X$1" == "X-h" ] || [ "X$1" == "X-help" ] || [ "X$1" == "X--help" ] || [ "X$1" == "Xhelp" ]; then
    echo -e "$HELP_MESSAGE"
    exit 1
fi
    
# -----------------------------------------------------------------------------
    
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

$PYTHON_EXE $NIMBUS_CONTROL_PYSRC/workspacecontrol/sbin/most-recent-log.py $NIMBUS_CONTROL_MAINCONF
