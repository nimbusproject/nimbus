#!/bin/bash

NIMBUS_PRINTNAME="build and install all"
NIMBUS_ANT_CMD="deploy-default-GT4.0-service -Dbuild.also=x $*"

BASEDIR_REL="`dirname $0`/.."
BASEDIR=`cd $BASEDIR_REL; pwd`
RUN=$BASEDIR/scripts/lib/gt4.0/build/run.sh

export NIMBUS_PRINTNAME NIMBUS_ANT_CMD
exec sh $RUN
