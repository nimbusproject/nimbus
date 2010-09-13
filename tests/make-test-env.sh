#!/bin/bash

bkdate=`date +%s`
work_dir=$1
if [ "X$work_dir" == "X" ]; then
    echo "You must provide a base install directory"
    exit 1
fi

if [ -e $HOME/.ssh ]; then
    echo "this will destroy your .ssh dir!  please back it up first"
    exit 1
fi
if [ -e $HOME/.globus ]; then
    echo "this will destroy your .globus dir!  please back it up first"
    exit 1
fi
if [ -e $HOME/.nimbus ]; then
    echo "this will destroy your .nimbus dir!  please back it up first"
    exit 1
fi
if [ -e $HOME/.s3cfg ]; then
    echo "this will destroy your .s3cfg file!  please back it up first"
    exit 1
fi

bd=`dirname $0`
cd $bd
src_dir=`pwd`

repo_dir="$work_dir/src"
mkdir $repo_dir
cd $repo_dir

repo="git://github.com/nimbusproject/nimbus.git"
if [ "X$NIMBUS_REPO" != "X" ]; then
    repo=$NIMBUS_REPO
fi
git clone --depth 1 $repo

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

new_key=$HOME/.ssh/id_rsa
python $src_dir/ssh.py $new_key
user=`whoami`
ls -l $HOME/.ssh
echo "Attempting to ssh"
ssh localhost hostname

cp -r $repo_dir/nimbus/control/  $work_dir
sed -e "s^@NIMBUS_WORKSPACE_CONTROL_HOME@^$work_dir/control^" -e "s^@KEY@^$new_key^" -e "s/@WHO@/$user/" $src_dir/autoconfig-decisions.sh.in > $install_dir/services/share/nimbus-autoconfig/autoconfig-decisions.sh

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
user_stuff=`$install_dir/bin/nimbus-new-user --group 04 --batch -r cloud_properties,cert,key,access_id,access_secret $user_name```

echo $user_stuff
cp=`echo $user_stuff | awk -F , '{ print $1 }'` 
cert=`echo $user_stuff | awk -F , '{ print $2 }'` 
key=`echo $user_stuff | awk -F , '{ print $3 }'` 
aid=`echo $user_stuff | awk -F , '{ print $4 }'` 
apw=`echo $user_stuff | awk -F , '{ print $5 }'` 
sed -e "s^@ID@^$aid^" -e "s/@KEY@/$apw/" $src_dir/s3cfg.in > $HOME/.s3cfg

echo $cp
echo $cert
echo $key

cp $install_dir/var/ca/ca-certs/*  lib/certs/
cp $cp conf/

mkdir $HOME/.nimbus
cp $cert  $HOME/.nimbus/
cp $key  $HOME/.nimbus/
cp -r $HOME/.nimbus $HOME/.globus

./bin/grid-proxy-init.sh

echo "localhost 10240" >> $install_dir/services/etc/nimbus/workspace-service/vmm-pools/testpool

echo $work_dir
export NIMBUS_HOME=$install_dir
export NIMBUS_WORKSPACE_CONTROL_HOME="$work_dir/control"
export NIMBUS_TEST_USER=$user_name

echo "Your test environment is:"
echo "NIMBUS_HOME:          $NIMBUS_HOME"
echo "NIMBUS_TEST_USER:     $NIMBUS_TEST_USER"
echo "CLOUD_CLIENT_HOME:    $CLOUD_CLIENT_HOME"
echo "NIMBUS_WORKSPACE_CONTROL_HOME:          $NIMBUS_HOME"


echo "export NIMBUS_HOME=$NIMBUS_HOME" > $src_dir/env.sh
echo "export NIMBUS_TEST_USER=$NIMBUS_TEST_USER" >> $src_dir/env.sh
echo "export CLOUD_CLIENT_HOME=$CLOUD_CLIENT_HOME" >> $src_dir/env.sh
echo "export NIMBUS_WORKSPACE_CONTROL_HOME=$NIMBUS_WORKSPACE_CONTROL_HOME" >> $src_dir/env.sh

