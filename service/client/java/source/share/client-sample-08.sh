#!/bin/bash -x

# (you will probably want to change the URLs and edit the sample
#  files in $GLOBUS_LOCATION/share/nimbus-clients )

####  Example 8
####  Query the factory resource properties

$GLOBUS_LOCATION/bin/workspace --factoryrp \
  -s https://127.0.0.1:8443/wsrf/services/WorkspaceFactoryService
