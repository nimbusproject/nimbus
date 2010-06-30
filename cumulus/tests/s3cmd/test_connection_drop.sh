#!/bin/bash

bucket_name=CumulusTest$RANDOM
fname=GRP$RANDOM
s3cmd mb s3://$bucket_name

f=`mktemp -t tmp.XXXXXXXXXX`
dd if=/dev/zero of=$f count=1024 bs=102400

s3cmd put $f s3://$bucket_name/$fname &
pid=$!
kill $pid
rm $f

#s3cmd info s3://$bucket_name/$fname
#if [ "X$?" == "X0" ]; then
#    echo "ERROR: the file should not be there"
#    exit 1
#fi
s3cmd del s3://$bucket_name/$fname
if [ "X$?" == "X0" ]; then
    echo "ERROR: the file should not be there"
    exit 1
fi

# cleanup
s3cmd rb s3://$bucket_name
if [ "X$?" != "X0" ]; then
    echo "ERROR: delete bucket failed"
    exit 1
fi

exit 0
