#!/bin/bash

if [ "X$LANTORRENT_HOME" == "X" ]; then
    echo "Please set the env LANTORRENT_HOME"
    exit 1
fi

count=$1
if [ "X$1" == "X" ]; then
    count=1
fi
pidfile=$2


who=`whoami`
delim=""
ports_str=""
rm -f $LANTORRENT_HOME/tests/xinetd.d/*
for i in `seq 1 $count`
do

    PORT=$RANDOM
    while [ $PORT -lt 2048 ];
    do
        PORT=$RANDOM
        x=`netstat -l --tcp --numeric-ports | grep $PORT`
        if [ "X$x" != "X" ]; then
            $PORT=0
        fi
    done

    SERVNAME="lantorrent$PORT"

    ports_str="$ports_str$delim$PORT"
    delim=","
    echo "s/@PORT@/$PORT/"
    echo "s/@SERVICENAME@/$SERVNAME/"
    echo "s^@LANTORRENT_HOME@^$LANTORRENT_HOME^" 
    sed -e "s/@WHO@/$who/" -e "s/@PORT@/$PORT/" -e "s/@SERVICENAME@/$SERVNAME/" -e "s^@LANTORRENT_HOME@^$LANTORRENT_HOME^" -e "s^@SERVER@^$LANTORRENT_HOME/bin/lt-daemon.sh^" $LANTORRENT_HOME/tests/lantorrent.inet.in | tee $LANTORRENT_HOME/tests/xinetd.d/$SERVNAME

done

echo "export LANTORRENT_TEST_PORTS=$ports_str" > $LANTORRENT_HOME/tests/ports_env.sh

ls -l $LANTORRENT_HOME/tests/xinetd.d/
echo "s^@LANTORRENT_HOME@^$LANTORRENT_HOME^"
sed "s^@LANTORRENT_HOME@^$LANTORRENT_HOME^" $LANTORRENT_HOME/tests/xinetd.conf.in | tee $LANTORRENT_HOME/tests/xinetd.conf


pidfile=$2
cmd="xinetd -f $LANTORRENT_HOME/tests/xinetd.conf -pidfile $pidfile -filelog x.log -dontfork"
echo $cmd
$cmd &
