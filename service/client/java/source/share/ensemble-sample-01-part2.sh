#!/bin/bash -x

# (you will probably want to change the URLs and edit the sample
#  files in $GLOBUS_LOCATION/share/nimbus-clients )

# Run this AFTER running ensemble-sample-01-part1.sh, because it uses the
# created ensemble1-whole-ensemble.epr file from part1.sh

####  127.0.0.1 :: propagation not required, different image, duration, and
####               memory than in part1 of the ensemble

$GLOBUS_LOCATION/bin/workspace --deploy --file ensemble1-group2 \
  --metadata $GLOBUS_LOCATION/share/nimbus-clients/sample-workspace.xml \
  -s https://127.0.0.1:8443/wsrf/services/WorkspaceFactoryService \
  --deploy-duration 60 --deploy-mem 2048 --deploy-state Running \
  --join-ensemble ensemble1-whole-ensemble.epr --last-in-ensemble \
  --trash-at-shutdown \
  --poll-delay 2000 \
  --numnodes 2 --terse-group-subscribe --groupfile ensemble1-group2.epr
