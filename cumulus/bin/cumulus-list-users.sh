#!/bin/bash

dir=`dirname $0`
cd $dir/..
source env.sh

./pycb/tools/list_users.py "${@}"
