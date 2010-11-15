#!/bin/bash

NIMBUS_HOME=../
SI_CONF=conf/

echo "Copying boto fix files to NIMBUS_HOME/ve/lib/python2.6/site-packages/boto/ec2/" 

cp $SI_CONF/*.py $NIMBUS_HOME/ve/lib/python2.6/site-packages/boto/ec2/

echo "Copying resource pool conf file to NIMBUS_HOME/services/etc/nimbus/workspace-service/vmm-pools/"

cp $SI_CONF/pool1 $NIMBUS_HOME/services/etc/nimbus/workspace-service/vmm-pools/

echo "Copying network pool conf file to NIMBUS_HOME/services/etc/nimbus/workspace-service/network-pools/"

cp $SI_CONF/public $NIMBUS_HOME/services/etc/nimbus/workspace-service/network-pools/

echo "Done!"
