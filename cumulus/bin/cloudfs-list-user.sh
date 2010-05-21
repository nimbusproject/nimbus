#!/bin/bash

dir=`dirname $0`
cd $dir/..
source env.sh

./pynimbusauthz/list_user.py "${@}"
