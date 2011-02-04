#!/bin/bash

bd=`dirname $0`
cd $bd
src_dir=`pwd`

tar_dir=`mktemp --tmpdir=$src_dir -d`

cd $tar_dir
$src_dir/../scripts/make-dist.sh

echo "building client now"
bash ../../cloud-client/builder/get-wscore.sh
cc_log=`mktemp`
bash ../../cloud-client/builder/dist.sh | tee $cc_log

cc_tar=`grep "Creating dist tar.gz:" $cc_log | sed 's/Creating dist tar.gz://'`
mv $cc_tar .
rm $cc_log

for t in *.tar.gz
do
    tar -zxvf $t
done
ls -l
pwd
NIMBUS_SRC_DIR=`ls -d nimbus-*-src.tar.gz | sed s'/\.tar.gz//g'`
NIMBUS_WSC_SRC_DIR=`ls -d nimbus-controls*.tar.gz | sed s'/\.tar.gz//g'`
NIMBUS_CC_DIR=`ls -d nimbus-cloud-client*.tar.gz | sed s'/\.tar.gz//g'`

export NIMBUS_SRC_DIR=`pwd`/$NIMBUS_SRC_DIR
export NIMBUS_WSC_SRC_DIR=`pwd`/$NIMBUS_WSC_SRC_DIR
export NIMBUS_CC_DIR=`pwd`/$NIMBUS_CC_DIR

echo $NIMBUS_SRC_DIR
echo $NIMBUS_WSC_SRC_DIR
echo $NIMBUS_CC_DIR

cd $src_dir
./bt-nimbus.sh "${@}"
rc=$?
echo "${@}"
exit $rc
