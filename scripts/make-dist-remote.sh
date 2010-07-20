#!/bin/bash

# You can call this with the following environment variables to override,
# make a script elsewhere to wrap this so you don't always need to edit:

# MAKE_DIST_REMOTE_BUILDDIR
# MAKE_DIST_REMOTE_WEBDIR
# MAKE_DIST_REMOTE_USERNAME
# MAKE_DIST_REMOTE_HOSTNAME
# MAKE_DIST_SSHKEY
# MAKE_DIST_PRINT_URL_BASE


REMOTE_BUILDDIR=${MAKE_DIST_REMOTE_BUILDDIR-/scratch/nimbus}
REMOTE_WEBDIR=${MAKE_DIST_REMOTE_WEBDIR-/mcs/ee.mcs.anl.gov/nimbus/downloads}
USERNAME=${MAKE_DIST_REMOTE_USERNAME-XXX}
HOSTNAME=${MAKE_DIST_REMOTE_HOSTNAME-login.mcs.anl.gov}
PRINT_URL_BASE=${MAKE_DIST_PRINT_URL_BASE-http://www.nimbusproject.org/downloads/}

if [ "X" == "X$MAKE_DIST_SSHKEY" ]; then
    FABSSHKEY=""
else
    FABSSHKEY="-i $MAKE_DIST_SSHKEY"
fi


EXPLANATION="

This is a script that directs the generation of tarballs on a remote server
and the remote server also hosts it on a web server.

You need to have fab installed locally (not remotely).  The remote build
directory need to be prepopulated with some checkout of the Nimbus git
repository (not the most recent one, just something).

It will pull to the latest in a tracked branch (so you can give 'HEAD' as
the argument to this script to just use the latest branch tip which is the
normal usage) but you can provide a different refspec.

The result will print, like:

http://www.nimbusproject.org/downloads/2010-07-08_2024_4004a4d4/nimbus-2.5RC1.9-src.tar.gz
http://www.nimbusproject.org/downloads/2010-07-08_2024_4004a4d4/nimbus-controls-2.5RC1.9.tar.gz


Everything besides the git <refspec> is defined at the top of the script.

Or you can override those setting with the environment variables described.
"

if ([ "X$1" == "X--help" ] || [ "X$1" == "X-h" ]); then
    echo "Usage:"
    echo "$0 <refspec>"
    echo "$EXPLANATION"
    exit 0
fi

if [ $# -ne 1 ]; then
    echo "Usage:"
    echo "$0 <refspec>"
    echo "$EXPLANATION"
    exit 1
fi

NH_REL="`dirname $0`/.."
NH=`cd $NH_REL; pwd`
FAB_SCRIPT=$NH/scripts/lib/web_ball.py

fab -f $FAB_SCRIPT $FABSSHKEY -u $USERNAME -H $HOSTNAME newball:builddir=$REMOTE_BUILDDIR,webdir=$REMOTE_WEBDIR,printbase=$PRINT_URL_BASE,gitref=$1

