#!/bin/bash

work_dir=$1
if [ "X$work_dir" == "X" ]; then
    work_dir=`mktemp --tmpdir=$HOME -d -t tmp.XXXXXXXXXX`
fi
echo $work_dir

bd=`dirname $0`
cd $bd
src_dir=`pwd`

repo_dir="$work_dir/src"
mkdir $repo_dir
cd $repo_dir

repo="git://github.com/nimbusproject/nimbus.git"
repo="/home/nimbus/nimbus"
repo="/home/bresnaha/Dev/Nimbus/nimbus"
git clone $repo

install_dir=$work_dir/NIMBUSINSTALL

cd nimbus/
echo "========================================="
echo "Installing nimbus"
echo "========================================="
python $src_dir/install-nim.py ./install $install_dir "$work_dir/install.log"
rc=$?
if [ $rc -ne 0 ]; then
    echo "nimbus install failed"
    exit 1
fi

echo "========================================="
echo "Configuring propagation only mode"
echo "========================================="

cp -r $repo_dir/nimbus/control/   $work_dir
ssh-keygen -N "" -f $work_dir/keys
cp ~/.ssh/authorized_keys ~/.ssh/authorized_keys.back
cat $work_dir/keys.pub >> ~/.ssh/authorized_keys
user=`whoami`

sed -e "s^@KEY@^$work_dir/keys^" -e "s/@WHO@/$user/" $src_dir/autoconfig-decisions.sh.in > $install_dir/services/share/nimbus-autoconfig/autoconfig-decisions.sh

cat $install_dir/services/share/nimbus-autoconfig/autoconfig-decisions.sh

$install_dir/services/share/nimbus-autoconfig/autoconfig-adjustments.sh

cd $work_dir/control
bash ./src/propagate-only-mode.sh

echo "========================================="
echo "Making cloud client"
echo "========================================="

cd $repo_dir/nimbus/cloud-client
bash ./builder/get-wscore.sh
bash ./builder/dist.sh
cd $work_dir
tar -zxvf $repo_dir/nimbus/cloud-client/nimbus-cloud-client*.tar.gz

cd nimbus-cloud-client*
./bin/cloud-client.sh --help
export CLOUD_CLIENT_HOME=`pwd`

echo "========================================="
echo "Making a new user"
echo "========================================="

user_name="nimbus@$RANDOM"
user_stuff=`$install_dir/bin/nimbus-new-user --batch -r cloud_properties,cert,key $user_name```

echo $user_stuff
cp=`echo $user_stuff | awk -F , '{ print $1 }'` 
cert=`echo $user_stuff | awk -F , '{ print $2 }'` 
key=`echo $user_stuff | awk -F , '{ print $3 }'` 

echo $cp
echo $cert
echo $key

cp $install_dir/var/ca/ca-certs/*  lib/certs/
cp $cp conf/
mkdir ~/.nimbus
cp $cert  ~/.nimbus/
cp $key  ~/.nimbus/
./bin/grid-proxy-init.sh


echo "========================================="
echo "Starting the services"
echo "========================================="
cd $install_dir
./bin/nimbusctl restart
if [ $? -ne 0 ]; then
    echo "something somewhere went wrong. look through the output above"
    exit 1
fi


echo "========================================="
echo "Run tests...."
echo "========================================="
cd $src_dir

for t in *test.sh
do
    ./t
    if [ $? -ne 0 ]; then
        echo "the test $t failed"
        exit 1
    fi
done

echo $work_dir
export NIMBUS_HOME=$install_dir
export NIMBUS_TEST_USER=$user_name
#rm -rf $work_dir
#mv ~/.ssh/authorized_keys.back ~/.ssh/authorized_keys

$install_dir/bin/nimbusctl stop
exit $?

