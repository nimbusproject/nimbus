#!/bin/bash -x

# (you will probably want to change the URLs and edit the sample
#  files in $GLOBUS_LOCATION/share/nimbus-clients )

####  Example 10
####  Destroy a workspace -- this will force anything that is happening to
####  to be cancelled and any propagated images associated with it on the
####  VMM node will be deleted.

$GLOBUS_LOCATION/bin/workspace --e workspace.epr --destroy
