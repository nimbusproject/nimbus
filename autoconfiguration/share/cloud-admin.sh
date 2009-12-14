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

# NOTE: this sets GLOBUS_LOCATION to the location it's built into:

THISDIR_REL="`dirname $0`"
THISDIR=`cd $THISDIR_REL; pwd`

GLOBUS_LOCATION=`cd $THISDIR/../../; pwd`
export GLOBUS_LOCATION
#echo "Using GLOBUS_LOCATION: $GLOBUS_LOCATION"


# -----------------------------------------------------------------------------
# {{{  old-school opt system
# -----------------------------------------------------------------------------

OPT_ADD_DN="--add-dn"
OPT_DEL_DN="--del-dn"
OPT_FIND_DN="--find-dn"
OPT_HASH_DN="--hash-dn"
OPT_FINDHASH="--find-hash"
OPT_ALL_DNS="--all-dns"
OPT_AUTHZ_ON="--enable-groupauthz"
OPT_AUTHZ_OFF="--disable-groupauthz"
OPT_ADD_QUERY_DN="--add-query-dn"

# }}}

# -----------------------------------------------------------------------------
# {{{  help system
# -----------------------------------------------------------------------------

function help() {
  
  echo ""
  echo "# -------------------------------------- #"
  echo "# Nimbus auto-configuration: cloud-admin #"
  echo "# -------------------------------------- #"
  echo ""
  echo "$OPT_ADD_DN \"/CN=Some DN\"       Adds new DN (interactive)"
  echo ""
  echo "$OPT_DEL_DN \"/CN=Some DN\"       Deletes a DN (interactive)"
  echo ""
  echo "$OPT_FIND_DN \"/CN=Some DN\"      Checks for a DN"
  echo ""
  echo "$OPT_HASH_DN \"/CN=Some DN\"      Outputs cloud hash for a DN"
  echo ""
  echo "$OPT_ADD_QUERY_DN \"/CN=Some DN\" Authorizes DN for query interface (interactive)"
  echo ""
  echo "$OPT_FINDHASH 1234abcd         Looks in policies for a DN with this hash"
  echo ""
  echo "$OPT_ALL_DNS                    Prints all active DNs"
  echo ""
  echo "$OPT_AUTHZ_ON          Enables the groupauthz plugin"
  echo ""
  echo "$OPT_AUTHZ_OFF         Disables the groupauthz plugin"
  echo ""
  echo "-h, --help                   This help output"
  echo ""
  exit 1
}

if [ "X$1" = "X" ]; then
  help
fi

if [ "X$1" = "X-h" ]; then
  help
fi

if [ "X$1" = "X-help" ]; then
  help
fi

if [ "X$1" = "X--h" ]; then
  help
fi

if [ "X$1" = "X--help" ]; then
  help
fi

# }}}

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
# {{{  ACTION: all-DNS
# -----------------------------------------------------------------------------

action_all_dns() {
  
  OTHERDIR=`$JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_GET_OTHERCONF_DIR`
  CONF="$OTHERDIR/$ACTIVEAUTHZ_TARGET_CONF_FILE"
  $JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_GROUPAUTHZ_HARNESS reportAll $CONF
  return $?
}

# }}}

# -----------------------------------------------------------------------------
# {{{  ACTION: find-DN
# -----------------------------------------------------------------------------

action_find_dn() {
  OTHERDIR=`$JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_GET_OTHERCONF_DIR`
  CONF="$OTHERDIR/$ACTIVEAUTHZ_TARGET_CONF_FILE"
  $JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_GROUPAUTHZ_HARNESS report $CONF "$1"
  return $?
}

# }}}

# -----------------------------------------------------------------------------
# {{{  ACTION: hash-DN
# -----------------------------------------------------------------------------

action_hash_dn() {
  OTHERDIR=`$JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_GET_OTHERCONF_DIR`
  CONF="$OTHERDIR/$ACTIVEAUTHZ_TARGET_CONF_FILE"
  $JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_GROUPAUTHZ_HARNESS hash $CONF "$1"
  return $?
}

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
    read response
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
    read response
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
    rm $CLOUDADMIN_DECISIONS_FILE
    echo "Removed '$CLOUDADMIN_DECISIONS_FILE'"
    exit 1
  fi
}

# }}}

# -----------------------------------------------------------------------------
# {{{  FUNCTIONS: decisions file related
# -----------------------------------------------------------------------------

function unset_all_global_config_settings() {
  unset NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD
  unset NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD_PATH
  unset NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD_ASKFORUSER
  unset NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD_DUPUSERNAME
  unset NIMBUS_CLOUD_ADMIN_REPO_ENABLED
  unset NIMBUS_CLOUD_ADMIN_SSH_REQUIRED
  unset NIMBUS_CLOUD_ADMIN_REPO_NODE
  unset NIMBUS_CLOUD_ADMIN_REPO_SSH_USER
  unset NIMBUS_CLOUD_ADMIN_BASE_DIR
  unset NIMBUS_CLOUD_ADMIN_SHARED_USERS
  unset NIMBUS_CLOUD_ADMIN_SHARED_USERNAME
  unset NIMBUS_CLOUD_ADMIN_LINK_FILES
  unset NIMBUS_CLOUD_ADMIN_LINK_FILES_DIR
  unset NIMBUS_CLOUD_ADMIN_SECOND_GRIDMAP_ADD
}

# arg1 = var name, arg2 = value
function record_one_opt() {
  if [ "X" != "X$2" ]; then
    file=$CLOUDADMIN_DECISIONS_FILE
    echo "$1=\"$2\"" >> $file
    echo "export $1" >> $file
    echo "" >> $file
  fi
}

function check_decision_file() {
  
  if [ "X" = "X$CLOUDADMIN_DECISIONS_FILE" ]; then
    echo "Decisions-file path is not set?"
    exit 1
  fi
  
  if [ -f $CLOUDADMIN_DECISIONS_FILE ]; then
    unset_all_global_config_settings
    source $CLOUDADMIN_DECISIONS_FILE
    return
  fi
  
  echo ""
  echo "You do not have a file with one or more cloud configuration decisions at:"
  echo "    $CLOUDADMIN_DECISIONS_FILE"
  echo ""
  echo "This needs to be set up in order to proceed with $1"
  echo ""
  echo "Once it is set up, you will not have to answer the questions again."
  
  get_y_n "Continue?"
  if [ $? -ne 0 ]; then
    echo ""
    echo "Goodbye."
    exit 1
  fi
  
  QUESTION="When you add users on a regular basis, do you want this script to adjust a local grid-mapfile?"
  get_y_n "$QUESTION"
  if [ $? -ne 0 ]; then
    record_one_opt NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD "n"
  else
    record_one_opt NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD "y"
    
    QUESTION="What is the absolute path to that grid-mapfile?"
    get_STRING_ANSWER "$QUESTION"
    record_one_opt NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD_PATH "$STRING_ANSWER"
    
    QUESTION="Will the new DN always map to the same account (typical for nimbus grid-mapfile)?"
    get_y_n "$QUESTION"
    if [ $? -ne 0 ]; then
      record_one_opt NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD_ASKFORUSER "y"
    else
      record_one_opt NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD_ASKFORUSER "n"
      
      QUESTION="What is that account name?"
      get_STRING_ANSWER "$QUESTION"
      record_one_opt NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD_DUPUSERNAME "$STRING_ANSWER"
      
    fi
    
  fi
  
  echo ""
  echo "----------"
  
  QUESTION="When you add users on a regular basis, do you want this script to also create the appropriate directories at the cloud repository?"
  
  get_y_n "$QUESTION"
  if [ $? -ne 0 ]; then
    echo ""
    echo "Alright, then that behavior is disabled."
    echo ""               
    echo "To be asked this question again in the future, delete this file:"
    echo "  '$CLOUDADMIN_DECISIONS_FILE'"
    echo ""
    echo "And run the current script again."
    echo ""
    echo "Hit return to continue."
    read ignored_read
    
    echo "----------"
  
    record_one_opt NIMBUS_CLOUD_ADMIN_REPO_ENABLED "n"
  
  else
  
    record_one_opt NIMBUS_CLOUD_ADMIN_REPO_ENABLED "y"
    
    QUESTION="Is the repository on another node?"
    get_y_n "$QUESTION"
    if [ $? -ne 0 ]; then
      ANSWER="n"
    else
      ANSWER="y"
    fi
    record_one_opt NIMBUS_CLOUD_ADMIN_SSH_REQUIRED "$ANSWER"
    
    if [ "y" = "$ANSWER" ]; then
      
      QUESTION="What is the name of that node?"
      get_STRING_ANSWER "$QUESTION"
      record_one_opt NIMBUS_CLOUD_ADMIN_REPO_NODE "$STRING_ANSWER"
      
      QUESTION="What account will create the directories (target of ssh invocation)?"
      get_STRING_ANSWER "$QUESTION"
      record_one_opt NIMBUS_CLOUD_ADMIN_REPO_SSH_USER "$STRING_ANSWER"
      
    fi
    
    QUESTION="What is the base directory for new user directories? (like \"/cloud\") "
    get_STRING_ANSWER "$QUESTION"
    record_one_opt NIMBUS_CLOUD_ADMIN_BASE_DIR "$STRING_ANSWER"
    
    QUESTION="Does each cloud user have the same UNIX account?"
    get_y_n "$QUESTION"
    if [ $? -ne 0 ]; then
      ANSWER="n"
    else
      ANSWER="y"
    fi
    record_one_opt NIMBUS_CLOUD_ADMIN_SHARED_USERS "$ANSWER"
    
    if [ "y" = "$ANSWER" ]; then
      
      QUESTION="What is the name of the account?"
      get_STRING_ANSWER "$QUESTION"
      record_one_opt NIMBUS_CLOUD_ADMIN_SHARED_USERNAME "$STRING_ANSWER"
      
    fi
    
    QUESTION="Do you want to softlink a set of read-only files into the directory (starter images)?"
    get_y_n "$QUESTION"
    if [ $? -ne 0 ]; then
      ANSWER="n"
    else
      ANSWER="y"
    fi
    record_one_opt NIMBUS_CLOUD_ADMIN_LINK_FILES "$ANSWER"
    
    if [ "y" = "$ANSWER" ]; then
      
      QUESTION="You need to create a directory on the same node that contains the files to softlink new users to.  What is the absolute path of that directory?"
      get_STRING_ANSWER "$QUESTION"
      record_one_opt NIMBUS_CLOUD_ADMIN_LINK_FILES_DIR "$STRING_ANSWER"
      
    fi
  fi
  
  
  # proceed just like the answer intake never happened:
  unset_all_global_config_settings
  source $CLOUDADMIN_DECISIONS_FILE
}

# }}}

# -----------------------------------------------------------------------------
# {{{  ACTION: add-DN
# -----------------------------------------------------------------------------

do_local_gridmap_add() {
  
  if [ "X" = "X$NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD_PATH" ]; then
    echo "Choice was made to add to grid-mapfile but there is no grid-mapfile path defined to add this DN to?"
    return 1
  fi
  
  if [ ! -f "$NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD_PATH" ]; then
    echo "Not a file: '$NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD_PATH'"
    return 1
  fi
  
  USERMAPPING=""
  if [ "y" = "$NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD_ASKFORUSER" ]; then
    QUESTION="What is that account name to map this to?"
    get_STRING_ANSWER "$QUESTION"
    USERMAPPING="$STRING_ANSWER"
  elif [ "X" = "X$NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD_DUPUSERNAME" ] ; then
    echo "user mapping required but not present:"
    echo "The 'NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD_DUPUSERNAME' variable should have been set up in the decisions file if 'NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD_ASKFORUSER' is 'n'"
    return 1
  else
    USERMAPPING="$NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD_DUPUSERNAME"
  fi
  
  $JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_GRIDMAP_ADD $NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD_PATH "$1" "$USERMAPPING"
  return $?
}

do_local_gridmap_del() {
  
  if [ ! -f "$NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD_PATH" ]; then
    echo "Not a file: '$NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD_PATH'"
    return 1
  fi
  
  $JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_GRIDMAP_DEL $NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD_PATH "$1"
  return $?
}

do_repo_add() {
  
  if [ "y" = "$NIMBUS_CLOUD_ADMIN_SSH_REQUIRED" ]; then
  
    if [ "X" = "X$NIMBUS_CLOUD_ADMIN_REPO_NODE" ]; then
      echo "ssh target node is required but not present:"
      echo "The 'NIMBUS_CLOUD_ADMIN_REPO_NODE' variable should have been set up in the decisions file if 'NIMBUS_CLOUD_ADMIN_SSH_REQUIRED' is 'y'"
      return 1
    fi
    
    if [ "X" = "X$NIMBUS_CLOUD_ADMIN_REPO_SSH_USER" ]; then
      echo "ssh target user is required but not present:"
      echo "The 'NIMBUS_CLOUD_ADMIN_REPO_SSH_USER' variable should have been set up in the decisions file if 'NIMBUS_CLOUD_ADMIN_SSH_REQUIRED' is 'y'"
      return 1
    fi
  
    CMD_PREFIX="ssh $NIMWIZ_SSH_BATCH_OPTIONS $NIMBUS_CLOUD_ADMIN_REPO_SSH_USER@$NIMBUS_CLOUD_ADMIN_REPO_NODE "
    
    echo ""
    echo "Testing SSH access: $CMD_PREFIX /bin/true"
    
    $CMD_PREFIX /bin/true
    if [ $? -ne 0 ]; then
      echo "SSH access to repository failed."
      return 1
    fi
    
    echo ""
  else
    CMD_PREFIX=""
  fi
  
  if [ "X" = "X$NIMBUS_CLOUD_ADMIN_BASE_DIR" ]; then
    echo ""
    echo "Exiting, no base directory is configured (variable in decisions file: NIMBUS_CLOUD_ADMIN_BASE_DIR)"
    exit 1
  fi
  
  TEST="$CMD_PREFIX test -d $NIMBUS_CLOUD_ADMIN_BASE_DIR"
  $TEST
  if [ $? -ne 0 ]; then
    echo "Could not locate base directory '$NIMBUS_CLOUD_ADMIN_BASE_DIR'"
    return 1
  fi
  
  echo "Base directory present: '$NIMBUS_CLOUD_ADMIN_BASE_DIR'"
  
  OTHERDIR=`$JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_GET_OTHERCONF_DIR`
  CONF="$OTHERDIR/$ACTIVEAUTHZ_TARGET_CONF_FILE"
  USERHASH=`$JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_GROUPAUTHZ_HARNESS hash $CONF "$1"`
  
  USERDIR="$NIMBUS_CLOUD_ADMIN_BASE_DIR/$USERHASH"
  
  TEST2="$CMD_PREFIX test -d $USERDIR"
  $TEST2
  if [ $? -eq 0 ]; then
    echo ""
    echo "Exiting, user's directory already present: '$USERDIR'"
    return 1
  fi
  
  RUN1="$CMD_PREFIX mkdir $USERDIR"
  $RUN1
  if [ $? -ne 0 ]; then
    echo ""
    echo "Exiting, could not create user's directory: '$USERDIR'"
    return 1
  fi
  
  echo "Created user directory: '$USERDIR'"
  
  CHOWNNAME=""
  
  if [ "y" = "$NIMBUS_CLOUD_ADMIN_SHARED_USERS" ]; then
  
    if [ "X" = "X$NIMBUS_CLOUD_ADMIN_SHARED_USERNAME" ]; then
      echo "shared repository username is required but not present:"
      echo "The 'NIMBUS_CLOUD_ADMIN_SHARED_USERNAME' variable should have been set up in the decisions file if 'NIMBUS_CLOUD_ADMIN_SHARED_USERS' is 'y'"
      return 1
    fi
    
    CHOWNNAME="$NIMBUS_CLOUD_ADMIN_SHARED_USERNAME"
  
  else
  
    QUESTION="What is the name of the account the new directory should be owned by?"
    get_STRING_ANSWER "$QUESTION"
    CHOWNNAME="$STRING_ANSWER"
  
  fi
  
  RUN2="$CMD_PREFIX chown $CHOWNNAME $USERDIR"
  $RUN2
  if [ $? -ne 0 ]; then
    echo ""
    echo "Exiting, could not give proper ownership to user's directory: '$USERDIR'"
    echo ""
    echo "Was trying to run this command:"
    echo ""
    echo "    $RUN2"
    echo ""
    echo "You usually either need to be root on the repository or you need to be the same user as the target user."
    echo ""
    return 1
  fi
  
  if [ "y" = "$NIMBUS_CLOUD_ADMIN_LINK_FILES" ]; then
  
    echo ""
    echo "Soft linking image files to this directory:"
    echo ""
    
    if [ "X" = "X$NIMBUS_CLOUD_ADMIN_LINK_FILES_DIR" ]; then
      echo "directory of files to link in to user's directory is required but not present:"
      echo "The 'NIMBUS_CLOUD_ADMIN_LINK_FILES_DIR' variable should have been set up in the decisions file if 'NIMBUS_CLOUD_ADMIN_LINK_FILES' is 'y'"
      return 1
    fi
    
    TEST3="$CMD_PREFIX test -d $NIMBUS_CLOUD_ADMIN_LINK_FILES_DIR"
    $TEST3
    if [ $? -ne 0 ]; then
      echo "Could not locate directory '$NIMBUS_CLOUD_ADMIN_LINK_FILES_DIR'"
      return 1
    fi
    
    FILES=`$CMD_PREFIX find $NIMBUS_CLOUD_ADMIN_LINK_FILES_DIR -type f -maxdepth 1 -printf "%f\n"`
    if [ $? -ne 0 ]; then
      echo "Problem running image file scan for softlinks"
      return 1
    fi
    
    ONE="n"
    
    for f in $FILES; do
      RUN4="$CMD_PREFIX ln -s $NIMBUS_CLOUD_ADMIN_LINK_FILES_DIR/$f $USERDIR/$f"
      $RUN4
      if [ $? -ne 0 ]; then
        echo "Problem running this: '$RUN4'"
        return 1
      fi
      echo "Soft linked '$f' to $USERDIR/"
      ONE="y"
    done
    
    if [ "n" = "$ONE" ]; then
      echo "WARNING: No files were linked, is the source directory empty? '$NIMBUS_CLOUD_ADMIN_LINK_FILES_DIR'"
    fi
  fi
  
  return 0
}

action_add_query_dn() {
  QUERY_CONF_PATH="$GLOBUS_LOCATION/etc/nimbus/query/query.conf"

  if [ ! -f "$QUERY_CONF_PATH" ]; then
    echo "Not a file: '$QUERY_CONF_PATH'"
    return 1
  fi

  $JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_QUERY_ADD $QUERY_CONF_PATH "$1"
  return $?
}


action_add_dn() {
  check_decision_file "adding a DN."
  echo ""
  OTHERDIR=`$JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_GET_OTHERCONF_DIR`
  CONF="$OTHERDIR/$ACTIVEAUTHZ_TARGET_CONF_FILE"
  $JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_GROUPAUTHZ_HARNESS add $CONF "$1"
  if [ $? -ne 0 ]; then
  
    return 1
    
  else
  
    if [ "Xy" = "X$NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD" ]; then
      echo ""
      echo "---------------------------------------"
      echo ""
      do_local_gridmap_add "$1"
      if [ $? -ne 0 ]; then
        return 1
      fi
    fi
  
    if [ "Xy" = "X$NIMBUS_CLOUD_ADMIN_REPO_ENABLED" ]; then
      echo ""
      echo "---------------------------------------"
      echo ""
      do_repo_add "$1"
      if [ $? -ne 0 ]; then
        return 1
      fi
    fi
  fi
  
  echo ""
  echo "Done."
  
  return 0
}

# }}}

# -----------------------------------------------------------------------------
# {{{  ACTION: del-DN
# -----------------------------------------------------------------------------

action_del_dn() {
  check_decision_file "deleting a DN."
  echo ""
  OTHERDIR=`$JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_GET_OTHERCONF_DIR`
  CONF="$OTHERDIR/$ACTIVEAUTHZ_TARGET_CONF_FILE"
  $JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_GROUPAUTHZ_HARNESS del $CONF "$1"
  if [ $? -ne 0 ]; then
  
    return 1
    
  else
  
    if [ "y" = "$NIMBUS_CLOUD_ADMIN_GRIDMAP_ADD" ]; then
      echo ""
      do_local_gridmap_del "$1"
    fi
    
    OTHERDIR=`$JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_GET_OTHERCONF_DIR`
    CONF="$OTHERDIR/$ACTIVEAUTHZ_TARGET_CONF_FILE"
    USERHASH=`$JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_GROUPAUTHZ_HARNESS hash $CONF "$1"`
    
    USERDIR="$NIMBUS_CLOUD_ADMIN_BASE_DIR/$USERHASH"
    echo ""
    echo "This user's repository directory is: '$USERDIR'"
    echo "(this script does not remove that directory)"
  fi
  
  echo ""
  echo "Done."
  
  return 0
}

# }}}

# -----------------------------------------------------------------------------
# {{{  ACTION: findhash
# -----------------------------------------------------------------------------

action_findhash() {
  OTHERDIR=`$JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_GET_OTHERCONF_DIR`
  CONF="$OTHERDIR/$ACTIVEAUTHZ_TARGET_CONF_FILE"
  $JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_GROUPAUTHZ_HARNESS findhash $CONF "$1"
  return $?
}

# }}}

# -----------------------------------------------------------------------------
# {{{  ACTION: enable group-authz
# -----------------------------------------------------------------------------

action_enable_group_authz() {
  
  echo "Enabling group authorization module."
  
  OTHERDIR=`$JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_GET_OTHERCONF_DIR`
  if [ $? -ne 0 ]; then
    echo "Problem locating the 'other' configuration directory"
    return 1
  fi
  
  if [ ! -f "$OTHERDIR/$GROUPAUTHZ_CONF_FILE" ]; then
    echo "Sorry, cannot find this file to activate the group authz module:"
    echo "  $OTHERDIR/$GROUPAUTHZ_CONF_FILE"
    return 1
  fi
  
  if [ -f "$OTHERDIR/$ACTIVEAUTHZ_TARGET_CONF_FILE" ]; then
    OUTPUT=`diff --brief $OTHERDIR/$ACTIVEAUTHZ_TARGET_CONF_FILE $OTHERDIR/$GROUPAUTHZ_CONF_FILE`
    if [ $? -eq 0 ]; then
      echo ""
      echo "Group authorization module was already enabled."
      return 0
    fi
  fi
  
  CMD="cp $OTHERDIR/$GROUPAUTHZ_CONF_FILE $OTHERDIR/$ACTIVEAUTHZ_TARGET_CONF_FILE"
  
  $CMD
  if [ $? -ne 0 ]; then
    echo "Problem copying file, command was: $CMD"
    return 1
  fi
  
  echo ""
  echo "Enabled.  Container restart is necessary."
  
  return 0
}

# }}}

# -----------------------------------------------------------------------------
# {{{  ACTION: disable group-authz
# -----------------------------------------------------------------------------

action_disable_group_authz() {
  
  echo "Disabling group authorization module."
  
  OTHERDIR=`$JAVA_BIN $NIMWIZ_JAVA_OPTS $EXE_GET_OTHERCONF_DIR`
  if [ $? -ne 0 ]; then
    echo "Problem locating the 'other' configuration directory"
    return 1
  fi
  
  if [ ! -f "$OTHERDIR/$DISABLEDAUTHZ_CONF_FILE" ]; then
    echo "Sorry, cannot find this file to deactivate the group authz module:"
    echo "  $OTHERDIR/$DISABLEDAUTHZ_CONF_FILE"
    return 1
  fi
  
  if [ -f "$OTHERDIR/$ACTIVEAUTHZ_TARGET_CONF_FILE" ]; then
    OUTPUT=`diff --brief $OTHERDIR/$ACTIVEAUTHZ_TARGET_CONF_FILE $OTHERDIR/$DISABLEDAUTHZ_CONF_FILE`
    if [ $? -eq 0 ]; then
      echo ""
      echo "Authorization callout was already disabled."
      return 0
    fi
  fi
  
  CMD="cp $OTHERDIR/$DISABLEDAUTHZ_CONF_FILE $OTHERDIR/$ACTIVEAUTHZ_TARGET_CONF_FILE"
  
  $CMD
  if [ $? -ne 0 ]; then
    echo "Problem copying file, command was: $CMD"
    return 1
  fi
  
  echo ""
  echo "Authorization callout disabled.  Container restart is necessary."
  
  return 0
}

# }}}

# -----------------------------------------------------------------------------
# {{{  decide what to do
# -----------------------------------------------------------------------------

if [ "$1" = "$OPT_AUTHZ_ON" ]; then
  action_enable_group_authz $*
elif [ "$1" = "$OPT_AUTHZ_OFF" ]; then
  action_disable_group_authz $*
elif [ "$1" = "$OPT_ALL_DNS" ]; then
  action_all_dns $*
elif [ "$1" = "$OPT_ADD_DN" ]; then
  if [ "X$3" = "X" ]; then
    action_add_dn "$2"
  else
    # 2+ extras
    echo "Requires just one argument, the DN to add."
    echo "Did you make sure to quote DNs that have spaces?"
    exit 1
  fi
elif [ "$1" = "$OPT_DEL_DN" ]; then
  if [ "X$3" = "X" ]; then
    action_del_dn "$2"
  else
    # 2+ extras
    echo "Requires just one argument, the DN to delete."
    echo "Did you make sure to quote DNs that have spaces?"
    exit 1
  fi
elif [ "$1" = "$OPT_FIND_DN" ]; then
  if [ "X$3" = "X" ]; then
    action_find_dn "$2"
  else
    # 2+ extras
    echo "Requires just one argument, the DN to search for."
    echo "Did you make sure to quote DNs that have spaces?"
    exit 1
  fi
elif [ "$1" = "$OPT_HASH_DN" ]; then
  if [ "X$3" = "X" ]; then
    action_hash_dn "$2"
  else
    # 2+ extras
    echo "Requires just one argument, the DN to hash."
    echo "Did you make sure to quote DNs that have spaces?"
    exit 1
  fi
elif [ "$1" = "$OPT_FINDHASH" ]; then
  if [ "X$3" = "X" ]; then
    action_findhash "$2"
  else
    # 2+ extras
    echo "Requires just one argument, the hash to search for."
    exit 1
  fi
elif [ "$1" = "$OPT_ADD_QUERY_DN" ]; then
  if [ "X$3" = "X" ]; then
    action_add_query_dn "$2"
  else
    # 2+ extras
    echo "Requires just one argument, the DN to add."
    echo "Did you make sure to quote DNs that have spaces?"
    exit 1
  fi
else
  echo "Unknown action: '$1'"
  exit 1
fi
exit $?

# }}}


