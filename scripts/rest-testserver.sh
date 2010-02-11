#!/bin/sh

BASEDIR_REL="`dirname $0`/.."
BASEDIR=`cd $BASEDIR_REL; pwd`

cd "$BASEDIR/messaging/rest/java/source/"
ant testserver
