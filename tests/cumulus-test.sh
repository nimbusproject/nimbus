#!/bin/bash

source $NIMBUS_HOME/ve/bin/activate
cd $NIMBUS_HOME/cumulus
source env.sh
cd tests
./run-all.sh
exit $?
