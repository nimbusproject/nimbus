#!/bin/bash -x

# (you will probably want to change the URLs and edit the sample
#  files in $GLOBUS_LOCATION/share/nimbus-clients )

####  Example 12
####  127.0.0.1 :: image file already in local image store or accessible via
####               shared filesystem
####               readonly image partition in local image store or accessible
####               via shared filesystem
####               blankspace partition requested, 256M

$GLOBUS_LOCATION/bin/workspace --deploy --file workspace.epr \
  --metadata \
  $GLOBUS_LOCATION/share/nimbus-clients/sample-workspace-multiple-partitions.xml \
  --request \
  $GLOBUS_LOCATION/share/nimbus-clients/sample-deployment-request-with-blankspace.xml \
  --poll-delay 2000 \
  -s https://127.0.0.1:8443/wsrf/services/WorkspaceFactoryService
