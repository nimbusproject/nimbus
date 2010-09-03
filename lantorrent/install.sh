#!/bin/bash

if [ "X$1" == "X-h" ]; then
    echo "<install dir>"
    exit 0
fi
if [ "X$1" == "X" ]; then
    echo "please specify a target directory"
    exit 1
fi

echo "---------------------"
echo "installing lantorrent"
echo "---------------------"

installdir=$1
cd $installdir
installdir=`pwd`

dir=`dirname $0`
cd $dir
src_dir=`pwd`

mkdir $installdir
cp -r $src_dir/* $installdir/
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

rm etc/req.db
sqlite3 etc/req.db  < etc/lt.sql
rc=$?
if [ $rc -ne 0 ]; then
    echo "could not create the database"
    exit $rc
fi
exit 0
