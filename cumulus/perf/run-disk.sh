#!/bin/sh

c_counts="1 2 4 8 16 32 64 128"
f_sizes="1 10 25 32"
echo "clients\tsizeMB\ttotalMB\ttotaltime\ttotalMBps\tavgtm\tavgbw\tmax\tmin"
for s in $f_sizes
do
    fname=$s.data
    dd of=$fname if=/dev/urandom count=$s bs=1048576 2> /dev/null
    for c in $c_counts
    do
        ./disk2disk.py $c $fname | tail -n 2
    done
    rm $fname
done
wait
