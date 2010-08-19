#!/bin/bash

dir=`dirname $0`
cd $dir/..
pypath=`pwd`
if [ "X${PYTHONPATH}" == "X" ]; then
    export PYTHONPATH=$pypath
else
    export PYTHONPATH=$pypath:${PYTHONPATH}
fi
VIRGA_REQ_DB=$pypath/req.db

export NIMBUS_HOME=$pypath
echo $NIMBUS_HOME

exec python ./pyvirga/request.py "${@}"
