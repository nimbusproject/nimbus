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
# {{{  backup pool
# -----------------------------------------------------------------------------

if [ "$DO_POOL" = "y" ]; then
  
  VMMPOOLDIR=`$JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_GET_VMMDIR`
  if [ $? -ne 0 ]; then
    exit 1
  fi
  
  echo "[*] Backing up old resource pool settings"
  
  if [ ! -d $VMMPOOLDIR ]; then
    echo "Not a directory? '$VMMPOOLDIR'"
    exit 1
  fi
  
  BASE_BACKUP_DIR="$VMMPOOLDIR/.backups"
  
  if [ -d "$BASE_BACKUP_DIR" ]; then
    #echo "    ... base backups directory exists already '$BASE_BACKUP_DIR'"
    true
  else
    mkdir $BASE_BACKUP_DIR
    if [ $? -ne 0 ]; then
      echo ""
      echo "Problem creating directory: $BASE_BACKUP_DIR"
      exit 1
    fi
    #echo "    ... created base backup directory '$BASE_BACKUP_DIR'"
  fi
  
  NEW_BACKUP_DIR=`$JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_CREATE_BACKUP_DIR $BASE_BACKUP_DIR old-pools-`
  if [ $? -ne 0 ]; then
    echo ""
    echo "Problem, exiting."
    exit 1
  fi
  echo "    ... created new directory '$NEW_BACKUP_DIR'"
  
  for f in `ls $VMMPOOLDIR`; do
  
    CMD="mv $VMMPOOLDIR/$f $NEW_BACKUP_DIR/"
    $CMD
    if [ $? -ne 0 ]; then
      echo "This failed: $CMD"
      exit 1
    fi
    echo "    ... moved '$f' to '$NEW_BACKUP_DIR'"
  
  done
  
  echo ""
  echo "----------"
  echo ""
  
fi

# }}}

# -----------------------------------------------------------------------------
# {{{  create sample pool
# -----------------------------------------------------------------------------

if [ "$DO_POOL" = "y" ]; then

  echo "[*] Creating new resource pool"
  
  POOLPATH="$VMMPOOLDIR/$NEW_POOL_NAME"
  
  cp $RESOURCE_POOL_TEMPLATE_FILE $POOLPATH
  if [ $? -ne 0 ]; then
    echo "Could not create '$POOLPATH' ?"
    exit 1
  fi
  
  DATE=`date`
  echo "# File contents injected @ $DATE" >> $POOLPATH
  if [ $? -ne 0 ]; then
    echo "Could not write to '$POOLPATH' ?"
    exit 1
  fi
  
  echo "$NIMBUS_CONFIG_TEST_VMM $NIMBUS_CONFIG_TEST_VMM_RAM" >> $POOLPATH
  if [ $? -ne 0 ]; then
    echo "Could not write to '$POOLPATH' ?"
    exit 1
  fi
  
  echo "    ... created '$POOLPATH'"
  
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

echo "Finished."
echo ""
