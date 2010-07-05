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

# This script adjusts ebtables rules to prevent MAC and IP spoofing.
# Unlike the 'main' ebtables-config script used with Xen, this will NOT allow
# you to stop DHCP packets from workspaces escaping to the site network.

# 1. Is the packet coming from a workspace virtual interface?
# 2. If not, proceed without further processing.
# 3. If so, is the MAC address incorrect?  Drop the packet.
# 4. Is this is a DHCP packet?
# 5. If so, allow it to be bridged.
# 6. If not a DHCP packet, it must have the correct source IP address,
#    otherwise the packet is dropped.


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
if [ ! -f $FLOCK ]; then
  echo "*** can not find flock program, disabling"
  echo "*** disabling flock might result in a error like \"kernel doesn't support a certain ebtables extension\""
  FLOCK=/bin/true
fi


#############
# ARGUMENTS #
#############

if [ $# -lt 2 ]; then
  echo "ERROR: requires at least 2 arguments, syntax: add|rem <vifname> [ <macaddr> <ipaddr> <dhcpif> ]"
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
  if [ $# -ne 4 ] && [ $# -ne 5 ]; then
    echo "ERROR: add requires 4 or 5 arguments: add <vifname> <macaddr> <ipaddr> [<dhcpif>]" 
    exit 1
  else
    MACADDR=$3
    echo "     macaddr: $MACADDR"
    IPADDR=$4
    echo "      ipaddr: $IPADDR"
    DHCPIF=$5
    echo "     dhcpif (ignored): $DHCPIF"
  fi
fi

if [ "$ADDREM" = "rem" ] && [ $# -ne 2 ]; then
  echo "ERROR: rem requires just 2 arguments: rem <vifname>"
  exit 1
fi


###################################
#### EBTABLES CHAINS AND RULES ####
###################################

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

  # If this is a DHCP request, accept it
  $EBTABLES -A $VIFNAME -p IPv4 --ip-proto 17 --ip-dport 67 -j ACCEPT
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

function add_input_rule() {
  $EBTABLES -A INPUT -i $VIFNAME -j $VIFNAME
  return $?
}

function rem_input_rule() {
  $EBTABLES -D INPUT -i $VIFNAME -j $VIFNAME
  return $?
}


##########################
#### SUBCOMMAND IMPLS ####
##########################
(
$FLOCK -x 200

if [ "$ADDREM" = "rem" ]; then

  SUCCESS="y"
  rem_input_rule
  if [ $? -ne 0 ]; then
    echo "ERROR: Failed to remove $VIFNAME INPUT rule"
    SUCCESS="n"
  else
    echo "Removed $VIFNAME INPUT rule"
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

  init_vifname_chain
  if [ $? -ne 0 ]; then
    exit 1
  else
    echo "Created $VIFNAME chain"
  fi

  add_input_rule
  if [ $? -ne 0 ]; then
    echo "ERROR: Failed to add $VIFNAME INPUT rule"
    exit 1
  else
    echo "Added $VIFNAME INPUT rule"
    exit 0
  fi
fi

) 200>$FLOCKFILE
