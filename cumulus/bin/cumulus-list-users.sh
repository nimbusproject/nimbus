#!/bin/bash

dir=`dirname $0`
cd $dir/..
source env.sh

exec $CUMULUS_VE_HOME/bin/python pycb.tools.list_users.py "${@}"
