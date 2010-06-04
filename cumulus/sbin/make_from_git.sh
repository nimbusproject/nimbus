#! /bin/bash

source_dir=`dirname $0`
cd $source_dir
cd ..

rm -rf dist
python setup.py sdist

cd dist
tar -zxvf *.tar.gz
cd cumulus*
pwd

./install.sh $1
