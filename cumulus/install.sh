#!/bin/bash

if ([ "X$1" == "X--help" ] || [ "X$1" == "X-h" ]); then
    echo "install.sh [<installation directory>]"
    exit 0
fi


start_dir=`pwd`
source_dir=`dirname $0`
cd $source_dir
source_dir=`pwd`
cp_files=""
for i in *
do
    cp_files="$cp_files $source_dir/$i"
done
cd $start_dir
if [ "X$1" == "X" ]; then
    installdir=$source_dir
else
    installdir=$1
    mkdir $installdir 2> /dev/null
    if [ "X$?" != "X0" ]; then
        echo "WARNING: Install directory already exists"
    fi
    cd $installdir
    # just in case they enter the same dir as the source distro
    installdir=`pwd`
    echo "Installing cumulus to $installdir"
        cp -r $cp_files $installdir
        if [ "X$?" != "X0" ]; then
            echo "Copy to $installdir failed"
            echo "verify that you can write to that directory"
            exit 1
        fi
    cd $source_dir
fi

python -c "from twisted.web import server, resource" 2> /dev/null
if [ "X$?" != "X0" ]; then
    echo "ERROR: twisted web not found"
    echo "       cumulus requires twisted.web to be installed"
    echo 1
fi

authz_deps="0"
sec_type="posix"
python -c "import sqlite3" 2> /dev/null
if [ "X$?" != "X0" ]; then
    authz_deps="1"
    echo "Could not import sqlite3 in python"
fi
x=`which sqlite3`
if [ $? -ne 0 ]; then
    authz_deps="1"
    echo "Could not find the program sqlite3"
fi

if [ "X$authz_deps" != "X0" ]; then
    echo "ERROR: Did not find sqlite3 thus we cannot use pynimbusauthz"
    echo "       using basic file security instead"
    echo "       this is ok for testing and small install"

    exit 1
fi

sec_type="authz"

dbf="$installdir/etc/authz.db"
if [ -f "$dbf" ]; then
    echo "WARNING: db already exists"
else
    sqlite3 -batch ${dbf} < $installdir/etc/acl.sql
fi


host=`hostname -f`
me=`id -u`
if [ "X$me" == "X0" ]; then
    port=80
    mkdir /etc/nimbus 2> /dev/null
    if [ -e /etc/nimbus/cumulus.ini ]; then
        mv -f /etc/nimbus/cumulus.ini /etc/nimbus/cumulus.ini.backup
    fi
    outf="/etc/nimbus/cumulus.ini"
else
    port=8888
    mkdir $HOME/.nimbus 2> /dev/null
    if [ -e $HOME/.nimbus/cumulus.ini ]; then
        mv -f $HOME/.nimbus/cumulus.ini $HOME/.nimbus/cumulus.ini.backup
    fi
    outf="$HOME/.nimbus/cumulus.ini"
fi

sed -e "s^@@INSTALLDIR@@^$installdir^g" $installdir/etc/env.sh.in > $installdir/env.sh
if [ $? -ne 0 ]; then
    exit 1
fi
sed -e "s^@@INSTALLDIR@@^$installdir^g" -e "s^@@HOSTNAME@@^$host^g" -e "s^@@PORT@@^$port^g" -e "s^@@SEC@@^$sec_type^g" $installdir/etc/cumulus.ini.in > $outf
if [ $? -ne 0 ]; then
    exit 1
fi
sed -e "s^@@HOST_PORT@@^$host:$port^g" $installdir/etc/dot_s3cfg.in > $installdir/dot_s3cfg
if [ $? -ne 0 ]; then
    exit 1
fi

sed -e "s^@@INSTALLDIR@@^$installdir^g" -e "s^@@HTTPS@@^True^g" $installdir/etc/cumulus_test.ini.in > $installdir/tests/cumulus_https.ini
if [ $? -ne 0 ]; then
    echo "problem setting up test suite configuration files"
fi

sed -e "s^@@INSTALLDIR@@^$installdir^g" -e "s^@@HTTPS@@^False^g" $installdir/etc/cumulus_test.ini.in > $installdir/tests/cumulus.ini
if [ $? -ne 0 ]; then
    echo "problem setting up test suite configuration files"
fi

echo "Make further customizations by editing $outf"

