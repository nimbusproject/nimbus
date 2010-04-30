#!/bin/bash

# This script is included in the binary client tarball to make client GAR
# file deployment one step.

if [ ! -d "$GLOBUS_LOCATION" ] ; then
  echo "Error: GLOBUS_LOCATION is invalid or unset: $GLOBUS_LOCATION" 1>&2
  exit 1
fi

if [ ! -d "$ANT_HOME" ] ; then
  echo "Error: ANT_HOME is invalid or unset: $ANT_HOME" 1>&2
  exit 1
fi

GARDIR=`dirname $0`

$GLOBUS_LOCATION/bin/globus-deploy-gar $GARDIR/nimbus-wsdl-gt4.0.gar
$GLOBUS_LOCATION/bin/globus-deploy-gar $GARDIR/nimbus-messaging-stubs-gt4.0.gar
$GLOBUS_LOCATION/bin/globus-deploy-gar $GARDIR/nimbus-clients.gar
