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
./bin/cloud-client.sh --download --name group --localfile $backf
if [ $? -ne 0 ]; then
    rm $backf
    echo "download failed"
    exit 1
fi

diff $backf /etc/group
if [ $? -ne 0 ]; then
    rm $backf
    echo "diff failed, file corrupted in transfer"
    exit 1
fi

./bin/cloud-client.sh --delete --name group --localfile $backf
rm $backf
exit 0

