# cd to this directory and source me

# only works from cloud-client/
#             and cloud-client/*/
#             and cloud-client/lib/globus/
#             and cloud-client/lib/globus/bin/

ADIR="`pwd`"

if [ -f "$ADIR/this-globus-environment.sh" ]; then
  EMBEDDED_GL=`cd $ADIR/globus; pwd`
elif [ -f "$ADIR/lib/this-globus-environment.sh" ]; then
  EMBEDDED_GL=`cd $ADIR/lib/globus; pwd`
elif [ -f "$ADIR/../this-globus-environment.sh" ]; then
  EMBEDDED_GL=`cd $ADIR/../globus; pwd`
elif [ -f "$ADIR/../lib/this-globus-environment.sh" ]; then
  EMBEDDED_GL=`cd $ADIR/../lib/globus; pwd`
elif [ -f "$ADIR/../../this-globus-environment.sh" ]; then
  EMBEDDED_GL=`cd $ADIR/../; pwd`
elif [ -f "$ADIR/../../lib/this-globus-environment.sh" ]; then
  EMBEDDED_GL=`cd $ADIR/../../lib/globus; pwd`
else
  echo "Cannot locate the globus installation, try sourcing from the \
directory this file is in."
  return 1
fi

OLD_GLOBUS_LOCATION=""
if [ -n "$GLOBUS_LOCATION" ]; then
  OLD_GLOBUS_LOCATION="$GLOBUS_LOCATION"
fi

GLOBUS_LOCATION=$EMBEDDED_GL
export GLOBUS_LOCATION

EMBEDDED_CADIR="$GLOBUS_LOCATION/../certs"
X509_CERT_DIR=`cd $EMBEDDED_CADIR; pwd`
export X509_CERT_DIR

if [ -n "$OLD_GLOBUS_LOCATION" ]; then
  if [ "$OLD_GLOBUS_LOCATION" != "$GLOBUS_LOCATION" ]; then
      echo "(Overriding old GLOBUS_LOCATION '$OLD_GLOBUS_LOCATION')"
  fi
fi

echo "Now using GLOBUS_LOCATION: $GLOBUS_LOCATION"

if [ -f "$GLOBUS_LOCATION/etc/globus-user-env.sh" ]; then
  source $GLOBUS_LOCATION/etc/globus-user-env.sh
  echo "Sourced this file: $GLOBUS_LOCATION/etc/globus-user-env.sh"
else
  # at least get bin on path
  PATH="$GLOBUS_LOCATION/bin:$PATH"
  export PATH
  echo "This directory is now at beginning of PATH: $GLOBUS_LOCATION/bin"
fi


