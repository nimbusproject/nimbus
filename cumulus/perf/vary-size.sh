#!/bin/bash

tm_file=$1
f_sizes="2 4 8 16 32 64 128 256 512 1024 2048"
buck="s3://TestBucket$RANDOM"
s3cmd mb $buck
for s in $f_sizes
do
    fname=$s.data
    dd of=$fname if=/dev/urandom count=$s bs=1048576 2> /dev/null
    md5=`md5sum $fname | awk '{ print $1 }'`
    b64=`./sumconvert.py $md5`
    echo $b64
    for i in `seq 1 10`
    do
        /usr/bin/time -a -o $tm_file --format "put $s %e" s3cmd --add-header=content-md5:$b64  --force put $fname $buck/$fname
        echo $?
        /usr/bin/time -a -o $tm_file --format "get $s %e" s3cmd --force get $buck/$fname $fname.back
        echo $?
        diff -q $fname $fname.back
        echo $?
        s3cmd del $buck/$fname
        rm $fname.back
    done
    rm $fname
done
s3cmd -r rb $buck

cat $tm_file
