#!/bin/bash

bucket=s3://CumulusTest$RANDOM
s3cmd mb $bucket
s3cmd ls $bucket
if [ "X$?" != "X0" ]; then
    echo "ERROR"
    exit 1
fi
s3cmd rb $bucket
if [ "X$?" != "X0" ]; then
    echo "ERROR"
    exit 1
fi

exit 0
