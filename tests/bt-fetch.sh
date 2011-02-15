#!/bin/bash

bd=`dirname $0`
cd $bd
src_dir=`pwd`

version=$1
shift
cc_version=$1
shift

src_name="nimbus-$version-src"
src="http://www.nimbusproject.org/downloads/$src_name.tar.gz"
wsc_name="nimbus-controls-$version"
wsc_src="http://www.nimbusproject.org/downloads/$wsc_name.tar.gz"
cc_name="nimbus-cloud-client-$cc_version"
cc_src="http://www.nimbusproject.org/downloads/$cc_name.tar.gz"

wget $src
if [ $? -ne 0 ]; then
    echo "Failed to get $src"
    exit 1
fi
wget $wsc_src
if [ $? -ne 0 ]; then
    echo "Failed to get $wsc_src"
    exit 1
fi
wget $cc_src
if [ $? -ne 0 ]; then
    echo "Failed to get $cc_src"
    exit 1
fi

tar -zxvf $src_name.tar.gz
if [ $? -ne 0 ]; then
    echo "Failed to extract $src_name"
    exit 1
fi

tar -zxvf $wsc_name.tar.gz
if [ $? -ne 0 ]; then
    echo "Failed to extract $wsc_name"
    exit 1
fi

tar -zxvf $cc_name.tar.gz
if [ $? -ne 0 ]; then
    echo "Failed to extract $cc_name"
    exit 1
fi

export NIMBUS_SRC_DIR=`pwd`/$src_name
export NIMBUS_WSC_SRC_DIR=`pwd`/$wsc_name
export NIMBUS_CC_DIR=`pwd`/$cc_name
export CLOUD_CLIENT_HOME=`pwd`/$cc_name

./bt-nimbus.sh $@
