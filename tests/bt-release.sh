#!/bin/bash

bd=`dirname $0`
cd $bd
src_dir=`pwd`

tar_dir=`mktemp --tmpdir=$src_dir -d`

cd $tar_dir
$src_dir/../scripts/make-dist.sh

echo "building client now"
bash ../../cloud-client/builder/get-wscore.sh
bash ../../cloud-client/builder/dist.sh

ls -l
pwd
#NIMBUS_SRC_DIR=
#NIMBUS_WSC_SRC_DIR=
#NIMBUS_CC_DIR=

