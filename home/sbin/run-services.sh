#!/bin/sh

NIMBUS_HOME_REL="`dirname $0`/.."
NIMBUS_HOME=`cd $NIMBUS_HOME_REL; pwd`

LOGFILE="$NIMBUS_HOME/var/services.log"

$NIMBUS_HOME/services/bin/globus-start-container.sh > $LOGFILE 2>&1
