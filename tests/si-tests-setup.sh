#!/bin/bash

NIMBUS_HOME=../
SI_CONF=conf/

echo "Copying boto fix files to NIMBUS_HOME/ve/lib/python2.6/site-packages/boto/ec2/" 

cp $SI_CONF/*.py $NIMBUS_HOME/ve/lib/python2.6/site-packages/boto/ec2/

echo "Copying elastic conf file to NIMBUS_HOME/ve/lib/python2.6/site-packages/boto/ec2/"

cp $SI_CONF/elastic.conf $NIMBUS_HOME/services/etc/nimbus/elastic/

echo "Done!"
