#!/bin/bash -x

# (you will probably want to change the URLs and edit the sample
#  files in $GLOBUS_LOCATION/share/nimbus-clients )

# Run this BEFORE running ensemble-sample-01-part2.sh

####  127.0.0.1 :: propagation required, different image, duration, and memory
####               than in part2 of the ensemble

$GLOBUS_LOCATION/bin/workspace --deploy --file ensemble1-group1 \
  --metadata $GLOBUS_LOCATION/share/nimbus-clients/sample-workspace-propagation.xml \
  -s https://127.0.0.1:8443/wsrf/services/WorkspaceFactoryService \
  --deploy-duration 30 --deploy-mem 1024 --deploy-state Running \
  --new-ensemble ensemble1-whole-ensemble.epr \
  --trash-at-shutdown \
  --poll-delay 2000 \
  --numnodes 5 --terse-group-subscribe --groupfile ensemble1-group1.epr
