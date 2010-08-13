#!/bin/bash

cd $CLOUD_CLIENT_HOME
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
./bin/cloud-client.sh --run --hours .2 --name group 
if [ $? -ne 0 ]; then
    echo "run failed"
    exit 1
fi

exit 0

