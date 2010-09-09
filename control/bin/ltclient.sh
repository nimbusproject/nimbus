#!/bin/bash

port=$1
userhost=$2
remoteexe=$3
remotepath=$4
localpath=$5
rid=$6
ltcs=$7

ssh -p $port $userhost "$remoteexe" --nonblock "$remotepath" "$localpath" "$rid" "$ltcs"
rc=$?
if [ $rc -ne 0 ]; then
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
#        ssh -p $port $userhost "$remoteexe" --nonblock --reattach "$rid"
#        if [ $? -ne 0 ]; then
#            exit $rc
#        fi
    fi
done

exit $rc
