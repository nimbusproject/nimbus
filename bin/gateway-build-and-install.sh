#!/bin/sh

NIMBUS_PRINTNAME="build and install gateway"
NIMBUS_ANT_CMD="deploy-gateway -Dbuild.also=x $*"

BASEDIR_REL="`dirname $0`/.."
BASEDIR=`cd $BASEDIR_REL; pwd`
RUN=$BASEDIR/bin/lib/gt4.0/build/run.sh

export NIMBUS_PRINTNAME NIMBUS_ANT_CMD
exec sh $RUN
