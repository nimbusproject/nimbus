#!/bin/bash

bd=`dirname $0`
cd $bd
src_dir=`pwd`

echo "========================================="
echo "Run tests...."
echo "========================================="
cd $src_dir

if [ -z "$RUN_TESTS" ] ; then
    RUN_TESTS=`ls *test.{sh,py}`
fi

cnt="0"
error_cnt="0"
error_ts=""
for t in $RUN_TESTS
do
    echo $t
    ./$t 2>&1 | tee $t.log
    if [ $PIPESTATUS -ne 0 ]; then
        echo "$cnt parent tests passed (many more subtests were run)"
        echo "the test $t failed"
        error_cnt=`expr $error_cnt + 1`
        error_ts="$error_ts $t"
    else
	x=`grep "Ran " $t.log  | grep " tests in " | sed "s/Ran //" | sed "s/tests in.*//"`
        if [ "X$x" == "X" ]; then
           cnt=`expr $cnt + 1`
        else
            for n in $x
            do
                cnt=`expr $cnt + $n`
            done
        fi
    fi
done
echo "$cnt tests passed"
echo "$error_cnt parent tests failed"
echo "    $error_ts"

exit $error_cnt

