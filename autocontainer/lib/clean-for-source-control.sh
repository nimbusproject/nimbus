#!/bin/bash -x

THISDIR="`dirname $0`"
if [ ! -f $THISDIR/common-env.sh ]; then
  echo "Failure, cannot find environment definitions"
  exit 1
fi
source $THISDIR/common-env.sh

if [ ! -d $TARGETDIR ]; then
  echo "No TARGETDIR to remove: $TARGETDIR"
else
  echo "Removing TARGETDIR: $TARGETDIR"
  rm -rf $TARGETDIR
fi

if [ ! -d $BASEDIR/$DOWNLOADS_RELDIR ]; then
  echo "No download directory to remove: $BASEDIR/$DOWNLOADS_RELDIR"
else
  echo "Removing download directory: $BASEDIR/$DOWNLOADS_RELDIR"
  rm -rf $BASEDIR/$DOWNLOADS_RELDIR
fi

rm -rf $AUTOGT_CLASSPATH_BASE1

cp $THISDIR/source-me-template.sh $THISDIR/../bin/source-me.sh
cp $THISDIR/test-container-template.sh $THISDIR/../bin/test-container.sh
chmod +x $THISDIR/../bin/test-container.sh

