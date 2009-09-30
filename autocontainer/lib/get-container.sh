#!/bin/bash

# ============================== [ FUNCTIONS ] ================================

# arg1=source, arg2=dest
function retrieve_tarball() {
  wget_retrieve_tarball $1 $2
}

# arg1=source, arg2=dest
function wget_retrieve_tarball() {
  wget -c -O $2 $1
}

# arg1=path arg2=destdir
function expand_tarball() {
  tar xzf $1 -C $2
}

# =============================== [ SCRIPT ] ==================================

THISDIR="`dirname $0`"
if [ ! -f $THISDIR/common-env.sh ]; then
  echo "Failure, cannot find environment definitions"
  exit 1
fi
source $THISDIR/common-env.sh

# do not proceed if directory exists
if [ -e $GLOBUS_LOCATION ]; then
  echo "Failure, target GLOBUS_LOCATION directory exists: $GLOBUS_LOCATION"
  exit 1
fi

if [ ! -d $TARGETDIR ]; then
  mkdir $TARGETDIR
  if [ $? -ne 0 ]; then
    echo "Failure, could not create directory: $TARGETDIR"
    exit 1
  fi
fi

if [ ! -d $BASEDIR/$DOWNLOADS_RELDIR ]; then
  mkdir $BASEDIR/$DOWNLOADS_RELDIR
  if [ $? -ne 0 ]; then
    echo "Failure, could not create directory: $BASEDIR/$DOWNLOADS_RELDIR"
    exit 1
  fi
fi


# do not download if tarball already exists
if [ ! -f $TARBALL_DEST ]; then
  retrieve_tarball $CONTAINER_URL $TARBALL_DEST
  if [ $? -ne 0 ]; then
    echo "Failure, could not retrieve tarball: $CONTAINER_URL"
    exit 1
  fi
fi

expand_tarball $TARBALL_DEST $TARGETDIR

if [ ! -e $GLOBUS_LOCATION ]; then
  echo "Failure, target GLOBUS_LOCATION directory does not exist? $GLOBUS_LOCATION"
  exit 1
fi

echo "Expanded to $TARGETDIR"




