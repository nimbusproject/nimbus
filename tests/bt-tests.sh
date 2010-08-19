#!/bin/bash

bd=`dirname $0`
cd $bd
src_dir=`pwd`

echo "========================================="
echo "Run tests...."
echo "========================================="
cd $src_dir

cnt="0"
error_cnt="0"
error_ts=""
for t in *test.{sh,py}
do
    echo $t
    ./$t 2>&1 | tee $t.log
    if [ $PIPESTATUS -ne 0 ]; then
        echo "$cnt parent tests passed (many more subtests were run)"
        echo "the test $t failed"
        error_cnt=`expr $error_cnt + 1`
        error_ts="$error_ts $t"
    else
        cnt=`expr $cnt + 1`
    fi
done
echo "$cnt parent tests passed (many more subtests were run)"
echo "$error_cnt parent tests failed"
echo "    $error_ts"

exit $error_cnt

