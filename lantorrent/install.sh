#!/bin/bash

if [ "X$1" == "X" ]; then
    echo "please specify a target directory"
    exit 
fi
installdir=$1

dir=`dirname $0`
cd $dir
LANTORRENT_HOME=`pwd`

rm -rf $installdir
mkdir $installdir
cp -r `pwd`/* $installdir
rc=$?
if [ $rc -ne 0 ]; then
    echo "failed to copy over lantorrent"
    exit $rc
fi

cd $installdir
rc=$?
if [ $rc -ne 0 ]; then
    echo "could not change to the installation directory"
    exit $rc
fi

who=`whoami`
sed -e "s/@PORT@/2893/" -e "s/@SERVICENAME@/lantorrent/" -e "s/@WHO@/$who/" -e "s^@LANTORRENT_HOME@^$LANTORRENT_HOME^" etc/lantorrent.inet.in > lantorrent.inet

rm etc/req.db
sqlite3 etc/req.db  < etc/lt.sql
rc=$?
if [ $rc -ne 0 ]; then
    echo "could not create the database"
    exit $rc
fi
exit 0
