#!/bin/bash -x

# (you will probably want to change the URLs and edit the sample
#  files in $GLOBUS_LOCATION/share/nimbus-clients )

####  Example 2
####  127.0.0.1 :: same as example 1, but uses a deployment request file
####               instead of specifying the resource request on the cmdline

$GLOBUS_LOCATION/bin/workspace --deploy --file workspace.epr \
  --metadata $GLOBUS_LOCATION/share/nimbus-clients/sample-workspace.xml \
  --request  $GLOBUS_LOCATION/share/nimbus-clients/sample-deployment-request.xml \
  --poll-delay 2000 \
  -s https://127.0.0.1:8443/wsrf/services/WorkspaceFactoryService
