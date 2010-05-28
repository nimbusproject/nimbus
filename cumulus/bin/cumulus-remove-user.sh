#!/bin/bash

dir=`dirname $0`
cd $dir/..
source env.sh

./pycb/tools/remove_user.py "${@}"
