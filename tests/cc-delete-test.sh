#!/bin/bash

backf=`mktemp`
rm $backf
cd $CLOUD_CLIENT_HOME
./bin/grid-proxy-init.sh
./bin/cloud-client.sh --transfer --sourcefile /etc/group
if [ $? -ne 0 ]; then
    rm $backf
    echo "upload failed"
    exit 1
fi
./bin/cloud-client.sh --delete --name group
if [ $? -ne 0 ]; then
    rm $backf
    echo "delete failed"
    exit 1
fi
./bin/cloud-client.sh --download --name group --localfile $backf
if [ $? -eq 0 ]; then
    rm $backf
    echo "download should have failed"
    exit 1
fi

diff $backf /etc/group
if [ $? -ne 0 ]; then
    rm $backf
    echo "diff failed, file corrupted in transfer"
    exit 1
fi

rm $backf
exit 0

