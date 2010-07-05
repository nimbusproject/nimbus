#!/bin/bash

bucket_name=CumulusTest$RANDOM
fname=GRP$RANDOM
src_f1=`mktemp -t tmp.XXXXXXXXXX`
src_f2=`mktemp -t tmp.XXXXXXXXXX`
dst_f=`mktemp -t tmp.XXXXXXXXXX`

# just run it a few times for races
for ((i=1; i <= 4; i++))
do
    dd if=/dev/urandom of=$src_f1 count=1024 bs=1024
    dd if=/dev/urandom of=$src_f2 count=1024 bs=1024
    s3cmd mb s3://$bucket_name
    s3cmd put $src_f1 s3://$bucket_name/$fname &
    s3cmd put $src_f2 s3://$bucket_name/$fname &
    wait
    s3cmd --force get s3://$bucket_name/$fname $dst_f

    diff -q $dst_f $src_f1 || diff -q $dst_f $src_f2
    if [ "X$?" != "X0" ]; then
        echo "ERROR: diff failed"
        exit 1
    fi
    rm $src_f1
    rm $src_f2
    rm $dst_f

    s3cmd del s3://$bucket_name/$fname
    if [ "X$?" != "X0" ]; then
        echo "ERROR: delete failed"
        exit 1
    fi

    s3cmd rb s3://$bucket_name
    if [ "X$?" != "X0" ]; then
        echo "ERROR: delete bucket failed"
        exit 1
    fi
done
exit 0
