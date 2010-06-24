#!/bin/sh

for i in `seq 1 $1`
do
    ./perf2.py 2 1024 100MB 7752df6ba16a2e520b808a62dc130ede $i $i $i&
done
wait
