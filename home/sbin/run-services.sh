#!/bin/bash

NIMBUS_HOME_REL="`dirname $0`/.."
NIMBUS_HOME=`cd $NIMBUS_HOME_REL; pwd`

GLOBUS_LOCATION="$NIMBUS_HOME/services"
export GLOBUS_LOCATION

X509_CERT_DIR="$NIMBUS_HOME/var/ca/trusted-certs"
export X509_CERT_DIR

LOGFILE="$NIMBUS_HOME/var/services.log"
PORT="8443"

exec $NIMBUS_HOME/services/bin/globus-start-container -p $PORT > $LOGFILE 2>&1
