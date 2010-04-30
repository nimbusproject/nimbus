#!/bin/bash

NIMBUS_HOME_REL="`dirname $0`/.."
NIMBUS_HOME=`cd $NIMBUS_HOME_REL; pwd`

LOGFILE="$NIMBUS_HOME/var/web.log"

exec $NIMBUS_HOME/web/bin/run-standalone-ssl.sh > $LOGFILE 2>&1
