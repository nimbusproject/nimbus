#!/bin/bash

port=$2
userhost=$3
remoteexe=$4
remotepath=$5
localpath=$6
rid=$7
ltcs=$8

ssh "$@"
rc=$?

if [ $rc -eq 0 ]; then
    md5sum $localpath
fi
exit $rc
