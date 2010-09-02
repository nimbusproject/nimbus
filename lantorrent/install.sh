#!/bin/bash

if [ "X$NIMBUS_HOME" == "X" ]; then
    echo "NIMBUS_HOME is not set."
    echo "This installation program is only used for the head node."
    echo "It is not used on the VMMs and it is typically only called from the main installer"
    exit 
fi

dir=`dirname $0`
cd $dir
cp -r `pwd` $NIMBUS_HOME/
cd $NIMBUS_HOME/lantorrent
sqlite3 etc/req.db  < etc/lt.sql
exit $?
