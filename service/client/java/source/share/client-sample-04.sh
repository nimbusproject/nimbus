#!/bin/bash -x

# (you will probably want to change the URLs and edit the sample
#  files in $GLOBUS_LOCATION/share/nimbus-clients )

####  Example 4
####  127.0.0.1 :: staging of image directly to the VMM is required

$GLOBUS_LOCATION//bin/workspace --deploy --file workspace.epr \
  --metadata $GLOBUS_LOCATION/share/nimbus-clients/sample-workspace.xml \
  --request  $GLOBUS_LOCATION/share/nimbus-clients/sample-deployment-request.xml \
  -s https://127.0.0.1:8443/wsrf/services/WorkspaceFactoryService \
  -o share/nimbus-clients/sample-optional-staging.xml \
  --poll-delay 2000 \
  -u https://127.0.0.1:8443/wsrf/services/DelegationFactoryService -q 10000 -t

