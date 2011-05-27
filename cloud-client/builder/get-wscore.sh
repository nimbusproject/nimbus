#!/bin/bash -x

# ============================== [ FUNCTIONS ] ================================

# arg1=source, arg2=dest
function retrieve_tarball() {
  wget_retrieve_tarball $1 $2
}

# arg1=source, arg2=dest
function wget_retrieve_tarball() {
  wget --no-check-certificate -c -O $2 $1
}


# =============================== [ SCRIPT ] ==================================

THISDIR="`dirname $0`"
if [ ! -f $THISDIR/environment.sh ]; then
  echo "Failure, cannot find environment definitions"
  exit 1
fi
source $THISDIR/environment.sh

# do not download if tarball already exists
if [ ! -f $CLCLBUILDER_TARBALL_DEST ]; then
  retrieve_tarball $CLCLBUILDER_WSCORE_URL $CLCLBUILDER_TARBALL_DEST
  if [ $? -ne 0 ]; then
    echo "Failure, could not retrieve wscore tarball: $CLCLBUILDER_WSCORE_URL"
    exit 1
  fi
fi

if [ ! -f $CLCLBUILDER_TARBALL_DEST ]; then
  echo "Retrieved wscore, but file is not there?"
  exit 1
fi
