#!/bin/bash

dir=`dirname $0`
cd $dir/..
source env.sh

exec ./pycb/tools/add_user.py "${@}"
