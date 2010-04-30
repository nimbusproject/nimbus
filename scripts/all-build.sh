#!/bin/bash

NIMBUS_PRINTNAME="build all"
NIMBUS_ANT_CMD="build-default-GT4.0-service $*"

BASEDIR_REL="`dirname $0`/.."
BASEDIR=`cd $BASEDIR_REL; pwd`
RUN=$BASEDIR/scripts/lib/gt4.0/build/run.sh

export NIMBUS_PRINTNAME NIMBUS_ANT_CMD
exec sh $RUN
