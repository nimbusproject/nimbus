#!/bin/bash

tm_file=$1
touch $tm_file
c_count="32"
f_sizes="512"
buck="s3://TestBucket$RANDOM"
s3cmd mb $buck
pwd_dir=`pwd`
for s in $f_sizes
do
    echo "doing file $s"
    fname=$s.data
    dd of=$fname if=/dev/urandom count=$s bs=1048576 2> /dev/null
    md5=`md5sum $fname | awk '{ print $1 }'`
    b64=`./sumconvert.py $md5`

    for c in $c_count
    do
        echo "doing $c clients at once"

        rm -f cumulus_tests.*
        # put them all at once
        for ((cnt=1; cnt <= $c; cnt++))
        do
            tf=`mktemp $pwd_dir/cumulus_tests.XXXXXXXXXX`
            echo $tf
            /usr/bin/time -a -o $tf --format "put $s $c %e" s3cmd --add-header=content-md5:$b64  --force put $fname $buck/$fname &
        done
        wait
        cat cumulus_tests.* >> $tm_file
        rm -f cumulus_tests.*

        for ((cnt=1; cnt <= $c; cnt++))
        do
            tf=`mktemp $pwd_dir/cumulus_tests.XXXXXXXXXX`
            /usr/bin/time -a -o $tf --format "get $s $c %e" s3cmd --force get $buck/$fname $fname.back &
        done
        wait
        cat cumulus_tests.* >> $tm_file
        rm -f cumulus_tests.*

        s3cmd del $buck/$fname
    done
    rm $fname
    rm $fname.back
done
s3cmd -r rb $buck

cat $tm_file
