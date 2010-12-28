#!/bin/bash

set -e

if [ "X$PYTHON" != "X" ]; then
    
    if [ ! -e $PYTHON ]; then
        echo "The path specified in the PYTHON environment variable does not exist:"
        echo "    $PYTHON"
        exit 1
    fi
    
    echo "Using the Python executable provided in the PYTHON environment variable:"
    echo "    $PYTHON"
    echo "Make sure to provide this environment to the installer as well."

else
    PYTHON=`which python`
fi

NH_REL="`dirname $0`/.."
NH=`cd $NH_REL; pwd`
PY_SCRIPT="$NH/scripts/lib/check_dependencies.py"

exec $PYTHON "$PY_SCRIPT" "$@"
