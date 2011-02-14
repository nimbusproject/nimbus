#!/bin/bash

export TERM=dumb
PYTHON_EXE="/usr/bin/env python"

NIMBUS_SRC_REL="`dirname $0`/.."
NIMBUS_SRC=`cd $NIMBUS_SRC_REL; pwd`

if [ "X$1" = "X" ]; then
    echo ""
    echo "Usage: $0 destination_dir"
    echo "    You must specify the destination directory."
    echo ""
    exit 1
fi

NIMBUS_HOME=$1

chmod 755 $NIMBUS_HOME/libexec/*sh
if [ $? -ne 0 ]; then
    echo "Could not chmod $NIMBUS_HOME/libexec files"
    exit 1
fi

CONFIG_SCRIPT="$NIMBUS_HOME/bin/nimbus-configure"


CUMULUS_REPO_BUCKET=`$CONFIG_SCRIPT --print-repobucket`
if [ $? -ne 0 ]; then
    echo "Could not determine Cumulus repository bucket."
    exit 1
fi

CUMULUS_HOST=`$CONFIG_SCRIPT --print-hostname`
if [ $? -ne 0 ]; then
    echo "Could not determine chosen hostname."
    exit 1
fi

echo ""
echo "Creating Cumulus repository with the following commands:"
echo ""

export CUMULUS_SETTINGS_FILE=$NIMBUS_HOME/cumulus/etc/cumulus.ini
CUMULUS_ENV="$NIMBUS_HOME/cumulus/env.sh"
if [ ! -f $CUMULUS_ENV ]; then
    echo "Expected file to be created: $CUMULUS_ENV"
    exit 1
fi

source $CUMULUS_ENV
REPOCMD="$NIMBUS_HOME/ve/bin/cumulus-create-repo-admin $CUMULUS_REPO_BUCKET"

echo "    $REPOCMD"
echo ""

$REPOCMD >/dev/null
if [ $? -ne 0 ]; then
    echo "Could not create Cumulus repository."
    exit 1
fi
echo "Created repo admin."

exit 0
