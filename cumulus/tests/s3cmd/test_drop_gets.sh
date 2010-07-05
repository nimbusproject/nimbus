#!/bin/bash

bucket_name=CumulusTest$RANDOM
fname=GRP$RANDOM
s3cmd mb s3://$bucket_name

# make a big enough file
f=`mktemp -t tmp.XXXXXXXXXX`
dd if=/dev/zero of=$f count=1024 bs=102400

s3cmd put $f s3://$bucket_name/$fname
if [ "X$?" != "X0" ]; then
    echo "ERROR: put failed"
    exit 1
fi

for ((i=1; i <= 4; i++))
do
    s3cmd --force get s3://$bucket_name/$fname $f &
    pid=$!
    echo "s3cmd --force get s3://$bucket_name/$fname $f"
    sleep 1
    kill $pid
    wait
done
rm $f

# cleanup
s3cmd del s3://$bucket_name/$fname
if [ "X$?" != "X0" ]; then
    echo "ERROR: did not delete the file"
    exit 1
fi
s3cmd rb s3://$bucket_name
if [ "X$?" != "X0" ]; then
    echo "ERROR: delete bucket failed"
    exit 1
fi

exit 0
