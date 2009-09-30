#!/bin/sh

NIMBUS_PRINTNAME="delete gateway persistence directory"
NIMBUS_EXTRAPRINT="     [ ** ] Note there are persistence mgmt scripts @ $GLOBUS_LOCATION/share/nimbus-gateway"
NIMBUS_ANT_CMD="delete-GT4.0-gateway-persistence $*"

BASEDIR_REL="`dirname $0`/.."
BASEDIR=`cd $BASEDIR_REL; pwd`
RUN=$BASEDIR/bin/lib/gt4.0/build/run.sh

export NIMBUS_PRINTNAME NIMBUS_ANT_CMD NIMBUS_EXTRAPRINT
exec sh $RUN
