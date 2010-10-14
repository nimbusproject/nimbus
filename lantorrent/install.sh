#!/bin/bash

if [ "X$1" == "X-h" ]; then
    echo "<install dir>"
    exit 0
fi
if [ "X$1" == "X" ]; then
    echo "please specify a target directory"
    exit 1
fi

echo "-----------------------------------------------------------------"
echo " Installing LANTorrent"
echo "-----------------------------------------------------------------"

src_dir=`dirname $0`
cd $src_dir
rc=$?
if [ $rc -ne 0 ]; then
    echo "could not change directories"
    exit $rc
fi
src_dir=`pwd`

installdir=$1
mkdir $installdir
cd $installdir
installdir=`pwd`


echo ""
echo "Copying from $src_dir to $installdir"
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

rm -f etc/req.db
sqlite3 etc/req.db  < etc/lt.sql
rc=$?
if [ $rc -ne 0 ]; then
    echo "could not create the database"
    exit $rc
fi
echo "Success."
exit 0
