#!/bin/bash

dest_dir=$1
co_dir=`mktemp -d -t tmp.XXXXXXXXXX`

if [ "X$2" != "X" ]; then
    repo=$2
else
    repo="git@github.com:nimbusproject/nimbus.git"
fi 

echo $co_dir
cd $co_dir
git clone $repo

cd nimbus/cumulus/deps
./get-em.sh
cd $co_dir/nimbus
ant -f scripts/lib/gt4.0/dist/build.xml create-dist

ls scripts/lib/gt4.0/dist/result/
cp scripts/lib/gt4.0/dist/result/*.tar.gz $dest_dir

echo $co_dir
#rm -rf $co_dir
