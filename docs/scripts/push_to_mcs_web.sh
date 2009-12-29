#!/bin/bash


SCRIPTDIR=$(cd $(dirname "$0"); pwd)
DOCROOTDIR=$(dirname $SCRIPTDIR) 

TEMPDIR=$DOCROOTDIR/tmp

# group to chown everything to
UNIXGROUP="vwtools"

# final output DIR:
BASE="/mcs/ee.mcs.anl.gov/nimbus/doc"

if [ ! -d "$BASE" ]; then
    echo "Base directory '$BASE' does not exist."
    exit 1
fi

TARGET=""
if [ $# = 1 ]; then
    TARGET="$BASE/$1"
else 
    TARGET="$BASE/dev/"
fi

if [ ! -d "$TARGET" ]; then
    echo "Target directory '$TARGET' does not exist."
    exit 1
fi

echo ""
echo "================="
echo "| m4 processing |"
echo "================="
echo ""

/usr/bin/python $SCRIPTDIR/process_m4.py -i $DOCROOTDIR/src -o $DOCROOTDIR/html -l $DOCROOTDIR/m4/worksp.lib.m4 -t $TEMPDIR

echo ""
echo "=================="
echo "| New or updated |"
echo "=================="
echo ""

# use checksums only for comparison
rsync -crlv $DOCROOTDIR/html/ $TARGET/

echo ""
echo "====================="
echo "| Group permissions |"
echo "====================="
echo ""

find $TARGET -exec chown :$UNIXGROUP {} \;
find $TARGET -type d -exec chmod 775 {} \;
find $TARGET -type f -exec chmod 664 {} \;

echo ""
echo "Done."
