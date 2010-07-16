#!/bin/bash

# Copyright 1999-2010 University of Chicago
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy
# of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

# For more information see: http://www.nimbusproject.org


#########
# ABOUT #
#########

# This script must be owned and writable only by root and placed in a non-
# tamperable directory.

# See the "adjust as necessary" section below.


########
# SUDO #
########

IFS=' '

\unalias -a

set -f -e -u -C -p -P

# set:
#
# -f  
#     Disable file name generation (globbing).
# -e  
#     Exit immediately if a simple command (see section Simple Commands)
#     exits with a non-zero status, unless the command that fails is part
#     of an until or while loop, part of an if statement, part of a && or
#     || list, or if the command's return status is being inverted using !
# -u  
#     Treat unset variables as an error when performing parameter expansion.
#     An error message will be written to the standard error, and a
#     non-interactive shell will exit.
# -C  
#     Prevent output redirection using `>', `>&', and `<>' from overwriting
#     existing files.
# -p
#     Turn on privileged mode. In this mode, the $BASH_ENV and $ENV files are
#     not processed, shell functions are not inherited from the environment,
#     and the SHELLOPTS variable, if it appears in the environment, is ignored.
#     If the shell is started with the effective user (group) id not equal to
#     the real user (group) id, and the -p option is not supplied, these
#     actions are taken and the effective user id is set to the real user id.
#     If the -p option is supplied at startup, the effective user id is not
#     reset. Turning this option off causes the effective user and group ids
#     to be set to the real user and group ids.
# -P
#     If set, do not follow symbolic links when performing commands such as cd
#     which change the current directory. The physical directory is used
#     instead. By default, Bash follows the logical chain of directories
#     when performing commands which change the current directory.

PATH="/bin:/sbin:/usr/bin:/usr/local/bin"
SHELL=/bin/bash
export PATH SHELL

unset -f strlen
function strlen (){
    eval echo "\${#${1}}"
}


#############
# FIND IMPL #
#############

# If DIRNAME is not available and commented out, you must adjust these
# files to their absolute paths. By default, $DIRNAME is used to find
# the TMP_LEASE_ALTER script, the assumption being that it is in the same
# directory as this script (tmp-lease.sh).
TMP_LEASE_ALTER="tmp-lease-alter.py"

DIRNAME="dirname"

function die_dirname() {
  echo "ERROR: DIRNAME invocation failed.  Suggestion: use hardcoded"
  echo "       path to $TMP_LEASE_ALTER"
  exit 1
}

if [ "X$DIRNAME" != "X" ]; then
  # get the current directory of this script (tmp-lease.sh)
  curdir=`$DIRNAME $0` || die_dirname

  TMP_LEASE_ALTER=$curdir/$TMP_LEASE_ALTER
fi

if [ ! -f $TMP_LEASE_ALTER ]; then
  echo "Cannot find tmp lease implementation: $TMP_LEASE_ALTER"
  exit 1
fi

TMP_LEASE_ALTER_CMD="python $TMP_LEASE_ALTER"

#echo "TMP_LEASE_ALTER_CMD: $TMP_LEASE_ALTER_CMD"

unset TMP_LEASE_ALTER
unset DIRNAME
unset curdir

# typical way to inject
unset PYTHONPATH
unset PYTHONINSPECT


#############
# ARGUMENTS #
#############

if [ "$1" = "print" ]; then
    $TMP_LEASE_ALTER_CMD --print
    exit 0
fi

if [ $# -ne 2 ]; then
  echo "Syntax: add|rem <vmname>   (or just 'print' for debugging)"
  exit 1
fi

ADDREM=$1
VMNAME=$2

if [ "$ADDREM" != "add" ] && [ "$ADDREM" != "rem" ]; then
  echo "ERROR: subcommand must be 'add' or 'rem'"
  exit 1
fi

if [ "$ADDREM" = "add" ]; then
    $TMP_LEASE_ALTER_CMD --add $2
fi

if [ "$ADDREM" = "rem" ]; then
    $TMP_LEASE_ALTER_CMD --remove $2
fi

