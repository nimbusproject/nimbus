#!/bin/sh

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
