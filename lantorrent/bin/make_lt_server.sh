#!/bin/bash

count=$1
if [ "X$1" == "X" ]; then
    count=1
fi

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


rm -f $LANTORRENT_HOME/tests/xinetd.d/*
for i in `seq 1 $count`
do

    PORT=$RANDOM
    while [ $PORT -lt 2048 ];
    do
        $PORT=$RANDOM
        x=`netstat -l --tcp --numeric-ports | grep $PORT`
        if [ "X$x" != "X" ]; then
            $PORT=0
        fi
    done

    SERVNAME="lantorrent$PORT"

    echo "s/@PORT@/$PORT/"
    echo "s/@SERVICENAME@/$SERVNAME/"
    echo "s^@LANTORRENT_HOME@^$LANTORRENT_HOME^" 
    sed -e "s/@PORT@/$PORT/" -e "s/@SERVICENAME@/$SERVNAME/" -e "s^@LANTORRENT_HOME@^$LANTORRENT_HOME^" $LANTORRENT_HOME/etc/lantorrent.inet.in | tee $LANTORRENT_HOME/tests/xinetd.d/$SERVNAME

done

ls -l $LANTORRENT_HOME/tests/xinetd.d/
echo "s^@LANTORRENT_HOME@^$LANTORRENT_HOME^"
sed "s^@LANTORRENT_HOME@^$LANTORRENT_HOME^" $LANTORRENT_HOME/etc/xinetd.conf.in | tee $LANTORRENT_HOME/tests/xinetd.conf


exec xinetd -d -f $LANTORRENT_HOME/tests/xinetd.conf -dontfork
