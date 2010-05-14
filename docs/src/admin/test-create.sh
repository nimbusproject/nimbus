#!/bin/bash

$GLOBUS_LOCATION/bin/grid-proxy-init

$GLOBUS_LOCATION/bin/workspace \
    -z none \
    --poll-delay 200 \
    --deploy \
    --file test.epr \
    --metadata $GLOBUS_LOCATION/share/nimbus-clients/sample-workspace.xml \
    -s https://127.0.0.1:8443/wsrf/services/WorkspaceFactoryService \
    --deploy-duration 30 --deploy-mem 256 --deploy-state Running
