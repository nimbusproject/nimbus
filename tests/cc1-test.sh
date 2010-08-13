#!/bin/bash

cd $CLOUD_CLIENT_HOME
./bin/grid-proxy-init.sh
./bin/cloud-client.sh --transfer --sourcefile /etc/group
if [ $? -ne 0 ]; then
    echo "upload failed"
    exit 1
fi
./bin/cloud-client.sh --list
if [ $? -ne 0 ]; then
    echo "list failed"
    exit 1
fi
handle_line=`./bin/cloud-client.sh --run --hours .2 --name group | tail -n 1`
if [ $? -ne 0 ]; then
    echo "run failed"
    exit 1
fi

echo $handle_line
handle=`echo $handle_line | sed  "s/.*Running: //" | sed "s/'//g"`
echo "++"
echo $handle
./bin/cloud-client.sh --terminate --handle $handle
if [ $? -ne 0 ]; then
    echo "terminate failed"
    exit 1
fi

exit 0

