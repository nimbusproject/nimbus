#!/bin/sh

NIMBUS_PRINTNAME="Run Nimbus REST test server"
NIMBUS_ANT_CMD="run-rest-testserver $*"

BASEDIR_REL="`dirname $0`/.."
BASEDIR=`cd $BASEDIR_REL; pwd`
RUN=$BASEDIR/scripts/lib/gt4.0/build/run.sh

export NIMBUS_PRINTNAME NIMBUS_ANT_CMD
exec sh $RUN
