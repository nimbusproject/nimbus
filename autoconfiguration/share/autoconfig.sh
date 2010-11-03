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
echo "# ------------------------- #"
echo "# Nimbus auto-configuration #"
echo "# ------------------------- #"
echo ""

# NOTE: this sets GLOBUS_LOCATION to the location it's built into:

THISDIR_REL="`dirname $0`"
THISDIR=`cd $THISDIR_REL; pwd`

GLOBUS_LOCATION=`cd $THISDIR/../../; pwd`
export GLOBUS_LOCATION

NIMBUS_HOME=`cd $GLOBUS_LOCATION/../; pwd`
export NIMBUS_HOME

NIMBUS_BIN="$NIMBUS_HOME/bin"
NIMBUSCTL="$NIMBUS_BIN/nimbusctl"

# -----------------------------------------------------------------------------
# {{{  get nimbus-wizard environment
# -----------------------------------------------------------------------------

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
# {{{  see if decisions file exists already
# -----------------------------------------------------------------------------

function unset_all_global_config_settings() {
  unset NIMBUS_CONFIG_CONTAINER_RUNNER
  unset NIMBUS_CONFIG_VMM_RUNNER
  unset NIMBUS_CONFIG_VMM_RUN_KEY
  unset NIMBUS_CONFIG_CONTAINER_HOSTNAME
  unset NIMBUS_CONFIG_SSH_USE_CONTACT_STRING
  unset NIMBUS_CONFIG_SSH_USE_CONTACT_PORT
  unset NIMBUS_CONFIG_TEST_VMM
  unset NIMBUS_CONFIG_TEST_VMM_RAM
  unset NIMBUS_CONFIG_VMM_CONTROL_EXE
  unset NIMBUS_CONFIG_VMM_CONTROL_TMPDIR
  unset NIMBUS_CONFIG_TEST_VM_NETWORK_ADDRESS
  unset NIMBUS_CONFIG_TEST_VM_NETWORK_HOSTNAME
  unset NIMBUS_CONFIG_TEST_VM_NETWORK_GATEWAY
  unset NIMBUS_CONFIG_TEST_VM_NETWORK_DNS
}

unset_all_global_config_settings

if [ -f $NIMWIZ_DECISIONS_FILE ]; then

  source $NIMWIZ_DECISIONS_FILE
  
  if [ "X" != "X$NIMBUS_CONFIG_CONTAINER_RUNNER$NIMBUS_CONFIG_VMM_RUNNER$NIMBUS_CONFIG_VMM_RUN_KEY$NIMBUS_CONFIG_CONTAINER_HOSTNAME$NIMBUS_CONFIG_SSH_USE_CONTACT_STRING$NIMBUS_CONFIG_TEST_VMM$NIMBUS_CONFIG_VMM_CONTROL_EXE$NIMBUS_CONFIG_VMM_CONTROL_TMPDIR$NIMBUS_CONFIG_TEST_VMM_RAM" ]; then
  
    echo ""
    echo "It looks like you already have a file with one or more configuration decisions at:"
    echo "    $NIMWIZ_DECISIONS_FILE"
    echo ""
    echo "If you would like to use these settings, hit CTR-C and run this:"
    echo "    $NIMWIZ_ADJUSTMENTS_SCRIPT"
    echo ""
    echo "Otherwise, hit return and it will be overwritten."
    read ignored_response_wait
  fi
fi


# }}}

# -----------------------------------------------------------------------------
# {{{  FUNCTIONS: prompts
# -----------------------------------------------------------------------------

function get_y_n() {
  RESPONSE="undefined"
  count=0
  while [ $count -lt 6 ]; do
    count=$((count + 1))
    echo ""
    echo "$1 y/n:"
    read -e response
    if [ "$response" = "y" ]; then
      RESPONSE="y"
      count=10
    elif [ "$response" = "n" ]; then
      RESPONSE="n"
      count=10
    else
      echo "Please enter 'y' or 'n'"
    fi
  done
  
  if [ "undefined" = "$RESPONSE" ]; then
    echo ""
    echo "Exiting, no response"
    exit 1
  fi
  
  if [ "y" = "$RESPONSE" ]; then
    return 0
  fi
  if [ "n" = "$RESPONSE" ]; then
    return 1
  fi
}

function get_STRING_ANSWER() {
  unset STRING_ANSWER
  
  count=0
  while [ $count -lt 6 ]; do
    count=$((count + 1))
    echo ""
    echo "$1"
    read -e response
    if [ "X$response" = "X" ]; then
      echo ""
      echo "Please enter something."
    else
      STRING_ANSWER="$response"
      count=10
    fi
  done
  
  if [ "X" = "X$STRING_ANSWER" ]; then
    echo ""
    echo "Exiting, no response."
    echo ""
    exit 1
  fi
}

# }}}

# -----------------------------------------------------------------------------
# {{{  choose container running user
# -----------------------------------------------------------------------------

CONTAINER_RUNNER=`whoami`

get_y_n "Is the current account ($CONTAINER_RUNNER) the one the service will run under?"
if [ $? -ne 0 ]; then
  echo ""
  echo "Sorry, please run the wizard with the account the container will run under on the machine the container will run on."
  exit 1
fi

if [ "$CONTAINER_RUNNER" = "root" ]; then

  echo ""
  echo "*** Running the container/service as root is NOT RECOMMENDED AT ALL ***"
  echo ""
  echo "If you understand and want to proceed anyhow, you can come and comment out this exit call :-)"
  echo ""
  echo "Bye."
  exit 1
fi

# }}}

# -----------------------------------------------------------------------------
# {{{  pick test VMM
# -----------------------------------------------------------------------------

QUESTION="Pick a VMM to test with, enter a hostname: "
get_STRING_ANSWER "$QUESTION"
TEST_VMM="$STRING_ANSWER"

echo ""
echo "----------"

QUESTION="How much RAM (MB) should be allocated for VMs on the '$TEST_VMM' VMM?"
get_STRING_ANSWER "$QUESTION"
TEST_VMM_RAM="$STRING_ANSWER"

echo ""
echo "Will allocate $TEST_VMM_RAM MB RAM for VMs on the '$TEST_VMM' VMM."

echo ""
echo "----------"

# }}}

# -----------------------------------------------------------------------------
# {{{  choose VMM running user
# -----------------------------------------------------------------------------

VMM_RUNNER="$CONTAINER_RUNNER"

get_y_n "Is the current account ($VMM_RUNNER) also the account the privileged scripts will run under on the VMM ($TEST_VMM)?"

if [ $? -ne 0 ]; then
  echo "OK, pick another account name."
  echo ""
  echo "This should be a dedicated account, the current user ($CONTAINER_RUNNER) will run *privileged* commands with it using SSH."
  
  QUESTION="What is that account name?"
  get_STRING_ANSWER "$QUESTION"
  VMM_RUNNER="$STRING_ANSWER"
fi

WCSSH_CONTACT="$VMM_RUNNER@$TEST_VMM"

# }}}

# -----------------------------------------------------------------------------
# {{{  special key for VMM account?
# -----------------------------------------------------------------------------

VMM_RUN_KEY=""
unset -f strlen
function strlen (){
    eval echo "\${#${1}}"
}

get_y_n "Does the container account ($CONTAINER_RUNNER) need a special (non-default) SSH key to access the '$VMM_RUNNER' account on the VMM nodes?"
if [ $? -eq 0 ]; then
  
  NO_RUNKEY_RESPONSE="undefined_129831298739821y39821y39821y39821y31"
  VMM_RUN_KEY="$NO_RUNKEY_RESPONSE"

  count=0
  while [ $count -lt 4 ]; do
    count=$((count + 1))
    echo ""
    echo "What is the absolute path to the private key to use that allows '$CONTAINER_RUNNER' to ssh to '$VMM_RUNNER'? "
    read -e response
    if [ "X$response" != "X" ]; then
      if [ "${response:0:1}" != "/" ]; then
        echo "Not an absolute path: $response"
      else
        VMM_RUN_KEY="$response"
        count=10
      fi
    else
      echo "Please enter an absolute path."
    fi
  done
  
  if [ "$NO_RUNKEY_RESPONSE" = "$VMM_RUN_KEY" ]; then
    echo ""
    echo "Exiting, no response"
    exit 1
  fi
fi

# }}}

# -----------------------------------------------------------------------------
# {{{  test w-c user (1)
# -----------------------------------------------------------------------------

echo ""
echo "----------"
echo ""
echo "Testing basic SSH access to $WCSSH_CONTACT"
echo ""

SSH_CMD_BEGINNING="$NIMWIZ_SSH_EXE"
if [ "X" != "X$VMM_RUN_KEY" ]; then
  SSH_CMD_BEGINNING="$NIMWIZ_SSH_EXE -i $VMM_RUN_KEY"
fi

unset SSH_CMD
SSH_CMD="$SSH_CMD_BEGINNING $NIMWIZ_SSH_BATCH_OPTIONS $WCSSH_CONTACT"
echo "Test command (1): $SSH_CMD /bin/true"
echo ""

SSH_TRUE_TEST_PASSED="n"
count=0
while [ $count -lt 6 ]; do
  count=$((count + 1))
  $SSH_CMD /bin/true
  if [ $? -eq 0 ]; then
    SSH_TRUE_TEST_PASSED="y"
    count=10
  else
    echo ""
    echo "*** That failed."
    echo ""
    echo "Try it manually in another terminal?  There should be no keyboard interaction necessary for this test to pass."
    echo ""
    echo "You may need to run it first without extra options, and perhaps accept the host key.  For example, try this in another terminal (make sure you are using the current '$CONTAINER_RUNNER' account):"
    echo ""
    echo "$SSH_CMD_BEGINNING $WCSSH_CONTACT /bin/true"
    echo ""
    echo "Hit return when you are ready to try the test again:"
    read ignore_response
  fi
done

if [ "n" = "$SSH_TRUE_TEST_PASSED" ]; then
  echo "Exiting, could not make progress"
  exit 1
else
  echo "Basic SSH test (1) working to $WCSSH_CONTACT"
fi

# }}}

# -----------------------------------------------------------------------------
# {{{  hostname decision
# -----------------------------------------------------------------------------

echo ""
echo "----------"
echo ""
echo "Now we'll set up the *hostname* that VMMs will use to contact the container over SSHd"
echo ""
echo "Even if you plan on ever setting up just one VMM and it is localhost to the container, you should still pick a hostname here ('localhost' if you must)"

HOSTGUESS=`$JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_HOSTGUESS`

if [ "localhost" = "$HOSTGUESS" ]; then

  echo ""
  echo "*** It does not look like you have a hostname set up, the best guess is 'localhost' but this will probably not work from other hosts.  That may be fine if you just want to test with a local hypervisor."

elif [ "X" = "X$HOSTGUESS" ]; then

  echo ""
  echo "*** Problem guessing hostname."

else
  echo ""
  echo "*** It looks like you have a hostname set up: $HOSTGUESS"

fi

CONTAINER_HOSTNAME="$HOSTGUESS"

get_y_n "Would you like to manually enter a different hostname?"
if [ $? -eq 0 ]; then
  QUESTION="Please enter the hostname to use:"
  get_STRING_ANSWER "$QUESTION"
  CONTAINER_HOSTNAME="$STRING_ANSWER"
fi

echo ""
echo "Using hostname: $CONTAINER_HOSTNAME"
echo ""
echo "----------"

# }}}

# -----------------------------------------------------------------------------
# {{{  SSH configurations
# -----------------------------------------------------------------------------

SSH_PORT="undefined"
count=0
while [ $count -lt 6 ]; do
  count=$((count + 1))
  echo ""
  echo "Is your local SSHd server on a port different than 22?  Enter 'n' or a port number: "
  read -e ssh_port
  if [ "$ssh_port" = "n" ]; then
    SSH_PORT=22
    count=10
  elif [ $ssh_port -lt 65536 ]; then
    if [ $ssh_port -gt 0 ]; then
      SSH_PORT=$ssh_port
      count=10
    fi
  fi
  
  if [ "undefined" = "$SSH_PORT" ]; then
    echo "Please enter 'n' or an integer between 0 and 65536"
  fi
done

if [ "undefined" = "$SSH_PORT" ]; then
  echo ""
  echo "Exiting, no response"
  exit 1
fi

echo ""
echo "Attempting to connect to: $CONTAINER_HOSTNAME:$SSH_PORT"
echo ""

PING_TEST_PASSED="n"
count=0
while [ $count -lt 6 ]; do
  count=$((count + 1))
  $JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_NETPING $CONTAINER_HOSTNAME $SSH_PORT
  if [ $? -eq 0 ]; then
    PING_TEST_PASSED="y"
    count=10
  else
    echo ""
    echo "It does not seem like there is any server @ $CONTAINER_HOSTNAME:$SSH_PORT"
    echo ""
    echo "If you need to change something in the provided answers, re-run the wizard."
    echo ""
    echo "Otherwise, start a server and press any key to try the test again"
    read -e response
  fi
done

if [ "n" = "$PING_TEST_PASSED" ]; then
  echo "Exiting, could not make progress"
  exit 1
else
  echo "Contacted a server @ $CONTAINER_HOSTNAME:$SSH_PORT"
fi

echo ""

SSH_USE_CONTACT_STRING="$CONTAINER_RUNNER@$CONTAINER_HOSTNAME"
SSH_USE_CONTACT_PORT="$SSH_PORT"

# }}}

# -----------------------------------------------------------------------------
# {{{  test w-c user (2)
# -----------------------------------------------------------------------------

echo "----------"
echo ""
echo "Now we will test the basic SSH notification conduit from the VMM to the container"
echo ""
echo "Test command (2): $SSH_CMD ssh $NIMWIZ_SSH_BATCH_OPTIONS -p $SSH_USE_CONTACT_PORT $SSH_USE_CONTACT_STRING /bin/true"
echo ""

SSH_NOTIF_TEST_PASSED="n"
count=0
while [ $count -lt 8 ]; do
  count=$((count + 1))
  $SSH_CMD ssh $NIMWIZ_SSH_BATCH_OPTIONS -p $SSH_USE_CONTACT_PORT $SSH_USE_CONTACT_STRING /bin/true
  if [ $? -eq 0 ]; then
    SSH_NOTIF_TEST_PASSED="y"
    count=10
  else
    echo ""
    echo "*** That failed."
    echo ""
    echo "Try it manually in another terminal?  There should be no keyboard interaction necessary for this test to pass."
    echo ""
    echo "You may need to run it first without extra options, and perhaps accept the host key.  For example, try this in another terminal (make sure you are using the VMM account '$VMM_RUNNER' account on the test VMM node '$TEST_VMM'):"
    echo ""
    echo "ssh -p $SSH_USE_CONTACT_PORT $SSH_USE_CONTACT_STRING /bin/true"
    echo ""
    echo "Hit return when you are ready to try the test again:"
    read ignore_response
  fi
done

if [ "n" = "$SSH_NOTIF_TEST_PASSED" ]; then
  echo "Exiting, could not make progress"
  exit 1
else
  echo "Notification test (2) working (ssh from $WCSSH_CONTACT to $SSH_USE_CONTACT_STRING at port $SSH_USE_CONTACT_PORT)"
  echo ""
fi

# }}}

# -----------------------------------------------------------------------------
# {{{  test workspace-control bin program
# -----------------------------------------------------------------------------

echo "----------"
echo ""
echo "OK, looking good."
echo ""

echo "---------------------------------------------------------------------"
echo "---------------------------------------------------------------------"

echo ""
echo "If you have not followed the instructions for setting up workspace control yet, please do the basic installation steps now."

echo ""
echo "Look for the documentation at:"
echo "  - $WORKSPACE_CONTROL_DOC_LINK"

echo ""
echo "Waiting for you to install workspace control for the account '$VMM_RUNNER' on the test VMM '$TEST_VMM'"
echo ""
echo "After this is accomplished, press return to continue."
read ignore_any_key

# }}}

# -----------------------------------------------------------------------------
# {{{  find/test workspace-control
# -----------------------------------------------------------------------------

echo "----------"
echo ""
echo "Going to test container access to workspace control installation."

EFFECTIVE_VMM_CONTROL_EXE="$CONTROL_EXE"

QUESTION="On '$TEST_VMM', did you install workspace-control somewhere else besides '$CONTROL_EXE'?"
get_y_n "$QUESTION"
if [ $? -eq 0 ]; then
  
  NO_CONTROL_RESPONSE="undefined_129831298739821y39821y39821y39821y31"
  EFFECTIVE_VMM_CONTROL_EXE="$NO_CONTROL_RESPONSE"

  count=0
  while [ $count -lt 4 ]; do
    count=$((count + 1))
    echo ""
    echo "What is the absolute path to workspace-control on the VMM node?"
    read -e response
    if [ "X$response" != "X" ]; then
      if [ "${response:0:1}" != "/" ]; then
        echo "Not an absolute path: $response"
      else
        EFFECTIVE_VMM_CONTROL_EXE="$response"
        count=10
      fi
    else
      echo "Please enter an absolute path."
    fi
  done
  
  if [ "$NO_CONTROL_RESPONSE" = "$EFFECTIVE_VMM_CONTROL_EXE" ]; then
    echo ""
    echo "Exiting, no response"
    exit 1
  fi
fi

echo ""
echo "Test command (3): $SSH_CMD $EFFECTIVE_VMM_CONTROL_EXE -h 1>/dev/null"

CONTROL_TEST_PASSED="n"
count=0
while [ $count -lt 8 ]; do
  count=$((count + 1))
  echo ""
  $SSH_CMD $EFFECTIVE_VMM_CONTROL_EXE -h 1>/dev/null
  if [ $? -eq 0 ]; then
    CONTROL_TEST_PASSED="y"
    count=10
  else
    echo ""
    echo "*** That failed."
    echo ""
    echo "Hit return to try the test again (if you changed something):"
    read ignore_response
  fi
done

if [ "n" = "$CONTROL_TEST_PASSED" ]; then
  echo "Exiting, could not make progress"
  exit 1
else
  echo "Workspace control test (3) working"
fi

# }}}

# -----------------------------------------------------------------------------
# {{{  find/test workspace-control tmpdir
# -----------------------------------------------------------------------------

echo ""
echo "----------"
echo ""
echo "Testing ability to push files to workspace control installation."

RESPONSE="undefined"
count=0
while [ $count -lt 6 ]; do
  count=$((count + 1))
  echo ""
  echo "We are looking for the directory on the VMM to push customization files from the container node. This defaults to '$CONTROL_TMPDIR'"
  echo ""
  echo "Did you install workspace-control under some other base directory besides /opt/nimbus? y/n: "
  read -e response
  if [ "$response" = "y" ]; then
    RESPONSE="y"
    count=10
  elif [ "$response" = "n" ]; then
    RESPONSE="n"
    count=10
  else
    echo "Please enter 'y' or 'n'"
  fi
done

EFFECTIVE_VMM_CONTROL_TMPDIR="$CONTROL_TMPDIR"

if [ "y" = "$RESPONSE" ]; then
  
  NO_TMPDIR_RESPONSE="undefined_129831298739821y39821y39821y39821y31"
  EFFECTIVE_VMM_CONTROL_TMPDIR="$NO_TMPDIR_RESPONSE"

  count=0
  while [ $count -lt 4 ]; do
    count=$((count + 1))
    echo ""
    echo "What is the absolute path to the workspace-control temporary scratch directory on the VMM node?"
    echo ""
    echo "It's typically at 'BASEDIR/var/workspace-control/tmp' if workspace-control is at 'BASEDIR/bin/workspace-control.sh'"
    
    read -e response
    if [ "X$response" != "X" ]; then
      if [ "${response:0:1}" != "/" ]; then
        echo "Not an absolute path: $response"
      else
        EFFECTIVE_VMM_CONTROL_TMPDIR="$response"
        count=10
      fi
    else
      echo "Please enter an absolute path."
    fi
  done
  
  if [ "$NO_TMPDIR_RESPONSE" = "$EFFECTIVE_VMM_CONTROL_TMPDIR" ]; then
    echo ""
    echo "Exiting, no response"
    exit 1
  fi
fi

SCP_CMD_BEGINNING="$NIMWIZ_SCP_EXE"
if [ "X" != "X$VMM_RUN_KEY" ]; then
  SCP_CMD_BEGINNING="$NIMWIZ_SCP_EXE -i $VMM_RUN_KEY"
fi
SCP_CMD_PRE="$SCP_CMD_BEGINNING $NIMWIZ_SCP_BATCH_OPTIONS"
SCP_CMD_POST="$WCSSH_CONTACT:$EFFECTIVE_VMM_CONTROL_TMPDIR/"

echo "Test command (4): $SCP_CMD_PRE $NIMWIZ_TRANSFER_TESTFILE $SCP_CMD_POST"

SCP_TEST_PASSED="n"
count=0
while [ $count -lt 8 ]; do
  count=$((count + 1))
  echo ""
  $SCP_CMD_PRE $NIMWIZ_TRANSFER_TESTFILE $SCP_CMD_POST
  if [ $? -eq 0 ]; then
    SCP_TEST_PASSED="y"
    count=10
  else
    echo ""
    echo "*** That failed."
    echo ""
    echo "Hit return to try the test again (if you changed something):"
    read ignore_response
  fi
done

if [ "n" = "$SCP_TEST_PASSED" ]; then
  echo "Exiting, could not make progress"
  exit 1
else
  echo ""
  echo "SCP test (4) working"
fi

# }}}


# -----------------------------------------------------------------------------
# {{{  export decisions
# -----------------------------------------------------------------------------

echo "----------"
echo ""
echo "*** Changes to your configuration are about to be executed."
echo ""
echo "So far, no configurations have been changed.  The following adjustments will be made based on the questions and tests we just went through:"

unset_all_global_config_settings

NIMBUS_CONFIG_CONTAINER_RUNNER="$CONTAINER_RUNNER"
NIMBUS_CONFIG_VMM_RUNNER="$VMM_RUNNER"
NIMBUS_CONFIG_VMM_RUN_KEY="$VMM_RUN_KEY"
NIMBUS_CONFIG_CONTAINER_HOSTNAME="$CONTAINER_HOSTNAME"
NIMBUS_CONFIG_SSH_USE_CONTACT_STRING="$SSH_USE_CONTACT_STRING"
NIMBUS_CONFIG_SSH_USE_CONTACT_PORT="$SSH_USE_CONTACT_PORT"
NIMBUS_CONFIG_TEST_VMM="$TEST_VMM"
NIMBUS_CONFIG_TEST_VMM_RAM="$TEST_VMM_RAM"
NIMBUS_CONFIG_VMM_CONTROL_EXE="$EFFECTIVE_VMM_CONTROL_EXE"
NIMBUS_CONFIG_VMM_CONTROL_TMPDIR="$EFFECTIVE_VMM_CONTROL_TMPDIR"

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



cp $NIMWIZ_DECISIONS_FILE_TEMPLATE $NIMWIZ_DECISIONS_FILE
if [ $? -ne 0 ]; then
  echo "Could not create/copy over $NIMWIZ_DECISIONS_FILE ?"
  exit 1
fi


DATE=`date`
echo "# File contents injected @ $DATE" >> $NIMWIZ_DECISIONS_FILE
if [ $? -ne 0 ]; then
  echo "Could not write to $NIMWIZ_DECISIONS_FILE ?"
  exit 1
fi

echo "" >> $NIMWIZ_DECISIONS_FILE

# arg1 var name
# arg2 var val
function append_nameval() {
  echo "$1=\"$2\"" >> $NIMWIZ_DECISIONS_FILE
  echo "export $1" >> $NIMWIZ_DECISIONS_FILE
  echo "" >> $NIMWIZ_DECISIONS_FILE
}

append_nameval NIMBUS_CONFIG_CONTAINER_RUNNER "$NIMBUS_CONFIG_CONTAINER_RUNNER"
append_nameval NIMBUS_CONFIG_VMM_RUNNER "$NIMBUS_CONFIG_VMM_RUNNER"
append_nameval NIMBUS_CONFIG_VMM_RUN_KEY "$NIMBUS_CONFIG_VMM_RUN_KEY"

append_nameval NIMBUS_CONFIG_CONTAINER_HOSTNAME "$NIMBUS_CONFIG_CONTAINER_HOSTNAME"
append_nameval NIMBUS_CONFIG_SSH_USE_CONTACT_STRING "$NIMBUS_CONFIG_SSH_USE_CONTACT_STRING"
append_nameval NIMBUS_CONFIG_SSH_USE_CONTACT_PORT "$NIMBUS_CONFIG_SSH_USE_CONTACT_PORT"
append_nameval NIMBUS_CONFIG_TEST_VMM "$NIMBUS_CONFIG_TEST_VMM"
append_nameval NIMBUS_CONFIG_TEST_VMM_RAM "$NIMBUS_CONFIG_TEST_VMM_RAM"
append_nameval NIMBUS_CONFIG_VMM_CONTROL_EXE "$NIMBUS_CONFIG_VMM_CONTROL_EXE"
append_nameval NIMBUS_CONFIG_VMM_CONTROL_TMPDIR "$NIMBUS_CONFIG_VMM_CONTROL_TMPDIR"

echo "" >> $NIMWIZ_DECISIONS_FILE

# check if service is running
$NIMBUSCTL services status >/dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "The Nimbus service does not appear to be running, which is required for us to add the VMM node to the resource pool. You can start it now with this command:"
  echo "  $NIMBUSCTL start"
  echo ""
  echo "If you don't start the service, a command will be printed for you to run later and add the node."
  echo ""
  echo "Please optionally start the service and then hit enter."
  read ignore_response
fi

echo ""
echo "These settings are now stored in '$NIMWIZ_DECISIONS_FILE'"
echo ""
echo "If you type 'y', that script will be run for you with the settings."
echo ""
echo "Or you can answer 'n' to the next question and adjust this file."
echo "And then manually run '$NIMWIZ_ADJUSTMENTS_SCRIPT' at your leisure."
echo ""

PROCEED="n"
count=0
while [ $count -lt 6 ]; do
  count=$((count + 1))
  echo ""
  echo "OK, point of no return.  Proceed? y/n" 
  read -e do_proceed
  if [ "$do_proceed" = "y" ]; then
    PROCEED="y"
    count=10
  elif [ "$do_proceed" = "n" ]; then
    PROCEED="n"
    count=10
  else
    echo "Please enter 'y' or 'n'"
  fi
done

if [ "y" = "$PROCEED" ]; then

  echo ""
  echo "*** Running $NIMWIZ_ADJUSTMENTS_SCRIPT . . ."
  
  $NIMWIZ_ADJUSTMENTS_SCRIPT noprint

else

  echo ""
  echo "Alright.  See:"
  echo " - Contents of: $NIMWIZ_DECISIONS_FILE"
  echo " - Run this afterwards: $NIMWIZ_ADJUSTMENTS_SCRIPT"
  exit 0
fi

# }}}



