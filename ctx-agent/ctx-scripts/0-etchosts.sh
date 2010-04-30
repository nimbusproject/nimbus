#!/bin/bash

# This configures the local /etc/hosts file with all members of the context.
# Don't change or delete this script unless you know what you are doing.

echo ""
echo "etchosts script"
echo "IP: $1"
echo "Short hostname: $2"
echo "Hostname: $3"

echo "$1   $3 $2" >> /etc/hosts


exit 0


