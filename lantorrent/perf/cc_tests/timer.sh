#!/bin/bash

cmd="./cmd.sh"
max=$1
pid_list=""
for i in `seq 1 $max`
do
    touch run.log
    $cmd | tee -a run.log &
    pid_list="$pid_list $!"
done

wait

