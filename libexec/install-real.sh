#!/bin/bash

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

REPOCMD1="$NIMBUS_HOME/bin/nimbusctl cumulus start"
REPOCMD2="$NIMBUS_HOME/ve/bin/cumulus-create-repo-admin nimbusadmin@${CUMULUS_HOST} $CUMULUS_REPO_BUCKET"
REPOCMD3="$NIMBUS_HOME/bin/nimbusctl cumulus stop"

echo "    $REPOCMD1"
echo "    $REPOCMD2"
echo "    $REPOCMD3"
echo ""

$REPOCMD1
if [ $? -ne 0 ]; then
    echo "Could not start Cumulus."
    exit 1
fi

$REPOCMD2 >/dev/null
if [ $? -ne 0 ]; then
    echo "Could not create Cumulus repository."
    exit 1
fi
echo "Created repo admin."

$REPOCMD3
if [ $? -ne 0 ]; then
    echo "Could not stop Cumulus? (continuing)"
fi

echo ""
echo ""
echo "-----------------------------------------------------------------"
echo " Nimbus installation succeeded!"
echo "-----------------------------------------------------------------"
echo ""
echo "Refer to the Zero To Cloud Guide for setup instructions:"
echo "    http://www.nimbusproject.org/docs/2.5/admin/z2c/"
echo ""
echo "Additional configuration may be necessary:"
echo ""
echo "    To run the auto-configuration program, launch:"
echo "        $NIMBUS_HOME/bin/nimbus-configure --autoconfig"
echo ""
echo "You can start/stop Nimbus services with the nimbusctl command. e.g:"
echo "    $NIMBUS_HOME/bin/nimbusctl start"
echo ""

exit 0
