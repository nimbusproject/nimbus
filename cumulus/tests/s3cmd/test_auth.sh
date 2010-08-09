#!/bin/bash

bucket_name=CumulusTest$RANDOM
fname=GRP$RANDOM

s3cmd mb s3://$bucket_name
# just run it a few times for races
s3cmd -r put /etc/group s3://$bucket_name/$fname
if [ "X$?" != "X0" ]; then
    echo "recursive put failed"
    exit 1
fi
s3cmd setacl --acl-public  s3://$bucket_name/$fname
if [ "X$?" != "X0" ]; then
    echo "setacl failed failed"
    exit 1
fi

s3cmd -r --force del s3://$bucket_name/
if [ "X$?" != "X0" ]; then
    echo "ERROR: delete bucket failed"
    exit 1
fi
exit 0
