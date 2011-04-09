#!/bin/bash

BASEDIR_REL="`dirname $0`/.."
BASEDIR=`cd $BASEDIR_REL; pwd`
BASEDIR=${BASEDIR/ /\\ }

EMBEDDED_GL="$BASEDIR/lib/globus"
USER_PROPFILE="$BASEDIR/conf/cloud.properties"
HISTORY_DIR="$BASEDIR/history"
EMBEDDED_CADIR="$BASEDIR/lib/certs"

if [ -n "$NIMBUS_X509_TRUSTED_CERTS" ]; then
  X509_CERT_DIR="$NIMBUS_X509_TRUSTED_CERTS"
else
  X509_CERT_DIR="$EMBEDDED_CADIR"
fi
export X509_CERT_DIR

OLD_GLOBUS_LOCATION=""
if [ -n "$GLOBUS_LOCATION" ]; then
  OLD_GLOBUS_LOCATION="$GLOBUS_LOCATION"
fi

GLOBUS_LOCATION=$EMBEDDED_GL
export GLOBUS_LOCATION

if [ -n "$OLD_GLOBUS_LOCATION" ]; then
  if [ "$OLD_GLOBUS_LOCATION" != "$GLOBUS_LOCATION" ]; then
      echo "(Overriding old GLOBUS_LOCATION '$OLD_GLOBUS_LOCATION')"
      echo -e "(New GLOBUS_LOCATION: '$GLOBUS_LOCATION')"
  fi
fi

needsconf="y"
needshist="y"
for i in "$@"; do
  if [ "--conf" == "$i" ]; then
    needsconf="n"
  fi
  if [ "--history-dir" == "$i" ]; then
    needshist="n"
  fi
done

INCLUDED_COMMANDLINE_STRING=""
if [ "X$needsconf" == "Xy" ]; then
  INCLUDED_COMMANDLINE_STRING="$INCLUDED_COMMANDLINE_STRING --conf $USER_PROPFILE"
fi
if [ "X$needshist" == "Xy" ]; then
  INCLUDED_COMMANDLINE_STRING="$INCLUDED_COMMANDLINE_STRING --history-dir $HISTORY_DIR"
fi

####### JAVA CHECK ##########

if [ "X$JAVA_HOME" = "X" ] ; then
  _RUNJAVA=java
 else
  _RUNJAVA="$JAVA_HOME"/bin/java
fi

####### Generated globus client sh script follows.

DELIM="#"
EXEC="org.globus.bootstrap.Bootstrap org.globus.workspace.cloud.client.CloudClient"

DEF_OPTIONS=""
DEF_CMD_OPTIONS="$INCLUDED_COMMANDLINE_STRING"
EGD_DEVICE="/dev/urandom"

updateOptions() {

  if [ "X$2" != "X" ] ; then
    GLOBUS_OPTIONS="$GLOBUS_OPTIONS -D$1=$2"
  fi

}

####### MAIN BODY ##########

if [ ! -d $GLOBUS_LOCATION ] ; then
  echo "Error: GLOBUS_LOCATION invalid or not set: $GLOBUS_LOCATION" 1>&2
  exit 1
fi

LOCALCLASSPATH=$GLOBUS_LOCATION/lib/bootstrap.jar:$GLOBUS_LOCATION/lib/cog-url.jar:$GLOBUS_LOCATION/lib/axis-url.jar

### SETUP OTHER VARIABLES ####

updateOptions "GLOBUS_LOCATION" "$GLOBUS_LOCATION"
updateOptions "java.endorsed.dirs" "$GLOBUS_LOCATION/endorsed"
updateOptions "X509_USER_PROXY" "$X509_USER_PROXY"
updateOptions "X509_CERT_DIR" "$X509_CERT_DIR"
updateOptions "GLOBUS_HOSTNAME" "$GLOBUS_HOSTNAME"
updateOptions "GLOBUS_TCP_PORT_RANGE" "$GLOBUS_TCP_PORT_RANGE"
updateOptions "GLOBUS_TCP_SOURCE_PORT_RANGE" "$GLOBUS_TCP_SOURCE_PORT_RANGE"
updateOptions "GLOBUS_UDP_SOURCE_PORT_RANGE" "$GLOBUS_UDP_SOURCE_PORT_RANGE"

if [ -c "$EGD_DEVICE" -a  -r "$EGD_DEVICE" ]; then
    updateOptions "java.security.egd" "file://$EGD_DEVICE"
fi

if [ "X$IBM_JAVA_OPTIONS" = "X" ] ; then
  IBM_JAVA_OPTIONS=-Xquickstart
  export IBM_JAVA_OPTIONS
fi

if [ $# -gt 0 ]; then
  if [ "X${DEF_CMD_OPTIONS}" != "X" ]; then
    set - ${GLOBUS_OPTIONS} -classpath ${LOCALCLASSPATH} ${EXEC} ${DEF_CMD_OPTIONS} "$@"
  else
    set - ${GLOBUS_OPTIONS} -classpath ${LOCALCLASSPATH} ${EXEC} "$@"
  fi
else
  if [ "X${DEF_CMD_OPTIONS}" != "X" ]; then
    set - ${GLOBUS_OPTIONS} -classpath ${LOCALCLASSPATH} ${EXEC}  ${DEF_CMD_OPTIONS}
  else
    set - ${GLOBUS_OPTIONS} -classpath ${LOCALCLASSPATH} ${EXEC}
  fi
fi

OLD_IFS=${IFS}
IFS=${DELIM}
for i in ${DEF_OPTIONS} ; do
  IFS=${OLD_IFS}
  DEFINE=`echo $i|cut -d'=' -f1`
  if [ "$DEFINE" != "$i" ]; then
    VALUE="`echo $i|cut -d'=' -f2-`"
    set - $DEFINE="$VALUE" "$@"
  else
    set - $DEFINE "$@"
  fi
  IFS=${DELIM}
done
IFS=${OLD_IFS}

### EXECUTE ############

exec $_RUNJAVA "$@"

