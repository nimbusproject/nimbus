#! /bin/bash

if ([ "X$1" == "X--help" ] || [ "X$1" == "X-h" ]); then
    echo "install.sh [<installation directory>]"
    exit 0
fi

installdir=$1
start_dir=`pwd`
source_dir=`dirname $0`
cd $source_dir
p=`which python`

env python virtualenv.py -p $p --no-site-packages $installdir

virtualpython=$installdir/bin/python

echo "Installing all of the python code"
$virtualpython setup.py install
if [ $? -ne 0 ]; then
    echo "Failed to install the python deps"
    exit 1
fi

cp -r $source_dir/etc $installdir
echo "setting up the sqlite database"
python -c "import sqlite3" 2> /dev/null
if [ "X$?" != "X0" ]; then
    echo "Could not import sqlite3 in python"
    exit 1
fi
x=`which sqlite3`
if [ $? -ne 0 ]; then
    echo "Could not find the program sqlite3"
    exit 1
fi

dbf="$installdir/etc/authz.db"
if [ -f "$dbf" ]; then
    echo "WARNING: db already exists"
else
    sqlite3 -batch ${dbf} < $source_dir/etc/acl.sql
fi


host=`hostname -f`
port=8888
mkdir $HOME/.nimbus 2> /dev/null
if [ -e $HOME/.nimbus/cumulus.ini ]; then
    mv -f $HOME/.nimbus/cumulus.ini $HOME/.nimbus/cumulus.ini.backup
fi
outf="$HOME/.nimbus/cumulus.ini"

sed -e "s^@@INSTALLDIR@@^$installdir^g" -e "s^@@HOSTNAME@@^$host^g" -e "s^@@PORT@@^$port^g" -e "s^@@SEC@@^$sec_type^g" $source_dir/etc/cumulus.ini.in > $outf
if [ $? -ne 0 ]; then
    exit 1
fi
sed -e "s^@@HOST_PORT@@^$host:$port^g" $source_dir/etc/dot_s3cfg.in > $installdir/dot_s3cfg
if [ $? -ne 0 ]; then
    exit 1
fi

cp -r $source_dir/tests $installdir
cp -r $source_dir/docs $installdir
sed -e "s^@@INSTALLDIR@@^$installdir^g" -e "s^@@HTTPS@@^True^g" $source_dir/etc/cumulus_test.ini.in > $installdir/tests/cumulus_https.ini
if [ $? -ne 0 ]; then
    echo "problem setting up test suite configuration files"
fi

sed -e "s^@@INSTALLDIR@@^$installdir^g" -e "s^@@HTTPS@@^False^g" $source_dir/etc/cumulus_test.ini.in > $installdir/tests/cumulus.ini
if [ $? -ne 0 ]; then
    echo "problem setting up test suite configuration files"
fi
echo "Make further customizations by editing $outf"

sed -e "s^@@INSTALLDIR@@^$installdir^g" $source_dir/etc/env.sh.in > $installdir/env.sh

mkdir $installdir/log
