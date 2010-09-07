#!/bin/bash

if [ "X$NIMBUS_PRINTNAME" = "X" ]; then
  echo "no definition for NIMBUS_PRINTNAME"
  exit 1
fi
if [ "X$NIMBUS_ANT_CMD" = "X" ]; then
  echo "no definition for NIMBUS_ANT_CMD"
  exit 1
fi

BANNER="Nimbus -"

echo ""
echo "*** $BANNER $NIMBUS_PRINTNAME:"

if [ "X$NIMBUS_EXTRAPRINT" != "X" ]; then
  echo "$NIMBUS_EXTRAPRINT"
fi

THISDIR_REL="`dirname $0`"
THISDIR=`cd $THISDIR_REL; pwd`
ANTFILE="$THISDIR/build.xml"

ant -q -f $ANTFILE $NIMBUS_ANT_CMD
RET=$?

if [ $RET -eq 0 ]; then
    echo "Successful: $BANNER $NIMBUS_PRINTNAME"
else
    echo "PROBLEM: exit code $RET - $BANNER $NIMBUS_PRINTNAME"
fi

unset NIMBUS_ANT_CMD
unset NIMBUS_PRINTNAME
unset NIMBUS_EXTRAPRINT

exit $RET
