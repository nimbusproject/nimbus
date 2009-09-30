#!/bin/bash -x

THISDIR="`dirname $0`"
if [ ! -f $THISDIR/common-env.sh ]; then
  echo "Failure, cannot find environment definitions"
  exit 1
fi
source $THISDIR/common-env.sh

if [ ! -f $AUTOCOMMON_BUILD_FILE ]; then
  echo "Failure, cannot find autocommon buildfile: $AUTOCOMMON_BUILD_FILE"
  exit 1
fi

if [ -d $GLOBUS_LOCATION ]; then
  rm -rf $GLOBUS_LOCATION
fi

bash $AUTOCONTAINER_GET_CONTAINER
if [ $? -ne 0 ]; then
  echo "Failure with: $AUTOCONTAINER_GET_CONTAINER"
  exit 1
fi

if [ ! -d $BASEDIR/$DOWNLOADS_RELDIR ]; then
  echo "No download directory to remove: $BASEDIR/$DOWNLOADS_RELDIR"
else
  echo "Removing download directory: $BASEDIR/$DOWNLOADS_RELDIR"
  rm -rf $BASEDIR/$DOWNLOADS_RELDIR
fi

if [ -f "$AUTOCONTAINER_COUNTER_SECCONF" ]; then

  COUNTER_SECCONF_TARGET="$GLOBUS_LOCATION/etc/globus_wsrf_core_samples_counter/security-config.xml"
  if [ ! -f $COUNTER_SECCONF_TARGET ]; then
    echo "Failure, cannot find current $COUNTER_SECCONF_TARGET"
    exit 1
  fi
  cp $AUTOCONTAINER_COUNTER_SECCONF $COUNTER_SECCONF_TARGET
  if [ $? -ne 0 ]; then
    echo "Could not copy $AUTOCONTAINER_COUNTER_SECCONF to $COUNTER_SECCONF_TARGET"
  fi
  echo "Copied $AUTOCONTAINER_COUNTER_SECCONF to $COUNTER_SECCONF_TARGET"
fi

ant -f $AUTOCOMMON_BUILD_FILE clean dist

if [ ! -d $AUTOCOMMON_DIST_DIR ]; then
  echo "Failure, cannot find autocommon dist dir: $AUTOCOMMON_DIST_DIR"
  exit 1
fi

rm -rf $AUTOGT_CLASSPATH_BASE1
mkdir $AUTOGT_CLASSPATH_BASE1
if [ $? -ne 0 ]; then
  echo "Failure, could not mkdir $AUTOGT_CLASSPATH_BASE1"
  exit 1
fi

cp -r $AUTOCOMMON_DIST_DIR/* $AUTOGT_CLASSPATH_BASE1/

cp $THISDIR/source-me-template.sh $THISDIR/../bin/source-me.sh
cp $THISDIR/test-container-template.sh $THISDIR/../bin/test-container.sh
chmod +x $THISDIR/../bin/*sh

