#!/bin/bash -x

# (you will probably want to change the URLs and edit the sample
#  files in $GLOBUS_LOCATION/share/nimbus-clients )

####  Example 1
####  127.0.0.1 :: image file already in local image store or accessible via
####               shared filesystem

$GLOBUS_LOCATION/bin/workspace --deploy --file workspace \
  --metadata $GLOBUS_LOCATION/share/nimbus-clients/sample-workspace.xml \
  -s https://127.0.0.1:8443/wsrf/services/WorkspaceFactoryService \
  --deploy-duration 30 --deploy-mem 256 --deploy-state Running \
  --poll-delay 2000 \
  --numnodes 5 --groupfile workspace-group.epr
