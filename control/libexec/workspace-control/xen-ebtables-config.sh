#!/bin/bash

# Copyright 1999-2010 University of Chicago
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy
# of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

# For more information see: http://www.nimbusproject.org


#########
# ABOUT #
#########

# This script adjusts ebtables rules to stop DHCP packets from workspaces
# escaping to the site network and to prevent MAC and IP spoofing.

# 1. Is the packet coming from a workspace virtual interface?
# 2. If not, proceed without further processing.
# 3. If so, is the MAC address incorrect?  Drop the packet.
# 4. Is this is a DHCP packet?
# 5. If so, allow it to be bridged only to the appropriate interface
#    for the bridge that it is on.  No other interface on the bridge
#    will see the broadcast packet.
# 6. If not a DHCP packet, it must have the correct source IP address,
#    otherwise the packet is dropped.


# If you are running a site network DHCP server (and even if not it is good
# practice), you should add an ebtables rule to each system that drops any DHCP
# request packets coming from the peth* interfaces.
# TODO: example rule


#######################
# ADJUST AS NECESSARY #
#######################

# NOTE: The ebtables path is configured here in order to make sure that if
#       someone compromises the privileged account, it cannot use the sudo
#       privilige of running this program to do more than desired.

PATH=/bin:/usr/bin:/sbin
export PATH

EBTABLES=ebtables
#EBTABLES=/sbin/ebtables
#EBTABLES=/usr/sbin/ebtables

FLOCKFILE=/var/lock/ebtables.config.lock
FLOCK=/usr/bin/flock
if [ ! -O $FLOCK ]; then
  echo "*** can not find flock program, disabling"
  echo "*** disabling flock might result in a error like \"kernel doesn't support a certain ebtables extension\""
  FLOCK=/bin/true
fi


#############
# ARGUMENTS #
#############

if [ $# -lt 2 ]; then
  echo "ERROR: requires at least 2 arguments, syntax: add|rem <vifname> [<macaddr> <ipaddr> [<dhcpif>]]"
  exit 1
fi

ADDREM=$1
echo "  subcommand: $ADDREM"
VIFNAME=$2
echo "     vifname: $VIFNAME"

if [ "$ADDREM" != "add" ] && [ "$ADDREM" != "rem" ]; then
  echo "ERROR: subcommand must be 'add' or 'rem'"
  exit 1
fi

if [ "$ADDREM" = "add" ]; then
  if [ $# -ne 4 ] && [ $# -ne 5  ]; then
    echo "ERROR: add requires 4 or 5 arguments: add <vifname> <macaddr> <ipaddr> [<dhcpif>]"
    exit 1
  else
    MACADDR=$3
    echo "     macaddr: $MACADDR"
    IPADDR=$4
    echo "      ipaddr: $IPADDR"
    DHCPIF=$5
    echo "      dhcpif: $DHCPIF"
  fi
fi

if [ "$ADDREM" = "rem" ] && [ $# -ne 2 ]; then
  echo "ERROR: rem requires just 2 arguments: rem <vifname>"
  exit 1
fi


###################################
#### EBTABLES CHAINS AND RULES ####
###################################


# function init_dhcpif_chain
#
# This chain is the target for any workspace on the bridge that is being
# served by a DHCP server in dom0 with a listening interface on the subnet.
#
# The overall logic is: if this is a workspace NIC and if this is a DHCP
# request (these two truths are the only way to get packets to this chain),
# then send it only to the right dom0 interface (or another dom if the dhcp
# server is running in another domain).  DROP every other port on the
# bridge, the DHCP request will not go to any other workspace or out
# to the site's network.

function init_dhcpif_chain() {
  $EBTABLES -N DHCP-$DHCPIF
  if [ $? -ne 0 ]; then
    return 1
  fi
  $EBTABLES -A DHCP-$DHCPIF -o $DHCPIF -j ACCEPT
  if [ $? -ne 0 ]; then
    echo "ERROR: could not add $DHCPIF -o rule"
    return 2
  fi
  $EBTABLES -P DHCP-$DHCPIF DROP
  if [ $? -ne 0 ]; then
    echo "ERROR: could not set default policy for DHCP-$DHCPIF chain"
    return 2
  fi
  return 0
}


# function init_vifname_chain
#
# This chain is the target that all packets from a workspace NIC are
# sent.  All packets must have the correct source MAC address.  Any
# DHCP request must be sent to the appropriate dhcpif chain.  If it
# is not a DHCP request, it must have the correct source IP address.

function init_vifname_chain() {
  $EBTABLES -N $VIFNAME
  if [ $? -ne 0 ]; then
    echo "ERROR: could not create $VIFNAME chain"
    return 1
  fi

  $EBTABLES -P $VIFNAME ACCEPT
  if [ $? -ne 0 ]; then
    echo "ERROR: could not set default policy for $VIFNAME chain"
    return 1
  fi

  # First make sure it is using the right MAC address
  $EBTABLES -A $VIFNAME -s ! $MACADDR -j DROP
  if [ $? -ne 0 ]; then
    echo "ERROR: could not set MAC address policy for $VIFNAME chain"
    return 1
  fi

  # If this is a DHCP request, send it to the right handler
  # if dhcpif was specified, expect a chain to exist and send it there
  # otherwise, general accept to the normal bridge
  if [ "X$DHCPIF" != "X" ]; then
      $EBTABLES -A $VIFNAME -p IPv4 --ip-proto 17 --ip-dport 67 -j DHCP-$DHCPIF
  else
      $EBTABLES -A $VIFNAME -p IPv4 --ip-proto 17 --ip-dport 67 -j ACCEPT
  fi
  if [ $? -ne 0 ]; then
    echo "ERROR: could not set DHCP policy for $VIFNAME chain"
    return 1
  fi

  # Make sure it using the right IP address (this needs to be after the
  # DHCP handler branch since those packets do not have IPs yet).
  $EBTABLES -A $VIFNAME -p IPv4 --ip-src ! $IPADDR -j DROP
  if [ $? -ne 0 ]; then
    echo "ERROR: could not set IP policy for $VIFNAME chain"
    return 1
  fi

  return 0
}


function delete_vifname_chain() {
  $EBTABLES -X $VIFNAME
  return $?
}

function add_forward_rule() {
  $EBTABLES -A FORWARD -i $VIFNAME -j $VIFNAME
  return $?
}

function rem_forward_rule() {
  $EBTABLES -D FORWARD -i $VIFNAME -j $VIFNAME
  return $?
}


##########################
#### SUBCOMMAND IMPLS ####
##########################
(
$FLOCK -x 200

if [ "$ADDREM" = "rem" ]; then

  SUCCESS="y"
  rem_forward_rule
  if [ $? -ne 0 ]; then
    echo "ERROR: Failed to remove $VIFNAME FORWARD rule"
    SUCCESS="n"
  else
    echo "Removed $VIFNAME FORWARD rule"
  fi

  delete_vifname_chain
  if [ $? -ne 0 ]; then
    echo "ERROR: Failed to delete $VIFNAME chain"
    SUCCESS="n"
  else
    echo "Deleted $VIFNAME chain"
  fi
  
  if [ "$SUCCESS" = "n" ]; then
    exit 1
  fi
  exit 0
fi


if [ "$ADDREM" = "add" ]; then

  if [ "X$DHCPIF" != "X" ]; then
    # error 1 is ignored, DHCPIF chain in place already (could write
    # func to test for that).  If there is a general ebtables problem
    # func after this will also fail.
    init_dhcpif_chain
    if [ $? -eq 2 ]; then
      echo "ERROR: could not create $DHCPIF chain"
      exit 1
    fi
  fi

  init_vifname_chain
  if [ $? -ne 0 ]; then
    exit 1
  else
    echo "Created $VIFNAME chain"
  fi

  add_forward_rule
  if [ $? -ne 0 ]; then
    echo "ERROR: Failed to add $VIFNAME FORWARD rule"
    exit 1
  else
    echo "Added $VIFNAME FORWARD rule"
    exit 0
  fi
fi

) 200>>$FLOCKFILE
