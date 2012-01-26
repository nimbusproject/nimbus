#!/bin/bash

sp=`expr $RANDOM / 8000`
sleep $sp.$RANDOM

port=$1
userhost=$2
remoteexe=$3
remotepath=$4
localpath=$5
rid=$6
ltcs=$7

localpath_basedir=`dirname $localpath`

retry_count=3
cnt=0
done_req=0
while [ $done_req -eq 0 ];
do
    done_req=1
    ssh -p $port $userhost "$remoteexe" --nonblock "$remotepath" "$localpath" "$rid" "$ltcs"
    rc=$?
    cnt=`expr $cnt + 1`
    if [ $rc -ne 0 ]; then
        if [ $cnt -gt $retry_count ]; then
            echo "could not submit request adter $cnt tries"
            exit $rc
        fi
        sp=`expr $RANDOM % 2`
        sleep $sp.$RANDOM
        done_req=0
    fi
done

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
        out=`ssh -p $port $userhost "$remoteexe" --nonblock --reattach "$rid"`
        rc=$?
        # we only parse the output if the return code was 0
        if [ $rc -eq 0 ]; then
            rc=`echo $out | awk -F , '{ print $1 }'`
            done=`echo $out | awk -F , '{ print $2 }'`
            message=`echo $out | awk -F , '{ print $3 }'`
        fi
        echo $out
        if [ $rc -ne 0 ]; then
            ssh_error_cnt=`expr $ssh_error_cnt + 1`
            echo "ssh failed, we allow this to happen a few times"
            if [ $ssh_error_cnt -gt 3 ]; then
                exit 1
            fi
        else
            # once it succeds we can reset the error counter
            ssh_error_cnt=0
        fi

        if [ "X$done" == "XTrue" ]; then
            echo "exiting"
            exit $rc
        fi
    fi

    if [ ! -e $localpath_basedir ]; then
        echo "the directory for the receiving file does not exist"
        echo "this likely means that the request was terminated"
        ssh -p $port $userhost "$remoteexe" --nonblock --cancel --reattach "$rid"
        echo $?
        exit 2
    fi
done

echo "$localpath exists"
echo "running a blocking query"
done="False"
ssh_error_cnt=0
# if we get here the file exists but we have not yet received word of 
# suceess from the head node.  run a blocking query
while [ "X$done" == "XFalse" ];
do
    out=`ssh -p $port $userhost "$remoteexe" --reattach "$rid"`
    rc=$?
    if [ $rc -eq 0 ]; then
        rc=`echo $out | awk -F , '{ print $1 }'`
        done=`echo $out | awk -F , '{ print $2 }'`
        message=`echo $out | awk -F , '{ print $3 }'`
        echo $out
    fi

    if [ $rc -ne 0 ]; then
        if [ "X$done" == "XFalse" ]; then
            ssh_error_cnt=`expr $ssh_error_cnt + 1`
            if [ $ssh_error_cnt -gt 3 ]; then
                done="True"
            else
                sleep 0.$RANDOM
            fi
        fi
    else
        done="True"
    fi
done
echo "exiting with $rc"
exit $rc
