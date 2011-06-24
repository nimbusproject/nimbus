#!/bin/bash

NIMBUS_PRINTNAME="Build and install Context Broker"
NIMBUS_ANT_CMD="deploy-broker -Dbuild.also=x $*"

BASEDIR_REL="`dirname $0`/../.."
BASEDIR=`cd $BASEDIR_REL; pwd`
RUN=$BASEDIR/scripts/lib/gt4.0/build/run.sh

export NIMBUS_PRINTNAME NIMBUS_ANT_CMD
exec sh $RUN
