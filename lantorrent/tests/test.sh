#!/bin/bash

if [ "X$LANTORRENT_HOME" == "X" ]; then
    echo "Please set the env LANTORRENT_HOME"
    exit 1
fi
cd $LANTORRENT_HOME

# most users will not have xinetd in their path even if it is installed
export PATH=/usr/sbin/:$PATH

pidfile=`mktemp`
$LANTORRENT_HOME/tests/make_lt_server.sh 4 $pidfile
xinet_pid=`cat $pidfile`
echo "xinet on $xinet_pid"

$LANTORRENT_HOME/bin/lt-daemon.sh &
ltd_pid=$!

trap "kill $xinet_pid $ltd_pid; sleep 10; kill -9 $xinet_pid $ltd_pid" EXIT
source $LANTORRENT_HOME/tests/ports_env.sh
nosetests tests/xfer_test.py  tests/simple_test.py

