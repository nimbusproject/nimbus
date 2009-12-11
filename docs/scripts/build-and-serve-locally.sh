#!/bin/bash

THISDIR_REL="`dirname $0`"
THISDIR=`cd $THISDIR_REL; pwd`

MACRO_PROGRAM="$THISDIR/process_m4.py"
MACRO_EXE="python"

SERVE_PROGRAM="$THISDIR/local-serve.py"
SERVE_EXE="python"


if [ ! -f "$MACRO_PROGRAM" ]; then
  echo "Cannot find macro processing script, exiting"
  exit 1
fi

if [ ! -f "$SERVE_PROGRAM" ]; then
  echo "Cannot find web serving script, exiting"
  exit 1
fi

  echo ""
  echo "====================="
  echo "| Building website: |"
  echo "====================="
  echo ""

(cd $THISDIR ; $MACRO_EXE $MACRO_PROGRAM)

echo ""
OUTPUT_DIR=$THISDIR/../html
OUTPUT_DIR2=`cd $OUTPUT_DIR; pwd`
echo "Website built @ $OUTPUT_DIR2"
echo ""
echo ""
echo "===================="
echo "| Serving website: |"
echo "===================="
echo ""

(cd $OUTPUT_DIR2 ; $SERVE_EXE $SERVE_PROGRAM)

