#!/bin/bash

histdir=`mktemp -d`
cmd="/home/bresnaha/nimbus-cloud-client-016/bin/cloud-client.sh --name lenny-vm.raw --hours .1 --run --history-dir $histdir"
$cmd
rm -rf $histdir

