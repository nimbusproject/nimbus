#!/bin/bash

if ([ "X$1" == "X--help" ] || [ "X$1" == "X-h" ]); then
    echo "Usage:"
    echo "make-dist.sh [<destination directory>] [<git repository>]"
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
git clone --depth 1 $repo

cd nimbus/cumulus/deps
./get-em.sh
cd $co_dir/nimbus
ant -f scripts/lib/gt4.0/brokerdist/build.xml clean
ant -f scripts/lib/gt4.0/dist/build.xml clean-local
ant -f scripts/lib/gt4.0/dist/build.xml create-dist
if [ $? -ne 0 ]; then
    echo "create-dist failed"
    exit 1
fi

ls scripts/lib/gt4.0/dist/result/

git_hash=`git rev-parse HEAD`
if [ $? -ne 0 ]; then
    echo "rev-parse failed"
    exit 1
fi

cd scripts/lib/gt4.0/dist/result/
nimbus_src=`ls nimbus-*-src.tar.gz`

cd $co_dir/nimbus

python home/libexec/nimbus_version.py --tar $nimbus_src > .nimbusversion
if [ $? -ne 0 ]; then
    echo "could not determine Nimbus version"
    exit 1
fi

echo "commit: $git_hash" >> .nimbusversion

build_time=`date --utc +%Y-%m-%d_%H.%M.%S`
build_time=UTC-${build_time}
echo "buildtime: $build_time" >> .nimbusversion

cd scripts/lib/gt4.0/dist/result/
gunzip $nimbus_src
if [ $? -ne 0 ]; then
    echo "gunzip failed"
    exit 1
fi

tar_file=`ls *.tar`
echo "adjusting tar file: $tar_file"

tardirname=`echo $tar_file | sed -e 's/.tar//g'`

mkdir -p $tardirname/home/libexec
mv $co_dir/nimbus/.nimbusversion $tardirname/home/libexec/
if [ $? -ne 0 ]; then
    echo "nimbusversion move failed"
    exit 1
fi

tar -r -f $tar_file $tardirname/home/libexec/
if [ $? -ne 0 ]; then
    echo "tar adjustment failed"
    exit 1
fi

echo "gzipping: $tar_file"
gzip --best $tar_file
if [ $? -ne 0 ]; then
    echo "regzip failed"
    exit 1
fi

cp *.tar.gz $dest_dir
if [ $? -ne 0 ]; then
    echo "copy failed"
    exit 1
fi

echo "Removing temp dir: $co_dir"
rm -rf $co_dir

echo "Finished. Tarballs copied to $dest_dir."
