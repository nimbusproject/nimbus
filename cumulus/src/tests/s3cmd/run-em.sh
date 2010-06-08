#!/bin/bash

s3cmd=`which s3cmd`

if [ "X$s3cmd" == "X" ]; then
    echo "s3cmd is not installed so we are skiping these tests"
    exit 0
fi

cnt=0
for t in test_*
do
    ./"$t"

    if [ "X$?" != "X0" ]; then
        echo "ERROR: $t failed"
        exit 1
    fi
    cnt=`expr $cnt + 1`
done

echo "Success : $cnt tests passed"
exit 0
