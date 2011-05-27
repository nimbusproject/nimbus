#!/bin/bash

unset SSH_AGENT_PID
unset SSH_AUTH_SOCK

work_dir=$1
clean_up=$2
run_tests=$3
bkdate=`date +%s`
function on_exit()
{
    echo "Cleaning up! $bkdate"
    if [ "X$work_dir" == "X" ]; then
        rm -rf $work_dir
        echo "deleting the checkout"
    fi
    if [ "X$clean_up" != "Xno" ]; then
        rm -rf $HOME/.nimbus
        rm -rf $HOME/.globus
        rm -rf $HOME/.ssh
        mv $HOME/.nimbus.$bkdate $HOME/.nimbus
        mv $HOME/.globus.$bkdate $HOME/.globus
        mv $HOME/.ssh.$bkdate $HOME/.ssh
        mv $HOME/.s3cfg.$bkdate $HOME/.s3cfg
        echo "put everything back"
    fi
}

if [ "X$work_dir" == "X" ]; then
    work_dir=`mktemp -p $HOME -d -t tmp.XXXXXXXXXX`
    #work_dir=`mktemp --tmpdir=$HOME -d -t tmp.XXXXXXXXXX`
fi

bd=`dirname $0`
cd $bd
src_dir=`pwd`

mv $HOME/.ssh $HOME/.ssh.$bkdate
mv $HOME/.nimbus $HOME/.nimbus.$bkdate
mv $HOME/.globus $HOME/.globus.$bkdate
mv $HOME/.s3cfg $HOME/.s3cfg.$bkdate
trap on_exit EXIT
echo "Building a Nimbus env at $work_dir"
./make-test-env.sh $work_dir | tee bandt.log
if [ $PIPESTATUS -ne 0 ]; then
    cat $NIMBUS_HOME/var/services.log
    echo "nimbus install failed"
    exit 1
fi
source env.sh

cd $CLOUD_CLIENT_HOME
./bin/grid-proxy-init.sh

echo "========================================="
echo "Starting the services"
echo "========================================="
cd $NIMBUS_HOME

# change fake mode
sed -i 's/fake.mode=true/fake.mode=false/' ./services/etc/nimbus/workspace-service/other/common.conf

pkill cumulus
./bin/nimbusctl restart
if [ $? -ne 0 ]; then
    echo "something somewhere went wrong. look through the output above"
    echo "and check the log files.  There is possibly already a service running"
    echo "or something else is claiming the needed ports."
    exit 1
fi

echo "========================================="
echo "Run tests...."
echo "========================================="
cd $src_dir

if [ "X$run_tests" != "Xno" ]; then
    ./bt-tests.sh
    rc=$?
else
    rc=0
fi
$NIMBUS_HOME/bin/nimbusctl stop
exit $rc

