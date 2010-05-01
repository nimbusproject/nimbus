#!/bin/sh

mv ~/.s3cfg ~/.s3cfg.cumulus.test

cumulus_host=`hostname -f`
cumulus_port=8888

#$CUMULUS_HOME/bin/cumulus.sh -p $cumulus_port &
cumulus_pid=$!
echo $cumulus_pid
trap "pkill cumulus; mv ~/.s3cfg.cumulus.test ~/.s3cfg" EXIT
sleep 2

log_file=`mktemp`
echo "Logging output to $log_file" 
$CUMULUS_HOME/bin/cumulus-add-user.sh -g -n tests3cmd1@nimbus.test | tee $log_file
grep ID $log_file
id=`grep ID $log_file | awk '{ print $2 }'`
pw=`grep ID $log_file | awk '{ print $4 }'`

sed -e "s^@@HOST_PORT@@^$cumulus_host:$cumulus_port^g" -e "s^@@ID@@^$id^" -e "s^@@KEY@@^$pw^" $CUMULUS_HOME/etc/dot_s3cfg.in > ~/.s3cfg

cnt=0
for t in test_*
do
    ./"$t"

    if [ "X$?" != "X0" ]; then
        echo "ERROR: $t failed"
        exit 1
    fi
    cnt=`expr $cnt + 1`
done

echo "Success : $cnt tests passed"
