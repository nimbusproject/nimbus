#! /bin/bash

if ([ "X$1" == "X--help" ] || [ "X$1" == "X-h" ]); then
    echo "install.sh [<installation directory>]"
    exit 0
fi

installdir=$1
start_dir=`pwd`
source_dir=`dirname $0`
cd $source_dir
cd ..
p=`which python`

env python virtualenv.py -p $p --no-site-packages $installdir

virtualpython=$installdir/bin/python
$virtualpython setup.py install
