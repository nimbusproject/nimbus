#!/bin/bash -x

# (you will probably want to change the URLs and edit the sample
#  files in $GLOBUS_LOCATION/share/nimbus-clients )

####  Example 5
####  127.0.0.1 :: staging of image to an image repository before it is then
####               required to propagate to the appropriate VMM node (if you
####               are using the resource pool model, this node is not known
####               ahead of time -- and besides, it could be on a private
####               network and/or without grid staging infrastructure).

$GLOBUS_LOCATION//bin/workspace --deploy --file workspace.epr \
  --metadata $GLOBUS_LOCATION/share/nimbus-clients/sample-workspace-propagation.xml \
  --request  $GLOBUS_LOCATION/share/nimbus-clients/sample-deployment-request.xml \
  -s https://127.0.0.1:8443/wsrf/services/WorkspaceFactoryService \
  -o share/nimbus-clients/sample-optional-staging.xml \
  --poll-delay 2000 \
  -u https://127.0.0.1:8443/wsrf/services/DelegationFactoryService -q 10000 -t
