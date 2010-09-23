#!/bin/bash

BASEDIR_REL="`dirname $0`/.."
BASEDIR=`cd $BASEDIR_REL; pwd`

EMBEDDED_GL="$BASEDIR/lib/globus"
USER_PROPFILE="$BASEDIR/conf/cloud.properties"
CLOUD_DIR="$BASEDIR/conf/clouds"
HISTORY_DIR="$BASEDIR/history"
EMBEDDED_CADIR="$BASEDIR/lib/certs"

X509_CERT_DIR="$EMBEDDED_CADIR"
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

INCLUDED_COMMANDLINE_STRING="--conf $USER_PROPFILE --cloud-dir $CLOUD_DIR --history-dir $HISTORY_DIR"

####### JAVA CHECK ##########

if [ "X$JAVA_HOME" = "X" ] ; then
  _RUNJAVA=java
 else
  _RUNJAVA="$JAVA_HOME"/bin/java
fi

COMMONJAR="$GLOBUS_LOCATION/lib/nimbus-messaging-common-gt4.0.jar"
if [ -f $COMMONJAR ]; then
  $_RUNJAVA -classpath $COMMONJAR org.nimbustools.messaging.gt4_0.common.OKJVM
  if [ $? -eq 2 ]; then
    echo -e "\n\n" >&2
    echo -e "WARNING: You seem to be using libgcj which has known issues." >&2
    echo -e "         See https://bugzilla.mcs.anl.gov/globus/show_bug.cgi?id=6871\n\n" >&2
  fi
fi

####### Generated globus client sh script follows.  It is slightly modified (see "NOTE").

DELIM="#"
EXEC="org.globus.bootstrap.Bootstrap org.globus.workspace.cloud.meta.client.CloudMetaClient"

DEF_OPTIONS=""
DEF_CMD_OPTIONS="$INCLUDED_COMMANDLINE_STRING"
EGD_DEVICE="/dev/urandom"

updateOptions() {

  if [ "X$2" != "X" ] ; then
    GLOBUS_OPTIONS="$GLOBUS_OPTIONS -D$1=$2"
  fi

}

####### MAIN BODY ##########

if [ ! -d "$GLOBUS_LOCATION" ] ; then
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

# NOTE: we've switched the normal position of DEF_CMD_OPTIONS.  This allows
#       cloud-client.sh user to pass in commandline flags that override flags
#       this script is passing: parser consumes the FIRST appearance of a flag.

if [ $# -gt 0 ]; then
  if [ "X${DEF_CMD_OPTIONS}" != "X" ]; then
    set - ${GLOBUS_OPTIONS} -classpath ${LOCALCLASSPATH} ${EXEC} "$@" ${DEF_CMD_OPTIONS} 
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

