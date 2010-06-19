#!/bin/bash

BASEDIR=`dirname $0`

PYTHONPATH="$BASEDIR/lib/:$BASEDIR/lib/pylib:$PYTHONPATH"
export PYTHONPATH

# This loops forever until there is a network address on one of the
# configured interfaces, see at the top of the "wait_for_network.py"
# file to change the options (currently looks for something on either
# eth0 and eth1).

python $BASEDIR/lib/wait_for_network.py 


# The larger your cluster is going to be, the higher "--polltime" should
# probably be.  The context broker will tell VMs to "come back later"
# if all of the VM's information is not known.  This "--polltime" is the
# delay of the agent before it goes back to re-query. 

python $BASEDIR/lib/nimbus_ctx_retrieve.py -c $BASEDIR/ctx.conf --polltime 2 -z -t

