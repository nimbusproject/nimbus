#!/bin/bash

sz="2 4 8 16 32 64"

echo "cleaning up old stuff"
pdsh -R ssh -g murder64 rm /home/nimbus/new.test
tm_file=$1
for s in $sz
do
    #echo "making header"
    #pdsh -R ssh -g murder$s /sbin/ifconfig  | grep 'inet addr:172' | sed 's/.*inet addr://' | sed 's/Bcast:.*//' | awk '{ print $1 ":2893/home/nimbus/new.test"}' > header$s

    echo "running virga"
    /usr/bin/time --append -o $tm_file --format "v2 $s %e" ./run-client.sh header$s /home/nimbus/FILE
    echo "check sum"
    pdsh -R ssh -g murder$s md5sum /home/nimbus/new.test
    pdsh -R ssh -g murder$s ls -l /home/nimbus/new.test
    echo "cleaning up"
    pdsh -R ssh -g murder$s rm /home/nimbus/new.test
    cat $tm_file
done
