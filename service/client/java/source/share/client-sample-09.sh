#!/bin/bash -x

# (you will probably want to change the URLs and edit the sample
#  files in $GLOBUS_LOCATION/share/nimbus-clients )

####  Example 9
####  Subscribe to a previously deployed workspace for state notifications

$GLOBUS_LOCATION/bin/workspace --e workspace.epr --subscribe --poll-delay 2000
