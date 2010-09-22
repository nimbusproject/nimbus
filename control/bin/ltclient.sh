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
ssh_error_cnt=0
done="False"
thresh=`expr $RANDOM % 30`
while [ ! -e $localpath ];
do
    sleep 1
    cnt=`expr $cnt + 1`

    # if we have waited 30 seconds verify that there wasnt an error
    if [ $cnt -gt $thresh ]; then
        echo "ltclient checking in..."
        thresh=30
        cnt=0
        out=`ssh -p $port $userhost "$remoteexe" --nonblock --reattach "$rid"`
        if [ $? -ne 0 ]; then
            ssh_error_cnt=`expr $ssh_error_cnt + 1`
            echo $out
            echo "ssh failed, we allow this to happen a few times"
            if [ $ssh_error_cnt -gt 3 ]; then
                exit 1
            fi
        else
            rc=`echo $out | awk -F , '{ print $1 }'`
            done=`echo $out | awk -F , '{ print $2 }'`
            message=`echo $out | awk -F , '{ print $3 }'`

            if [ "X$done" == "XTrue" ]; then
                if [ $rc -ne 0 ]; then
                    echo $message
                    exit $rc
                fi
            fi
        fi
    fi
done
if [ "X$done" == "XFalse" ]; then
    ssh -p $port $userhost "$remoteexe" --reattach "$rid"
fi
rc=$?
exit $rc
