#!/bin/bash

cd ..
source env.sh
cd -

mv ~/.s3cfg ~/.s3cfg.cumulus.test

cumulus_host=`hostname -f`
cumulus_port=8888

export CUMULUS_TEST_HOST=$cumulus_host
export CUMULUS_TEST_PORT=$cumulus_port


$CUMULUS_HOME/bin/cumulus.sh -p $cumulus_port &
cumulus_pid=$!
echo $cumulus_pid
trap "pkill cumulus; mv ~/.s3cfg.cumulus.test ~/.s3cfg; $CUMULUS_HOME/bin/cumulus-add-user.sh -r tests3cmd1@nimbus.test" EXIT
sleep 2
log_file=`mktemp`
echo "Logging output to $log_file" 
$CUMULUS_HOME/bin/cumulus-add-user.sh -g -n tests3cmd1@nimbus.test | tee $log_file
grep ID $log_file
id=`grep ID $log_file | awk '{ print $2 }'`
pw=`grep ID $log_file | awk '{ print $4 }'`

sed -e "s^@@HOST_PORT@@^$cumulus_host:$cumulus_port^g" -e "s^@@ID@@^$id^" -e "s^@@KEY@@^$pw^" $CUMULUS_HOME/etc/dot_s3cfg.in > ~/.s3cfg

cd s3cmd
./run-em.sh
if [ "X$?" != "X0" ]; then
    echo "FAILED : s3 tests failed"
    exit 1
fi
cd ..

cd authz
nosetests *.py
if [ "X$?" != "X0" ]; then
    echo "FAILED : authz tests failed"
    exit 1
fi
cd ..

cd client
nosetests *.py
if [ "X$?" != "X0" ]; then
    echo "FAILED : boto tests failed"
    exit 1
fi

echo "Success : $cnt tests passed"
