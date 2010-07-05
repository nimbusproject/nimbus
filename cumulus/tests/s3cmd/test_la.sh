#!/bin/bash

bucket_name=CumulusTest$RANDOM
fname=GRP$RANDOM
src_dir=`mktemp -d -t tmp.XXXXXXXXXX`
dest_dir=`mktemp -d -t tmp.XXXXXXXXXX`

for ((i=1; i <= 10; i++))
do
    cp /etc/group $src_dir
done

s3cmd mb s3://$bucket_name
# just run it a few times for races
s3cmd -r put $src_dir s3://$bucket_name/
if [ "X$?" != "X0" ]; then
    echo "recursive put failed"
    exit 1
fi
s3cmd la
if [ "X$?" != "X0" ]; then
    echo "la failed"
    exit 1
fi

s3cmd -r --force del s3://$bucket_name/
if [ "X$?" != "X0" ]; then
    echo "ERROR: delete bucket failed"
    exit 1
fi
exit 0
