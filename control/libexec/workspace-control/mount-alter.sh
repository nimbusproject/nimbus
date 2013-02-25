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

# This script is called via sudo to mount a workspace image, alter the
# filesystem and un-mount it.  This makes it possible to not open up the
# mount/umount commands to the globus user entirely via sudo.
# Use sudo version greater or equal to 1.6.8p2
# See: http://www.gratisoft.us/sudo/alerts/bash_functions.html

# This script must be owned and writable only by root and placed in a non-
# tamperable directory.

# See the "adjust as necessary" section below.


########
# SUDO #
########

IFS=' '

\unalias -a

set -f -u -C -p -P

# set:
#
# -f  
#     Disable file name generation (globbing).
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

PATH=/bin
SHELL=/bin/bash
export PATH SHELL

unset -f strlen
function strlen (){
    eval echo "\${#${1}}"
}

MOUNT="/bin/mount"
UMOUNT="/bin/umount"
CP="/bin/cp"
MKDIR="/bin/mkdir"
CHMOD="/bin/chmod"
MODPROBE="/sbin/modprobe"
EXPR="/usr/bin/expr"
PKILL="/usr/bin/pkill"

# qemu-nbd is used when an HD image is in qcow2 format.
# It will be used to attach the HD image as an nbd device, which allows to
# mount the root partition inside.
#
# Make sure QEMU_NBD is the full path to the qemu-nbd executable, and
# QEMU_NBD_NAME matches its process name (usually qemu-nbd).
QEMU_NBD="/usr/bin/qemu-nbd"
QEMU_NBD_NAME="qemu-nbd"

FLOCKFILE=/opt/nimbus/var/workspace-control/lock/loopback.lock
FLOCK=/usr/bin/flock
if [ ! -O $FLOCK ]; then
  echo "*** can not find flock program, disabling"
  echo "*** disabling flock might result in a error like \"kernel doesn't support a certain ebtables extension\""
  FLOCK=/bin/true
fi


#######################
# ADJUST AS NECESSARY #
#######################

DRYRUN="false"  # or "true"

# If CREATE_SSH_DIR is set to true (default), mount-alter will create the
# /root/.ssh directory before copying the authorized_keys file. This allows to
# make propagation succeed with VM images which do not have a /root/.ssh
# directory (as it is often the case when no SSH key has ever been installed).
#
# If set to false, this script will not try to create the /root/.ssh directory.
CREATE_SSH_DIR="true" # or "false"

# Only requests to mount files UNDER this directory are honored.
# You must use absolute path and include trailing slash.
IMAGE_DIR=/opt/nimbus/var/workspace-control/secureimages/

# Only requests to mount files TO this directory are honored.
# You must use absolute path and include trailing slash.
MOUNTPOINT_DIR=/opt/nimbus/var/workspace-control/mnt/

# Only requests to copy over files UNDER this directory are honored.
# You must use absolute path and include trailing slash.
FILE_DIR=/opt/nimbus/var/workspace-control/tmp/


#############
# ARGUMENTS #
#############

# TODO: for now this only implements subcommand called 'one' for one
# file/target pair add more ability later


if [ $# -lt 5 ]; then
  echo "requires at least 5 arguments: <subcommand> <imagefile> <mntpoint> <datafile> <datatarget> [extra,]"
  exit 1
fi

echo "$0 received these arguments: "
subcommand=$1
echo "  - subcommand: $subcommand"
imagefile=$2
echo "  - imagefile: $imagefile"
mountpoint=$3
echo "  - mountpoint: $mountpoint"
datafile=$4
datatarget=$5

if [ "$subcommand" = "config" ]; then
  subcommand="CONFIG"
  echo "config subcommand is DEPRECATED, exiting"
  exit 1
elif [ "$subcommand" = "cert" ]; then
  subcommand="CERT"
  echo "cert subcommand is DEPRECATED, exiting"
  exit 1
elif [ "$subcommand" = "one" ]; then
  subcommand="ONE"
elif [ "$subcommand" = "hdone" ]; then
  subcommand="HDONE"
elif [ "$subcommand" = "qcowone" ]; then
  subcommand="QCOWONE"
else
  echo "invalid subcommand"
  exit 1
fi

if [ "$subcommand" = "ONE" ]; then
  if [ $# -ne 5 ]; then
    echo "one subcommand requires 5 and only 5 arguments: one <imagefile> <mntpoint> <datafile> <datatarget>"
    exit 1
  fi
  echo "  - datafile: $datafile"
  echo "  - datatarget: $datatarget"

  sourcefiles="$datafile"
  targetfiles="$datatarget"
  
elif [ "$subcommand" = "HDONE" ]; then
  if [ $# -ne 6 ]; then
    echo "hdone subcommand requires 6 and only 6 arguments: hdone <imagefile> <mntpoint> <datafile> <datatarget> <offset>"
    exit 1
  fi
  offset=$6
  echo "  - datafile: $datafile"
  echo "  - datatarget: $datatarget"
  echo "  - offset: $offset"
  
  offsetint=$(($offset * 1))
  if [ $? -ne 0 ]; then
    echo "not an integer: $offset"
    exit 2
  fi
  
  if [ $offsetint -le 0 ]; then
    echo "expecting offset to be greater than zero: $offsetint"
    exit 2
  fi

  sourcefiles="$datafile"
  targetfiles="$datatarget"
  
elif [ "$subcommand" = "QCOWONE" ]; then
  if [ $# -ne 5 ]; then
    echo "qcowone subcommand requires 6 and only 6 arguments: qcowone <imagefile> <mntpoint> <datafile> <datatarget>"
    exit 1
  fi
  echo "  - datafile: $datafile"
  echo "  - datatarget: $datatarget"

  sourcefiles="$datafile"
  targetfiles="$datatarget"

else
  echo "??"
  exit 64
fi

#############
# IMAGEFILE #
#############

echo ""
echo "Checking input:"

echo "  - IMAGE_DIR: $IMAGE_DIR"

arglen0=`strlen imagefile`
imagefile=`echo $imagefile | sed -e 's/[^/\.a-zA-Z0-9%_-]//g'`
arglen=`strlen imagefile`

if [ $arglen0 -ne $arglen ]; then
  echo "given imagefile is invalid (char-violation): use absolute path to image under authorized image directory"
  exit 2
fi

dirlen=`strlen IMAGE_DIR`

if [ $arglen -le $dirlen ]; then
  echo "given imagefile is invalid (len): use absolute path to image under authorized image directory"
  exit 3
fi

if [ "${imagefile:0:dirlen}" != "$IMAGE_DIR" ]; then
  echo "given imagefile is invalid (=): use absolute path to image under authorized image directory"
  exit 4
fi

echo "           OK: $imagefile"

##############
# MOUNTPOINT #
##############

echo "  - MOUNTPOINT_DIR: $MOUNTPOINT_DIR"

arglen0=`strlen mountpoint`
mountpoint=`echo $mountpoint | sed -e 's/[^/\.a-zA-Z0-9_-]//g'`
arglen=`strlen mountpoint`

if [ $arglen0 -ne $arglen ]; then
  echo "given mountpoint is invalid (char-violation): use absolute path to mount point under authorized mountpoint directory"
  exit 2
fi

dirlen=`strlen MOUNTPOINT_DIR`

if [ $arglen -le $dirlen ]; then
  echo "given mountpoint is invalid (len): use absolute path to mount point under authorized mountpoint directory"
  exit 3
fi

if [ "${mountpoint:0:dirlen}" != "$MOUNTPOINT_DIR" ]; then
  echo "given mountpoint is invalid (=): use absolute path to mount point under authorized mountpoint directory"
  exit 4
fi

echo "                OK: $mountpoint"

################
# SOURCE FILES #
################

echo "  - FILE_DIR: $FILE_DIR"

for x in $sourcefiles
do
  arglen0=`strlen x`
  y=`echo $x | sed -e 's/[^/\.a-zA-Z0-9_-]//g'`
  arglen=`strlen y`
  
  if [ $arglen0 -ne $arglen ]; then
    echo "given datafile $x is invalid (char-violation): use absolute path to file under authorized file directory"
    exit 2
  fi

  dirlen=`strlen FILE_DIR`
  
  if [ $arglen -le $dirlen ]; then
    echo "given datafile $y is invalid (len): use absolute path to file under authorized file directory"
    exit 3
  fi
  
  if [ "${y:0:dirlen}" != "$FILE_DIR" ]; then
    echo "given datafile $y is invalid (=): use absolute path to file under authorized file directory"
    exit 4
  fi
  
  echo "          OK: $y"
done

################
# TARGET FILES #
################

for x in $targetfiles
do
  arglen0=`strlen x`
  y=`echo $x | sed -e 's/[^/\.a-zA-Z0-9_-]//g'`
  arglen=`strlen y`
  
  if [ $arglen0 -ne $arglen ]; then
    echo "given target file $x is invalid (char-violation)"
    exit 2
  fi
  echo "  - target OK: $y"
done

###############
# ALTER IMAGE #
###############

function qemu_nbd_disconnect () {
  cmd="$QEMU_NBD -d /dev/nbd0"

  echo "command = $cmd"
  if [ "$DRYRUN" != "true" ]; then
    ( $cmd )
    if [ $? -ne 0 ]; then
      # nbd will print to stderr
      problem="true"
    else
      echo "  - successful"
    fi
  fi

  # Sometimes qemu-nbd process get stuck and a disconnect is not enough.
  # So we kill them.
  cmd="$PKILL -9 $QEMU_NBD_NAME"

  echo "command = $cmd"
  if [ "$DRYRUN" != "true" ]; then
    ( $cmd )
    if [ $? -eq 0 ]; then
      # One or more processes matched the criteria:
      # let's log some info
      echo "  - successful"
      echo ""
      echo "!!! We killed some (stuck?) $QEMU_NBD_NAME processes !!!"
      echo ""
    elif [ $? -eq 1 ]; then
      echo "  - successful (no process killed)"
    else
      problem="true"
      echo "  - failed"
    fi
  fi

}

(
$FLOCK -x 200

echo ""
echo "Altering image (dryrun = $DRYRUN):"
echo ""

problem="false"

if [ "$subcommand" = "QCOWONE" ]; then
  cmd="$MODPROBE nbd max_part=8"
  echo "command = $cmd"
  if [ "$DRYRUN" != "true" ]; then
    ( $cmd )
    if [ $? -ne 0 ]; then
      # xm will print to stderr
      exit 5
    else
      echo "  - successful"
    fi
  fi

  cmd="$QEMU_NBD --connect /dev/nbd0 $imagefile"

  echo "command = $cmd"
  if [ "$DRYRUN" != "true" ]; then
    (
      # Close the lock file descriptor in this subshell, otherwise it stays
      # locked in the qemu-nbd daemonized process.  If qemu-nbd hangs, further
      # mount-alter won't be able to flock.
      exec 200>&-
      $cmd
    )

    if [ $? -ne 0 ]; then
      # xm will print to stderr
      exit 6
    else
      echo "  - successful"
    fi
  fi

  # qemu-nbd takes some time to connect the image to the device.
  # Wait for maximum 30 seconds for nbd0p1 to show up.
  i=30
  while [ $i -ne 0 ]; do
    ls /dev/nbd0p1 > /dev/null 2>&1 && break
    i=`$EXPR $i - 1`
    echo "No /dev/nbd0p1 yet, will try again for $i seconds"
    sleep 1
  done
  if ! ls /dev/nbd0p1 > /dev/null 2>&1; then
    echo "Waited 30 seconds but no /dev/nbd0p1 showed up, exiting"
    qemu_nbd_disconnect
    exit 66
  fi
fi

if [ "$subcommand" = "ONE" ]; then
  cmd="$MOUNT -o loop,noexec,nosuid,nodev,noatime,sync $imagefile $mountpoint"
elif [ "$subcommand" = "HDONE" ]; then
  cmd="$MOUNT -o loop,noexec,nosuid,nodev,noatime,sync,offset=$offsetint $imagefile $mountpoint"
elif [ "$subcommand" = "QCOWONE" ]; then
  cmd="$MOUNT /dev/nbd0p1 $mountpoint"
else
  echo "??"
  exit 65
fi

echo "command = $cmd"
if [ "$DRYRUN" != "true" ]; then
  ( $cmd )
  if [ $? -ne 0 ]; then
    # mount will print to stderr
    if [ "$subcommand" = "QCOWONE" ]; then
      # disconnect the nbd device
      qemu_nbd_disconnect
    fi
    exit 5
  else
    echo "  - successful"
  fi
fi

if [ "$CREATE_SSH_DIR" == "true" -a "$datatarget" == "/root/.ssh/authorized_keys" -a -d "$mountpoint/root" ]; then
  cmd="$MKDIR -p $mountpoint/root/.ssh"
  echo "command = $cmd"
  if [ "$DRYRUN" != "true" ]; then
    ( $cmd )
    if [ $? -eq 0 ]; then
      echo "  - successful"
    else
      problem="true"
    fi
  fi

  cmd="$CHMOD 700 $mountpoint/root/.ssh"
  echo "command = $cmd"
  if [ "$DRYRUN" != "true" ]; then
    ( $cmd )
    if [ $? -eq 0 ]; then
      echo "  - successful"
    else
      problem="true"
    fi
  fi
fi

if [ "$subcommand" = "ONE" ]; then
  cmd="$CP $datafile $mountpoint/$datatarget"
elif [ "$subcommand" = "HDONE" ]; then
  cmd="$CP $datafile $mountpoint/$datatarget"
elif [ "$subcommand" = "QCOWONE" ]; then
  cmd="$CP $datafile $mountpoint/$datatarget"
else
  echo "??"
  problem="true"
fi

echo "command = $cmd"
if [ "$DRYRUN" != "true" ]; then
  ( $cmd )
  if [ $? -eq 0 ]; then
    echo "  - successful"
  else
    problem="true"
  fi
fi

cmd="$UMOUNT -d -f $mountpoint"

echo "command = $cmd"
if [ "$DRYRUN" != "true" ]; then
  ( $cmd )
  if [ $? -ne 0 ]; then
    # umount will print to stderr
    problem="true"
  else
    echo "  - successful"
  fi
fi

if [ "$subcommand" = "QCOWONE" ]; then
  qemu_nbd_disconnect
fi

# notify that one or more command invocations did not succeed:
if [ "$problem" = "true" ]; then
  exit 7
fi

) 200<$FLOCKFILE
