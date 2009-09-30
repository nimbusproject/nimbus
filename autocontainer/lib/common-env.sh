if [ ! "X$AUTOCOMMON_ENVIRONMENT_DEFINED" = "X" ]; then
  return 0
fi

# number of usercert-XXX.pem to try for
AUTO_CONTAINER_MAX_CERT_TRIES=100
export AUTO_CONTAINER_MAX_CERT_TRIES

CONTAINER_URL="http://www-unix.globus.org/ftppub/gt4/4.0/4.0.8/ws-core/bin/ws-core-4.0.8-bin.tar.gz"
CONTAINER_TARNAME="ws-core-4.0.8-bin.tar.gz"
CONTAINER_DIRNAME="ws-core-4.0.8"
TARGET_RELDIR="gt"
DOWNLOADS_RELDIR="downloads"
export CONTAINER_URL CONTAINER_TARNAME CONTAINER_DIRNAME TARGET_RELDIR

BASEDIR_REL="`dirname $0`/.."
BASEDIR=`cd $BASEDIR_REL; pwd`
TARBALL_DEST="$BASEDIR/$DOWNLOADS_RELDIR/$CONTAINER_TARNAME"
TARGETDIR=$BASEDIR/$TARGET_RELDIR
GLOBUS_LOCATION=$TARGETDIR/$CONTAINER_DIRNAME
export BASEDIR TARGETDIR GLOBUS_LOCATION TARBALL_DEST

CONF_SERVERCONFIG="$GLOBUS_LOCATION/etc/globus_wsrf_core/server-config.wsdd"
export CONF_SERVERCONFIG

CONF_SECDESC="$GLOBUS_LOCATION/etc/globus_wsrf_core/global_security_descriptor.xml"
export CONF_SECDESC

AUTOCOMMON_DIST_DIR=$BASEDIR/../autocommon/dist
export AUTOCOMMON_DIST_DIR

AUTOCOMMON_BUILD_FILE=$BASEDIR/../autocommon/build.xml
export AUTOCOMMON_BUILD_FILE

AUTOCONTAINER_GET_CONTAINER=$BASEDIR/lib/get-container.sh
export AUTOCONTAINER_GET_CONTAINER

AUTOCONTAINER_COUNTER_SECCONF=$BASEDIR/lib/new-counter-service-security-config.xml
export AUTOCONTAINER_COUNTER_SECCONF

AUTOGT_CLASSPATH_BASE1=$BASEDIR/lib/jars
AUTOGT_CLASSPATH_BASE2=$GLOBUS_LOCATION/lib

JAVA_OPTS="-classpath ."

if [ -d $AUTOGT_CLASSPATH_BASE1 ]; then
  for f in `ls $AUTOGT_CLASSPATH_BASE1`; do
    JAVA_OPTS="$JAVA_OPTS:$AUTOGT_CLASSPATH_BASE1/$f"
  done
fi

if [ -d $AUTOGT_CLASSPATH_BASE2 ]; then
  for f in `ls $AUTOGT_CLASSPATH_BASE2`; do
    JAVA_OPTS="$JAVA_OPTS:$AUTOGT_CLASSPATH_BASE2/$f"
  done
fi

export JAVA_OPTS

EXE_JVMCHECK="org.nimbustools.auto_common.JVMCheck"
export EXE_JVMCHECK

EXE_HOSTGUESS="org.nimbustools.auto_common.HostGuess"
export EXE_HOSTGUESS

EXE_LOGICAL_HOST="org.nimbustools.auto_common.confmgr.AddOrReplaceLogicalHost"
export EXE_LOGICAL_HOST

EXE_PUBLISH_HOST="org.nimbustools.auto_common.confmgr.AddOrReplacePublishHostname"
export EXE_PUBLISH_HOST

EXE_GLOBUS_SECDESC="org.nimbustools.auto_common.confmgr.AddOrReplaceGlobalSecDesc"
export EXE_GLOBUS_SECDESC

EXE_NEW_GRIDMAPFILE="org.nimbustools.auto_common.confmgr.ReplaceGridmap"
export EXE_NEW_GRIDMAPFILE

EXE_NEW_HOSTCERTFILE="org.nimbustools.auto_common.confmgr.ReplaceCertFile"
export EXE_NEW_HOSTCERTFILE

EXE_NEW_HOSTKEYFILE="org.nimbustools.auto_common.confmgr.ReplaceKeyFile"
export EXE_NEW_HOSTKEYFILE

EXE_CREATE_CONTAINER_DIR="org.nimbustools.auto_common.dirmgr.CreateNewNumberedDirectory"
export EXE_CREATE_CONTAINER_DIR

EXE_CREATE_NEW_CA="org.nimbustools.auto_common.ezpz_ca.GenerateNewCA"
export EXE_CREATE_NEW_CA

EXE_CREATE_NEW_CERT="org.nimbustools.auto_common.ezpz_ca.GenerateNewCert"
export EXE_CREATE_NEW_CERT

EXE_FIND_CA_PUBPEM="org.nimbustools.auto_common.ezpz_ca.FindCAPubFile"
EXE_FIND_CA_PRIVPEM="org.nimbustools.auto_common.ezpz_ca.FindCAPrivFile"
export EXE_FIND_CA_PUBPEM EXE_FIND_CA_PRIVPEM

EXE_CREATE_NEW_CERT_SHELL_SCRIPT_NAME="create-new-cert.sh"
export EXE_CREATE_NEW_CERT_SHELL_SCRIPT_NAME

AUTOCOMMON_ENVIRONMENT_DEFINED="X"
export AUTOCOMMON_ENVIRONMENT_DEFINED

