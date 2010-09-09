#!/bin/bash

port=$2
userhost=$3
remoteexe=$4
remotepath=$5
localpath=$6
rid=$7
ltcs=$8

ssh -p $port $userhost "$remoteexe" --nonblock "$remotepath" "$localpath" "$rid" "$ltcs"
rc=$?
if [ "X$rc" -ne 0 ]; then
    exit $rc
fi

# check for an error every 30 seconds.. this may need to be in a decent language
cnt=0
while [ ! -e $localpath ];
do
    sleep 1
    cnt=`expr $cnt + 1`

    # if we have waited 30 seconds verify that there wasnt an error
    if [ $cnt -gt 30 ]; then
        cnt=0
        ssh -p $port $userhost "$remoteexe" --nonblock --reattach "$rid"
        if [ "X$rc" -ne 0 ]; then
            exit $rc
        fi
    fi
done

exit $rc
