#!/bin/bash

THISDIR="`dirname $0`"
LIBDIR="$THISDIR/../lib/"
if [ ! -f $LIBDIR/common-env.sh ]; then
  echo "Failure, cannot find environment definitions"
  exit 1
fi
source $LIBDIR/common-env.sh

SOURCEME="$THISDIR/source-me.sh"
if [ ! -f $SOURCEME ]; then
  echo "Could not find the 'source-me.sh' file, exiting"
  exit 1
fi

source $SOURCEME

if [ "X" = "X$X509_CERT_DIR" ]; then
  echo "Could not find the X509_CERT_DIR setting, exiting."
  echo "This should have been set up in 'source-me.sh' -- did you run setup-container.sh yet?"
  exit 1
fi

echo "Active certificates @ $X509_CERT_DIR"
echo "Starting container @ $GLOBUS_LOCATION"

$GLOBUS_LOCATION/bin/globus-start-container

