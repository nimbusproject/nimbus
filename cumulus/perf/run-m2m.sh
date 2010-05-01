#!/bin/sh

c_counts="1 2 4 8 16 32 64 128"
f_sizes="1 10 25 32"
echo "clients\tsizeMB\ttotalMB\ttotaltime\ttotalMBps\tavgtm\tavgbw\tmax\tmin"
for c in $c_counts
do
    for s in $f_sizes
    do
        ./mem2mem.py $c $s | tail -n 2
    done
done
wait
