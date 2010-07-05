#!/bin/bash

bucket_name=CumulusTest$RANDOM
fname=GRP$RANDOM
src_f1=`mktemp -t tmp.XXXXXXXXXX`

s3cmd mb s3://$bucket_name
# just run it a few times for races
dd if=/dev/urandom of=$src_f1 count=1024 bs=1024
for ((i=1; i <= 10; i++))
do
    s3cmd put $src_f1 s3://$bucket_name/$fname$i
done
s3cmd -r --force del s3://$bucket_name/
if [ "X$?" != "X0" ]; then
    echo "ERROR: delete bucket failed"
    exit 1
fi
exit 0
