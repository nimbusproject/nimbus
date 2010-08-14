#!/bin/bash

cd $NIMBUS_HOME/cumulus
source env.sh
cd tests
./run-all.sh
exit $?
