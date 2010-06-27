#!/bin/bash

PYTHON_EXE="/usr/bin/env python"
NIMBUS_CONTROL_DIR_REL="`dirname $0`/.."
NIMBUS_CONTROL_DIR=`cd $NIMBUS_CONTROL_DIR_REL; pwd`

WORKSPACE_CONTROL="$NIMBUS_CONTROL_DIR/bin/workspace-control.sh"
    
# -----------------------------------------------------------------------------

NIMBUS_CONTROL_PYLIB="$NIMBUS_CONTROL_DIR/lib/python"
NIMBUS_CONTROL_PYSRC="$NIMBUS_CONTROL_DIR/src/python"
PYTHONPATH="$NIMBUS_CONTROL_PYSRC:$NIMBUS_CONTROL_PYLIB:$PYTHONPATH"
export PYTHONPATH

# -----------------------------------------------------------------------------

export WCONTROL_ACTION="create"

WC_ARGS=`$PYTHON_EXE $NIMBUS_CONTROL_PYSRC/workspacecontrol/sbin/construct-control-args.py $* 2>/dev/null`
if [ $? -ne 0 ]; then
    # run again to get stdout (like help) to print correctly (...)
    $PYTHON_EXE $NIMBUS_CONTROL_PYSRC/workspacecontrol/sbin/construct-control-args.py $*
    exit 1
fi

CMD="$WORKSPACE_CONTROL $WC_ARGS"

echo -e "\n=======================================================\n"
echo -e "Running this command:\n"
echo $CMD
echo -e "\n=======================================================\n"

echo -e "\n\n"

exec $CMD
