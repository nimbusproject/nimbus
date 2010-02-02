#!/bin/sh

NIMBUS_HOME_REL="`dirname $0`/.."
NIMBUS_HOME=`cd $NIMBUS_HOME_REL; pwd`

GLOBUS_LOCATION="$NIMBUS_HOME/services"
export GLOBUS_LOCATION

X509_CERT_DIR="$NIMBUS_HOME/var/ca/trusted-certs"
export X509_CERT_DIR

LOGFILE="$NIMBUS_HOME/var/services.log"

exec $NIMBUS_HOME/services/bin/globus-start-container > $LOGFILE 2>&1
