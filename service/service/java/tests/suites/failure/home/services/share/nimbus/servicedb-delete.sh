#!/bin/bash

BASEDIR_REL="`dirname $0`"
BASEDIR=`cd $BASEDIR_REL; pwd`

BUILDFILE=$BASEDIR/lib/db-mgmt.xml

ant -q -f $BUILDFILE deleteDB $*
