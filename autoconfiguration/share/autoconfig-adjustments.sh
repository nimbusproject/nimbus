#!/bin/bash

# Copyright 1999-2008 University of Chicago
# 
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy
# of the License at
# 
#    http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

echo ""
echo "# ------------------------------------------- #"
echo "# Nimbus auto-configuration: make adjustments #"
echo "# ------------------------------------------- #"
echo ""

# -----------------------------------------------------------------------------
# {{{  get nimbus-wizard environment
# -----------------------------------------------------------------------------

THISDIR_REL="`dirname $0`"
THISDIR=`cd $THISDIR_REL; pwd`

GLOBUS_LOCATION=`cd $THISDIR/../../; pwd`
export GLOBUS_LOCATION

NIMBUS_HOME=`cd $GLOBUS_LOCATION/../; pwd`
export NIMBUS_HOME

NIMBUS_BIN="$NIMBUS_HOME/bin"
NIMBUS_NODES="$NIMBUS_BIN/nimbus-nodes"
NIMBUSCTL="$NIMBUS_BIN/nimbusctl"

LIBDIR="$THISDIR/lib/"
if [ ! -f $LIBDIR/common-env.sh ]; then
  echo "Failure, cannot find environment definitions"
  exit 1
fi
NIMBUS_WIZARD_LIBDIR="$LIBDIR"
export NIMBUS_WIZARD_LIBDIR
source $LIBDIR/common-env.sh
if [ $? -ne 0 ]; then
  echo "Problem retrieving environment definitions."
  exit 1
fi

# }}}

# -----------------------------------------------------------------------------
# {{{  dependency checks
# -----------------------------------------------------------------------------

JAVA_BIN="java"

function nobin() {
  echo ""
  echo "ERROR: cannot find $1"
  echo "- install $1"
  echo "- OR adjust the configuration value at the top of this script to point to $1"
  echo ""
  exit 1
}

CAPTURE=`$JAVA_BIN -version 2>&1`
if [ $? -ne 0 ]; then
  nobin java
fi

$JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_JVMCHECK
if [ $? -ne 0 ]; then
  exit 1
fi

# }}}

# -----------------------------------------------------------------------------
# {{{  find decisions environment variables
# -----------------------------------------------------------------------------

if [ ! -f $NIMWIZ_DECISIONS_FILE ]; then
  echo "Could not find '$NIMWIZ_DECISIONS_FILE'"
  echo ""
  echo "Did you run '$NIMWIZ_MAIN_SCRIPT' ?"
  exit 1
fi

source $NIMWIZ_DECISIONS_FILE
if [ $? -ne 0 ]; then
  echo "Problem sourcing '$NIMWIZ_DECISIONS_FILE'"
  exit 1
fi

echo "Read settings from '$NIMWIZ_DECISIONS_FILE'"
echo ""
echo "----------"
echo ""

# }}}

# -----------------------------------------------------------------------------
# {{{  print decisions environment variables
# -----------------------------------------------------------------------------

NOT_SET="(( not set ))"

# arg1 var name
# arg2 var val
function examine() {
  if [ "X" = "X$2" ]; then
    export $1="$NOT_SET"
  else
    export $1="$2"
  fi
}

examine NIMBUS_CONFIG_CONTAINER_RUNNER "$NIMBUS_CONFIG_CONTAINER_RUNNER"
examine NIMBUS_CONFIG_VMM_RUNNER "$NIMBUS_CONFIG_VMM_RUNNER"
examine NIMBUS_CONFIG_VMM_RUN_KEY "$NIMBUS_CONFIG_VMM_RUN_KEY"
examine NIMBUS_CONFIG_CONTAINER_HOSTNAME "$NIMBUS_CONFIG_CONTAINER_HOSTNAME"
examine NIMBUS_CONFIG_SSH_USE_CONTACT_STRING "$NIMBUS_CONFIG_SSH_USE_CONTACT_STRING"
examine NIMBUS_CONFIG_SSH_USE_CONTACT_PORT "$NIMBUS_CONFIG_SSH_USE_CONTACT_PORT"
examine NIMBUS_CONFIG_TEST_VMM "$NIMBUS_CONFIG_TEST_VMM"
examine NIMBUS_CONFIG_TEST_VMM_RAM "$NIMBUS_CONFIG_TEST_VMM_RAM"
examine NIMBUS_CONFIG_VMM_CONTROL_EXE "$NIMBUS_CONFIG_VMM_CONTROL_EXE"
examine NIMBUS_CONFIG_VMM_CONTROL_TMPDIR "$NIMBUS_CONFIG_VMM_CONTROL_TMPDIR"
examine NIMBUS_CONFIG_TEST_VM_NETWORK_ADDRESS "$NIMBUS_CONFIG_TEST_VM_NETWORK_ADDRESS"
examine NIMBUS_CONFIG_TEST_VM_NETWORK_HOSTNAME "$NIMBUS_CONFIG_TEST_VM_NETWORK_HOSTNAME"
examine NIMBUS_CONFIG_TEST_VM_NETWORK_GATEWAY "$NIMBUS_CONFIG_TEST_VM_NETWORK_GATEWAY"
examine NIMBUS_CONFIG_TEST_VM_NETWORK_DNS "$NIMBUS_CONFIG_TEST_VM_NETWORK_DNS"


if [ "Xnoprint" != "X$1" ]; then

  echo ""
  echo " - The account running the container/service: $NIMBUS_CONFIG_CONTAINER_RUNNER"
  echo " - The hostname running the container/service: $NIMBUS_CONFIG_CONTAINER_HOSTNAME"
  echo " - The contact address of the container/service for notifications: $NIMBUS_CONFIG_SSH_USE_CONTACT_STRING (port $NIMBUS_CONFIG_SSH_USE_CONTACT_PORT)"
  echo ""
  echo " - The test VMM: $NIMBUS_CONFIG_TEST_VMM"
  echo " - The available RAM on that VMM: $NIMBUS_CONFIG_TEST_VMM_RAM"
  echo " - The privileged account on the VMM: $NIMBUS_CONFIG_VMM_RUNNER"
  if [ "X" != "X$NIMBUS_CONFIG_VMM_RUN_KEY" ]; then
    echo " - The SSH key needed to access the '$NIMBUS_CONFIG_VMM_RUNNER' account on VMM: $NIMBUS_CONFIG_VMM_RUN_KEY"
  fi
  echo ""
  echo " - The workspace-control path on VMM: $NIMBUS_CONFIG_VMM_CONTROL_EXE"
  echo " - The workspace-control tmpdir on VMM: $NIMBUS_CONFIG_VMM_CONTROL_TMPDIR"
  
  echo ""
  echo "----------"
  echo ""

fi

# }}}

# -----------------------------------------------------------------------------
# {{{  ssh.conf --> service.sshd.contact.string
# -----------------------------------------------------------------------------

if [ "$NOT_SET" = "$NIMBUS_CONFIG_SSH_USE_CONTACT_STRING" ]; then
  
  echo "Warning: no information available to adjust ssh.conf --> service.sshd.contact.string"
  
else
  
  if [ "$NOT_SET" = "$NIMBUS_CONFIG_SSH_USE_CONTACT_PORT" ]; then
    VALUE="$NIMBUS_CONFIG_SSH_USE_CONTACT_STRING"
  else
    VALUE="$NIMBUS_CONFIG_SSH_USE_CONTACT_STRING:$NIMBUS_CONFIG_SSH_USE_CONTACT_PORT"
  fi
  
  $JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_ALTER_SSHDCONTACT "$VALUE"
  if [ $? -ne 0 ]; then
    exit 1
  fi
fi

echo ""
echo "----------"
echo ""

# }}}

# -----------------------------------------------------------------------------
# {{{  ssh.conf --> control.ssh.user
# -----------------------------------------------------------------------------

if [ "$NOT_SET" = "$NIMBUS_CONFIG_VMM_RUNNER" ]; then
  echo "Warning: no information available to adjust ssh.conf --> control.ssh.user"
else
  VALUE="$NIMBUS_CONFIG_VMM_RUNNER"
  $JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_ALTER_CONTROLUSER "$VALUE"
  if [ $? -ne 0 ]; then
    exit 1
  fi
fi

echo ""
echo "----------"
echo ""

# }}}

# -----------------------------------------------------------------------------
# {{{  ssh.conf --> use.identity
# -----------------------------------------------------------------------------

if [ "$NOT_SET" = "$NIMBUS_CONFIG_VMM_RUN_KEY" ]; then
  CMD="$JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_ALTER_CONTROLUSERKEY"
else
  CMD="$JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_ALTER_CONTROLUSERKEY $NIMBUS_CONFIG_VMM_RUN_KEY"
fi

$CMD
if [ $? -ne 0 ]; then
  exit 1
fi

echo ""
echo "----------"
echo ""

# }}}

# -----------------------------------------------------------------------------
# {{{  vmm.conf --> control.path
# -----------------------------------------------------------------------------

if [ "$NOT_SET" = "$NIMBUS_CONFIG_VMM_CONTROL_EXE" ]; then
  echo "Warning: no information available to adjust vmm.conf --> control.path"
else
  VALUE="$NIMBUS_CONFIG_VMM_CONTROL_EXE"
  $JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_ALTER_CONTROLEXE "$VALUE"
  if [ $? -ne 0 ]; then
    exit 1
  fi
fi

echo ""
echo "----------"
echo ""

# }}}

# -----------------------------------------------------------------------------
# {{{  vmm.conf --> control.tmp.dir
# -----------------------------------------------------------------------------

if [ "$NOT_SET" = "$NIMBUS_CONFIG_VMM_CONTROL_TMPDIR" ]; then
  echo "Warning: no information available to adjust vmm.conf --> control.tmp.dir"
else
  VALUE="$NIMBUS_CONFIG_VMM_CONTROL_TMPDIR"
  $JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_ALTER_CONTROLTMPDIR "$VALUE"
  if [ $? -ne 0 ]; then
    exit 1
  fi
fi

echo ""
echo "----------"
echo ""

# }}}

# -----------------------------------------------------------------------------
# {{{  do new pool?
# -----------------------------------------------------------------------------

DO_POOL="y"

if [ "$NOT_SET" = "$NIMBUS_CONFIG_TEST_VMM" ]; then
  echo "Warning: not enough information available to create a new list of VMMs (a new resource pool).  No test VMM configured."
  DO_POOL="n"
fi

if [ "$DO_POOL" = "y" ]; then
  if [ "$NOT_SET" = "$NIMBUS_CONFIG_TEST_VMM_RAM" ]; then
    echo "Warning: not enough information available to create a new list of VMMs (a new resource pool).  No test VMM RAM configured."
    DO_POOL="n"
    
  fi
fi

if [ "$DO_POOL" = "n" ]; then
  echo ""
  echo "----------"
  echo ""
fi
  
# }}}

# -----------------------------------------------------------------------------
# {{{  turn off fake mode;  other/common.conf --> fake.mode (set to false)
# -----------------------------------------------------------------------------

echo "Making sure 'fake mode' is off:"
echo ""

$JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_ALTER_FAKEMODE false
if [ $? -ne 0 ]; then
  exit 1
fi

echo ""
echo "----------"
echo ""


# }}}

# -----------------------------------------------------------------------------
# {{{  add first VMM
# -----------------------------------------------------------------------------

if [ "$DO_POOL" = "y" ]; then

  echo "[*] Adding first VMM node"
  
  ADD_CMD="$NIMBUS_NODES --add $NIMBUS_CONFIG_TEST_VMM --memory $NIMBUS_CONFIG_TEST_VMM_RAM"

  $NIMBUSCTL services status >/dev/null 2>&1
  if [ $? -ne 0 ]; then
    echo ""
    echo "The Nimbus service is not running. It must be started before your first VMM node can be added. You can start Nimbus with this command:"
    echo "  $NIMBUSCTL start"
    echo "And then add the VMM yourself:"
    echo "  $ADD_CMD"
    VMM_NOT_ADDED="yes"
  
  else
    
    echo "Running '$ADD_CMD'.."
    $ADD_CMD
    if [ $? -ne 0 ]; then
      echo "Failed to add VMM node!"
      echo "This could be because Nimbus is not running. Or it could be a problem communicating with the service, which happens via Unix domain sockets. If Nimbus is installed on a distributed or network filesystem, domain sockets may not be supported. In this case, you can configure a different socket directory that is on a local filesystem. Look at the admin.conf file for details."
      echo ""
      echo "Once you have resolved these issues, use this command to add the VMM node:"
      echo "  $ADD_CMD"
      VMM_NOT_ADDED="yes"
    else
      echo "Added node."
    fi
    
  fi 
  
  echo ""
  echo "----------"
  echo ""
fi

# }}}

echo "Finished."
if [ "X$VMM_NOT_ADDED" = "Xyes" ]; then
  echo "WARNING: No VMM nodes are configured! Please see above for instructions."
fi
echo ""
