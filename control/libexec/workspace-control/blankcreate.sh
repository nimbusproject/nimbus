#!/bin/bash

#############
# ARGUMENTS #
#############

if [ $# -lt 3 ]; then
  echo "ERROR: requires 2 arguments. Syntax: $0 <file> <megabytes> <label>"
  exit 1
fi

FILE=$1
echo "  blank partition:  $FILE"
MEGS=$2
echo "      size (megs):  $MEGS"
LABEL=$3
echo "            label:  $LABEL"

CMD="dd if=/dev/zero of=$FILE bs=1M seek=$MEGS count=1"
echo "          running:  $CMD"

$CMD
if [ $? -ne 0 ]; then
  echo "ERROR: Failed to make sparse image"
  exit 1
fi

CMD="/sbin/mke2fs -F -L $LABEL $FILE"
echo "          running:  $CMD"

$CMD
if [ $? -ne 0 ]; then
  echo "ERROR: Failed to make filesystem"
  exit 1
else
  exit 0
fi
