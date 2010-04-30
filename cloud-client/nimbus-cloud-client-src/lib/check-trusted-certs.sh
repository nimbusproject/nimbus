#!/bin/bash

BASEDIR_REL="`dirname $0`/.."
BASEDIR=`cd $BASEDIR_REL; pwd`
EMBEDDED_GL="$BASEDIR/lib/globus"
EMBEDDED_CADIR="$BASEDIR/lib/certs"

if [ "X$X509_CERT_DIR" = "X" ]; then

  echo "It doesn't look like you have the X509_CERT_DIR environment variable"
  echo "in your environment.  Run this:"
  echo ""
  echo "   export X509_CERT_DIR=$EMBEDDED_CADIR"
  
elif [ "X$X509_CERT_DIR" != "X$EMBEDDED_CADIR" ]; then
  
  echo "It looks like you have the X509_CERT_DIR environment variable in your environment but it's not set to the embedded cert directory."
  echo ""
  echo "This may or may not be what you want.  Potentially run:"
  echo ""
  echo "   export X509_CERT_DIR=$EMBEDDED_CADIR"
  
else 

  echo "Your X509_CERT_DIR environment variable has the embedded certificates directory defined. That's probably what you'd like to be the case."

fi

