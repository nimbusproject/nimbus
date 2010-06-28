#!/bin/bash

PYTHON_EXE="/usr/bin/env python"
NIMBUS_CONTROL_DIR_REL="`dirname $0`/.."
NIMBUS_CONTROL_DIR=`cd $NIMBUS_CONTROL_DIR_REL; pwd`
    
WORKSPACE_CONTROL="$NIMBUS_CONTROL_DIR/bin/workspace-control.sh"

CMD="$WORKSPACE_CONTROL --action remove --name control-test"

echo -e "\n=======================================================\n"
echo -e "Running this command:\n"
echo $CMD
echo -e "\n=======================================================\n"

exec $CMD

