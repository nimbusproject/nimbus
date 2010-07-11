#!/bin/bash

# Script that allows you to configure workspace-control to fake doing
# privileged work.
#
# What is left is mainly propagation and service notification.
#
# This allows you to experiment with propagation techniques on any cluster
# where you have access to a single unix account with passwordless SSH
# access from node to node on that account.  The cluster doesn't need to
# support virtualization at all and you don't need special account
# privileges.
#
# This is of course also for convenience when developing or testing
# propagation.  It is useful even if the cluster supports virtualization
# and you have sudo access for privileged things: you don't need to set
# anything up, you can drop workspace-control in and go (as long as it
# has the configuration that this script sets up).
#
# This does two things.
#
# 1. The sudo configuration will become "/bin/true"
#
#    So all commands that do privileged work will "succeed" but not
#    make any changes.
#
# 2. Configures the platform module as the DoNothingPlatform module
#  
#    The unit tests and other mock modes take advantage of the libvirt 
#    'mock' driver which is great for many purposes and what should be
#    used to do accurate VMM mocking.
#   
#    What this new DoNothingPlatform module does is bypass libvirt
#    altogether, allowing the propagation-only mode to run on libvirt-less
#    clusters and/or non-privileged accounts.  Basically reducing the
#    dependencies of propagation research and development to the bare
#    minimum.
#
# Those two things are actually not mutually exclusive, just put together
# by default.  You can change this to just do #2 in order to mess with
# networking settings and mount+alter without needing to have libvirt
# around, for example.
#
# Note that this works fine localhost as well, for developing on laptops.
# Make another account (e.g. "justpropagate"), set it up as owner of all
# /opt/nimbus workspace-control files, and set up localhost passwordless
# SSH access.   Then make sure to answer the account name and hostname
# questions correctly in the setup process ("nimbus-configure --autoconfig").


BASEDIR_REL="`dirname $0`/.."
BASEDIR=`cd $BASEDIR_REL; pwd`
ETCDIR="$BASEDIR/etc/workspace-control"

BAK_SUDO="$ETCDIR/real_sudo.conf"
REAL_SUDO="$ETCDIR/sudo.conf"
BAK_INTERNAL="$ETCDIR/real_internal.conf"
REAL_INTERNAL="$ETCDIR/internal.conf"

if [ -e $BAK_SUDO ] ; then
    echo "Cannot continue, backup file exists already: $BAK_SUDO"
    exit 1
fi

if [ -e $BAK_INTERNAL ] ; then
    echo "Cannot continue, backup file exists already: $BAK_INTERNAL"
    exit 1
fi

CMD="cp $REAL_SUDO $BAK_SUDO"
$CMD
if [ $? -ne 0 ]; then
    echo "Failed: $CMD"
    exit 1
fi

CMD="cp $REAL_INTERNAL $BAK_INTERNAL"
$CMD
if [ $? -ne 0 ]; then
    echo "Failed: $CMD"
    exit 1
fi



# All commands that do privileged work will "succeed" but not affect any
# change.

echo "
[sudo]
# do nothing, pretend to succeed
sudo: /bin/true
" > $REAL_SUDO



# Replace platform module with something that does nothing.  Usually the
# "mock" libvirt driver is used for many unit tests, etc., but having this
# platform module that does nothing at all means you don't even have to
# have libvirt installed in the first place.

sed 's/workspacecontrol.defaults.lvrt.Platform/workspacecontrol.mocks.DoNothingPlatform/' $BAK_INTERNAL > $REAL_INTERNAL




