#!/bin/bash

# Allow this to run from either the source tree or a NIMBUS_HOME deployment
NIMBUS_HOMEDIR_REL="`dirname $0`/../../.."
NIMBUS_HOMEDIR=`cd $NIMBUS_HOMEDIR_REL; pwd`
if [ -d $NIMBUS_HOMEDIR/ctx-broker/home/lib/pynimbusconfig ]; then
    NIMBUS_PYLIB=$NIMBUS_HOMEDIR/ctx-broker/home/lib
else
    NIMBUS_HOMEDIR_REL="`dirname $0`/.."
    NIMBUS_HOMEDIR=`cd $NIMBUS_HOMEDIR_REL; pwd`
    if [ -d $NIMBUS_HOMEDIR/lib/pynimbusconfig ]; then
        NIMBUS_PYLIB=$NIMBUS_HOMEDIR/lib
    else
        echo "Cannot locate Python lib directory"
        exit 1
    fi
fi

PYTHONPATH="$NIMBUS_PYLIB:$PYTHONPATH"
export PYTHONPATH

nosetests --nologcapture -x -v -w $NIMBUS_PYLIB/pynimbusconfig
