#!/bin/bash

if [ "X$NIMBUS_HOME" == "X" ]; then
    echo "NIMBUS_HOME is not set."
    echo "This installation program is only used for the head node."
    echo "It is not used on the VMMs and it is typically only called from the main installer"
    exit 
fi

dir=`dirname $0`
cd $dir
LANTORRENT_HOME=`pwd`
cp -r `pwd` $NIMBUS_HOME/
cd $NIMBUS_HOME/lantorrent

who=`whoami`
sed -e "s/@PORT@/2893/" -e "s/@SERVICENAME@/lantorrent/" -e "s/@WHO@/$who/" -e "s^@LANTORRENT_HOME@/$LANTORRENT_HOME/" etc/lantorrent.inet.in > lantorrent.inet

sqlite3 etc/req.db  < etc/lt.sql
exit $?
