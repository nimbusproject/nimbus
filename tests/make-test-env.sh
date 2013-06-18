#!/bin/bash

echo "home directory in use is $HOME"
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

mkdir $HOME/.ssh
mkdir $HOME/.globus
mkdir $HOME/.nimbus
chmod 700 $HOME/.ssh
chmod 700 $HOME/.globus
chmod 700 $HOME/.nimbus


bd=`dirname $0`
cd $bd
src_dir=`pwd`


if [ "X$NIMBUS_SRC_DIR" == "X" ]; then
    echo "Going to use git"
    repo_dir="$work_dir/src"
    mkdir $repo_dir
    cd $repo_dir

    repo="git://github.com/nimbusproject/nimbus.git"
    if [ "X$NIMBUS_REPO" != "X" ]; then
        repo=$NIMBUS_REPO
    fi
    echo "Checking out nimbus from $repo"
    git clone --depth 1 $repo nimbus
    if [ $? -ne 0 ]; then
        echo "failed to checkout git from $NIMBUS_REPO"
        exit 1
    fi

    nimbus_source_dir=$repo_dir/nimbus
    nimbus_wsc_source_dir=$nimbus_source_dir/control/ 
    nimbus_cc_dir=$nimbus_source_dir/cloud-client
    mkdir $nimbus_wsc_dir
    unset CLOUD_CLIENT_HOME
else
    echo "Going to use premade dirs"

    if [ "X$NIMBUS_WSC_SRC_DIR" = "X" ]; then
        echo "Either unset NIMBUS_SRC_DIR or set NIMBUS_WSC_SRC_DIR"
        exit 1
    fi 
    if [ "X$NIMBUS_CC_DIR" = "X" ]; then
        echo "Either unset NIMBUS_SRC_DIR or set NIMBUS_CC_DIR"
        exit 1
    fi 
    if [ "X$CLOUD_CLIENT_HOME" = "X" ]; then
        echo "Either unset NIMBUS_SRC_DIR or set CLOUD_CLIENT_HOME"
        exit 1
    fi 

    nimbus_source_dir=$NIMBUS_SRC_DIR
    nimbus_wsc_source_dir=$NIMBUS_WSC_SRC_DIR/workspace-control/
    export CLOUD_CLIENT_HOME=$NIMBUS_CC_DIR
    nimbus_cc_dir=$CLOUD_CLIENT_HOME
fi
wsc_dst_src=$work_dir/control
mkdir $wsc_dst_src

install_dir=$work_dir/NIMBUSINSTALL
export NIMBUS_HOME=$install_dir

cd $nimbus_source_dir
echo "========================================="
echo "Installing nimbus"
echo "========================================="
python $src_dir/install-nim.py ./install $install_dir "$work_dir/install.log"
rc=$?
if [ $rc -ne 0 ]; then
    ls -l $src_dir/install-nim.py
    ls -l 
    echo "nimbus install failed"
    exit 1
fi

echo "========================================="
echo "Configuring propagation only mode"
echo "========================================="

ls -l $HOME/.ssh/
new_key=$HOME/.ssh/id_rsa
python $src_dir/ssh.py $new_key
rc=$?
ls -l $HOME/.ssh/
if [ $rc -ne 0 ]; then
    echo "failed to make the ssh key"
    exit 1
fi
user=`whoami`
echo "Attempting to ssh"
ssh localhost hostname
rc=$?
echo "ssh return code $rc"

export NIMBUS_WORKSPACE_CONTROL_HOME=$wsc_dst_src
cp -r $nimbus_wsc_source_dir/*  $wsc_dst_src
if [ $? -ne 0 ]; then
    echo "could not copy in WSC:: cp -r $nimbus_wsc_source_dir/*  $wsc_dst_src"
    exit 1
fi

sed -e "s^@NIMBUS_WORKSPACE_CONTROL_HOME@^$NIMBUS_WORKSPACE_CONTROL_HOME^" -e "s^@KEY@^$new_key^" -e "s/@WHO@/$user/" $src_dir/autoconfig-decisions.sh.in > $install_dir/services/share/nimbus-autoconfig/autoconfig-decisions.sh

cat $install_dir/services/share/nimbus-autoconfig/autoconfig-decisions.sh

$install_dir/services/share/nimbus-autoconfig/autoconfig-adjustments.sh

#cd $work_dir/control
cd $wsc_dst_src
pwd
if [ "X$NIMBUS_TEST_REAL" == "X" ]; then
    bash ./src/propagate-only-mode.sh
fi

if [ $? -ne 0 ]; then
    echo "PROP ONLY MODE CONFIGURATION FAILED"
    exit 1
fi

echo "========================================="
echo "Making cloud client"
echo "========================================="

if [ "X$CLOUD_CLIENT_HOME" == "X" ]; then
    cd $nimbus_cc_dir
    pwd
    bash ./builder/get-wscore.sh
    if [ $? -ne 0 ]; then
        echo "bash ./builder/get-wscore.sh failed"
        exit 1
    fi
    bash ./builder/dist.sh
    if [ $? -ne 0 ]; then
        echo "bash ./builder/dist.sh failed"
        exit 1
    fi
    cd $work_dir
    tar -zxvf $nimbus_cc_dir/nimbus-cloud-client*.tar.gz
    if [ $? -ne 0 ]; then
        echo "failed to untar $nimbus_cc_dir/nimbus-cloud-client*.tar.gz"
        exit 1
    fi

    cd nimbus-cloud-client*
    ./bin/cloud-client.sh --help
    export CLOUD_CLIENT_HOME=`pwd`
fi

echo "========================================="
echo "Making a common user"
echo "========================================="
user_name="nimbus@$RANDOM"
user_stuff=`$install_dir/bin/nimbus-new-user --batch -r cloud_properties,cert,key,access_id,access_secret $user_name`
aid=`echo $user_stuff | awk -F , '{ print $4 }'` 
apw=`echo $user_stuff | awk -F , '{ print $5 }'` 

sed -e "s^@ID@^$aid^" -e "s/@KEY@/$apw/" $src_dir/s3cfg.in > $HOME/.s3cfg
cat $HOME/.s3cfg

echo "========================================="
echo "Making a new user"
echo "========================================="

user_name="nimbus@$RANDOM"
user_stuff=`$install_dir/bin/nimbus-new-user --batch -r cloud_properties,cert,key,access_id,access_secret,canonical_id $user_name`

echo $user_stuff
cp=`echo $user_stuff | awk -F , '{ print $1 }'` 
cert=`echo $user_stuff | awk -F , '{ print $2 }'` 
key=`echo $user_stuff | awk -F , '{ print $3 }'` 
aid=`echo $user_stuff | awk -F , '{ print $4 }'` 
apw=`echo $user_stuff | awk -F , '{ print $5 }'` 
can_id=`echo $user_stuff | awk -F , '{ print $6 }'` 

sed -e "s^@ID@^$aid^" -e "s/@KEY@/$apw/" $src_dir/s3cfg.in > $HOME/.s3cfg.reg

pwd
echo $cp
echo $cert
echo $key

cd $CLOUD_CLIENT_HOME
cp $install_dir/var/ca/ca-certs/*  $CLOUD_CLIENT_HOME/lib/certs/
if [ $? -ne 0 ]; then
    pwd
    echo "could not copy to $CLOUD_CLIENT_HOME/lib/certs/"
    exit 1
fi
cp $cp conf/

mkdir $HOME/.nimbus
cp $cert  $HOME/.nimbus/
cp $key  $HOME/.nimbus/
cp -r $HOME/.nimbus $HOME/.globus

echo "reporitng contents of dot nimbus and globus"
ls -l $HOME/.nimbus/
ls -l $HOME/.globus/

cd $CLOUD_CLIENT_HOME
$CLOUD_CLIENT_HOME/bin/grid-proxy-init.sh

echo "========================================="
echo "Setting up VMM and network pools"
echo "========================================="

#sed -i 's^socket.dir=$NIMBUS_HOME/var/run/privileged/^socket.dir=//tmp^' $install_dir/services/etc/nimbus/workspace-service/admin.conf
#if [ $? -ne 0 ]; then
#    echo "failed to sed admin file"
#    exit 1
#fi


$install_dir/bin/nimbusctl services start
if [ $? -ne 0 ]; then
    echo "Starting Nimbus services failed"
    exit 1
fi
sleep 15 # make sure it is really started, uhhhhh
echo "trying $install_dir/bin/nimbusctl services status"
$install_dir/bin/nimbusctl services status
if [ $? -ne 0 ]; then
    echo "Starting Nimbus services failed"
    cat $install_dir/var/services.log
    cat $install_dir/var/cumulus.log
    exit 1
fi

done=1
try_count=0
while [ $done -ne 0 ];
do
    $src_dir/nimbus-nodes.sh
    if [ $? -eq 0 ]; then
        done=0
    else
        try_count=`expr $try_count + 1`
     
        if [ $try_count -gt 10 ]; then
            echo "Adding VMM node failed"
            cat $install_dir/var/services.log
            cat $install_dir/var/cumulus.log
            ls -l $install_dir/var/
            exit 1
        fi
        sleep 30
    fi
done

$install_dir/bin/nimbusctl services stop
if [ $? -ne 0 ]; then
    echo "Stopping Nimbus services failed"
    exit 1
fi


cp $src_dir/public  $install_dir/services/etc/nimbus/workspace-service/network-pools/public

# Install test dependencies
source $NIMBUS_HOME/ve/bin/activate
easy_install pexpect

echo $work_dir
export NIMBUS_TEST_USER=$user_name

export NIMBUS_TEST_CA="/O=Auto/OU=CA"

echo "Your test environment is:"
echo "NIMBUS_HOME:          $NIMBUS_HOME"
echo "NIMBUS_TEST_USER:     $NIMBUS_TEST_USER"
echo "CLOUD_CLIENT_HOME:    $CLOUD_CLIENT_HOME"
echo "NIMBUS_WORKSPACE_CONTROL_HOME:          $NIMBUS_WORKSPACE_CONTROL_HOME"
echo "NIMBUS_TEST_USER_CAN_ID:          $can_id"
echo "NIMBUS_TEST_CA:       $NIMBUS_TEST_CA"


echo "export NIMBUS_HOME=$NIMBUS_HOME" > $src_dir/env.sh
echo "export NIMBUS_TEST_USER=$NIMBUS_TEST_USER" >> $src_dir/env.sh
echo "export CLOUD_CLIENT_HOME=$CLOUD_CLIENT_HOME" >> $src_dir/env.sh
echo "export NIMBUS_WORKSPACE_CONTROL_HOME=$NIMBUS_WORKSPACE_CONTROL_HOME" >> $src_dir/env.sh
echo "export NIMBUS_TEST_USER_CAN_ID=$can_id" >> $src_dir/env.sh
echo "export NIMBUS_TEST_IMAGE=group" >> $src_dir/env.sh
echo "export NIMBUS_SOURCE_TEST_IMAGE=/etc/group" >> $src_dir/env.sh
echo "export NIMBUS_TEST_TIMEOUT=90" >> $src_dir/env.sh
echo "export NIMBUS_TEST_CA=$NIMBUS_TEST_CA" >> $src_dir/env.sh

