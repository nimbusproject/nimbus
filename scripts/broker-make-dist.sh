#!/bin/bash

if ([ "X$1" == "X--help" ] || [ "X$1" == "X-h" ]); then
    echo "Usage:"
    echo "broker-make-dist.sh [<destination directory>] [<git repository>]"
    echo "Default: make-dist . git://github.com/nimbusproject/nimbus.git"
    exit 0
fi

if [ "X$2" != "X" ]; then
#   normalize dir, otherwise it is relative to $co_dir
    dest_dir=`cd "$1";pwd`
else
    dest_dir=`pwd`
fi

if [ "X$2" != "X" ]; then
    repo=$2
else
    repo="git://github.com/nimbusproject/nimbus.git"
fi 

co_dir=`mktemp -d -t tmp.XXXXXXXXXX`

echo "Destination directory: $dest_dir"
echo "Nimbus git repository: $repo"
echo "Created temp directory: $co_dir"

cd $co_dir
git clone $repo

cd $co_dir/nimbus
ant -f scripts/lib/gt4.0/dist/build.xml clean
ant -f scripts/lib/gt4.0/brokerdist/build.xml clean-local
ant -f scripts/lib/gt4.0/brokerdist/build.xml create-dist
if [ $? -ne 0 ]; then
    echo "create-dist failed"
    exit 1
fi

ls scripts/lib/gt4.0/brokerdist/result/

cd scripts/lib/gt4.0/brokerdist/result/

cp *.tar.gz $dest_dir
if [ $? -ne 0 ]; then
    echo "copy failed"
    exit 1
fi

echo "Removing temp dir: $co_dir"
rm -rf $co_dir

echo "Finished. Tarballs copied to $dest_dir."
