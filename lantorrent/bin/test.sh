#!/bin/bash

dir=`dirname $0`
cd $dir/..
pypath=`pwd`
if [ "X${PYTHONPATH}" == "X" ]; then
    export PYTHONPATH=$pypath
else
    export PYTHONPATH=$pypath:${PYTHONPATH}
fi
# so that we pick up the ini file
export LANTORRENT_HOME=$pypath

pidfile=`mktemp`
$LANTORRENT_HOME/bin/make_lt_server.sh 4 $pidfile
xinet_pid=`cat $pidfile`
echo "xinet on $xinet_pid"

trap "kill $xinet_pid; sleep 10; kill -9 $xinet_pid" EXIT
source $LANTORRENT_HOME/tests/ports_env.sh
nosetests tests/*_test.py
