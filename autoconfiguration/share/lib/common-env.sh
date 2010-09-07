if [ "X$NIMWIZ_ENVIRONMENT_DEFINED" != "X" ]; then
  return 0
fi

# To turn on some debugging, uncomment the next line:
#JAVA_DEBUG="true"

if [ "X" = "X$NIMBUS_WIZARD_LIBDIR" ]; then
  echo "Cannot find configuration wizard lib directory."
  return 1
fi

NIMBUS_WIZARD_RUNDIR=`cd $NIMBUS_WIZARD_LIBDIR/../; pwd`
NIMBUS_WIZARD_LIBDIR=`cd $NIMBUS_WIZARD_LIBDIR; pwd`

NIMWIZ_DECISIONS_FILE="$NIMBUS_WIZARD_RUNDIR/autoconfig-decisions.sh"
export NIMWIZ_DECISIONS_FILE

NIMWIZ_MAIN_SCRIPT="$NIMBUS_WIZARD_RUNDIR/autoconfig.sh"
export NIMWIZ_MAIN_SCRIPT

NIMWIZ_ADJUSTMENTS_SCRIPT="$NIMBUS_WIZARD_RUNDIR/autoconfig-adjustments.sh"
export NIMWIZ_ADJUSTMENTS_SCRIPT

NIMWIZ_DECISIONS_FILE_TEMPLATE="$NIMBUS_WIZARD_LIBDIR/decisions-template.sh"
export NIMWIZ_DECISIONS_FILE_TEMPLATE

NIMWIZ_TRANSFER_TESTFILE="$NIMBUS_WIZARD_LIBDIR/transfer-test-file.txt"
export NIMWIZ_TRANSFER_TESTFILE

NIMWIZ_SSH_EXE="ssh"
export NIMWIZ_SSH_EXE

NIMWIZ_SSH_BATCH_OPTIONS="-T -n -o BatchMode=yes"
export NIMWIZ_SSH_BATCH_OPTIONS

NIMWIZ_SCP_EXE="scp"
export NIMWIZ_SCP_EXE

NIMWIZ_SCP_BATCH_OPTIONS="-o BatchMode=yes"
export NIMWIZ_SCP_BATCH_OPTIONS

NIMWIZ_NO_NETWORK_CONFIGS="(( not configuring ))"
export NIMWIZ_NO_NETWORK_CONFIGS

WORKSPACE_CONTROL_DOC_LINK="http://www.nimbusproject.org/docs/current/admin/"
export WORKSPACE_CONTROL_DOC_LINK

CONTROL_EXE="/opt/nimbus/bin/workspace-control.sh"
export CONTROL_EXE

CONTROL_TMPDIR="/opt/nimbus/var/workspace-control/tmp"
export CONTROL_TMPDIR

CLASSPATH_BASE1=$GLOBUS_LOCATION/lib

if [ "X$JAVA_DEBUG" = "Xtrue" ]; then
  NIMWIZ_JAVA_OPTS="-Dnimbus.wizard.debug=true -DGLOBUS_LOCATION=$GLOBUS_LOCATION -classpath ."
else
  NIMWIZ_JAVA_OPTS="-DGLOBUS_LOCATION=$GLOBUS_LOCATION -classpath ."
fi



if [ -d $CLASSPATH_BASE1 ]; then
  for f in `ls $CLASSPATH_BASE1`; do
    NIMWIZ_JAVA_OPTS="$NIMWIZ_JAVA_OPTS:$CLASSPATH_BASE1/$f"
  done
fi

export NIMWIZ_JAVA_OPTS

EXE_JVMCHECK="org.nimbustools.auto_common.JVMCheck"
export EXE_JVMCHECK

EXE_HOSTGUESS="org.nimbustools.auto_common.HostGuess"
export EXE_HOSTGUESS

EXE_NETPING="org.nimbustools.auto_config.net.NetworkPing"
export EXE_NETPING

EXE_HOSTLOOKUP="org.nimbustools.auto_config.net.ResolveHostname"
export EXE_HOSTLOOKUP

EXE_ALTER_SSHDCONTACT="org.nimbustools.auto_config.confmgr.AlterSSHdContactString"
export EXE_ALTER_SSHDCONTACT

EXE_ALTER_CONTROLUSER="org.nimbustools.auto_config.confmgr.AlterControlUser"
export EXE_ALTER_CONTROLUSER

EXE_ALTER_CONTROLUSERKEY="org.nimbustools.auto_config.confmgr.AlterControlUserKey"
export EXE_ALTER_CONTROLUSERKEY

EXE_ALTER_CONTROLEXE="org.nimbustools.auto_config.confmgr.AlterControlPath"
export EXE_ALTER_CONTROLEXE

EXE_ALTER_CONTROLTMPDIR="org.nimbustools.auto_config.confmgr.AlterControlTmpDir"
export EXE_ALTER_CONTROLTMPDIR

EXE_GET_VMMDIR="org.nimbustools.auto_config.confmgr.GetVmmPoolDirectory"
export EXE_GET_VMMDIR

EXE_CREATE_BACKUP_DIR="org.nimbustools.auto_common.dirmgr.CreateNewNumberedDirectory"
export EXE_CREATE_BACKUP_DIR

RESOURCE_POOL_TEMPLATE_FILE="$NIMBUS_WIZARD_LIBDIR/resource-pool-template.txt"
export RESOURCE_POOL_TEMPLATE_FILE

NEW_POOL_NAME="testpool"
export NEW_POOL_NAME

EXE_ALTER_FAKEMODE="org.nimbustools.auto_config.confmgr.AlterFakeMode"
export EXE_ALTER_FAKEMODE

NIMWIZ_ENVIRONMENT_DEFINED="X"
export NIMWIZ_ENVIRONMENT_DEFINED


