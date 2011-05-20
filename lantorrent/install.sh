#!/bin/bash

if [ "X$1" == "X" ]; then
    echo "you must provide a target installation directory"
    exit 1
fi

export LANTORRENT_HOME=$1

pylib=`python -c 'import sys; print sys.prefix'`

echo "Installing into the python libraries into $pylibs..."
echo "Installing wrapper scripts into $LANTORRENT_HOME"

mkdir -p $LANTORRENT_HOME/bin
mkdir -p $LANTORRENT_HOME/etc
dir=`dirname $0`
cd $dir

python setup.py install
if [ $? -ne 0 ]; then
    exit 1
fi

ltrequest="$pylib/bin/ltrequest"
ltdaemon="$pylib/bin/ltdaemon"

sed "s^@PGMNAME@^$ltrequest^" etc/exe.in > $LANTORRENT_HOME/bin/lt-request.sh
sed "s^@PGMNAME@^$ltdaemon^" etc/exe.in > $LANTORRENT_HOME/bin/lt-daemon.sh

chmod 755 $LANTORRENT_HOME/bin/lt-request.sh
chmod 755 $LANTORRENT_HOME/bin/lt-daemon.sh

cp -r tests $LANTORRENT_HOME
cp etc/xinetd.conf.in $LANTORRENT_HOME/tests
cp etc/lantorrent.inet.in $LANTORRENT_HOME/tests
