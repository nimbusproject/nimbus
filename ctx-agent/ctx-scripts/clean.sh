#!/bin/bash

# You can put commands that 'reset' the VM for contextualization
# here and then call this before launch.sh in your rc.local (like
# the sample clusters do).

cp /etc/hosts.clean /etc/hosts
cp /etc/exports.clean /etc/exports

cat /dev/null > /etc/hosts.equiv

# wipe the torque nodes file, ipandhost script will fill it with all 
# compute nodes
cat /dev/null > /var/spool/torque/server_priv/nodes

rm -f /root/this_node_is_torque_master

cat /dev/null > /opt/nimbus/ctxlog.txt
rm -rf /opt/nimbus/ctx/tmp/  
mkdir /opt/nimbus/ctx/tmp/

