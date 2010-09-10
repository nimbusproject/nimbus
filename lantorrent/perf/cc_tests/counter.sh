#!/bin/bash

tm_file=$1
cnts="4 8 16 32 64"
trials=4

touch $tm_file
for c in $cnts
do
    for i in `seq 1 $trials`
    do
        echo $i
        /usr/bin/time --append -o $tm_file --format "$c %e" ./timer.sh $c
        sleep 90
        rm -rf /home/bresnaha/nimbus-cloud-client-016/history/*
        sleep 90
    done
done

