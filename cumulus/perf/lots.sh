#!/bin/bash

for ((i=1; i <= $1; i++))
do
    ./perf2.py 2 1024 100MB 7752df6ba16a2e520b808a62dc130ede $i $i $i&
done
wait
