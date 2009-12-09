#!/bin/bash


SCRIPTDIR=$(cd $(dirname "$0"); pwd)
DOCROOTDIR=$(dirname $SCRIPTDIR) 

TEMPDIR=$DOCROOTDIR/tmp

# final output DIR:
BASE="/mcs/ee.mcs.anl.gov/nimbus/doc/2.2/"

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
rsync -crlv $DOCROOTDIR/html/ $BASE/

echo ""
echo "====================="
echo "| Group permissions |"
echo "====================="
echo ""

find $BASE -exec chown :vwtools {} \;
find $BASE -type d -exec chmod 775 {} \;
find $BASE -type f -exec chmod 664 {} \;

echo ""
echo "Done."
