#!/bin/bash -x

THISDIR="`dirname $0`"
if [ ! -f $THISDIR/environment.sh ]; then
  echo "Failure, cannot find environment definitions"
  exit 1
fi
source $THISDIR/environment.sh

if [ ! -f $CLCLBUILDER_TARBALL_DEST ]; then
    
    if [ ! -f $CLCLBUILDER_TARBALL_DEST2 ]; then
        echo "Cannot find the wscore binary tarball.  Did you download it?  See README file."
        exit 1
    else
        CLCLBUILDER_TARBALL_DEST="$CLCLBUILDER_TARBALL_DEST2"
    fi
fi

CHECKSUM=`openssl md5 $CLCLBUILDER_TARBALL_DEST | awk '{print $2}'`
if [ $? -ne 0 ]; then
  echo "Checksum failed"
  exit 1
fi

# remember, two spaces between checksum value and filename in expected output:
if [ "$CHECKSUM" != "$CLCLBUILDER_WSCORE_MD5SUM" ]; then
  echo "Checksum comparison failed"
  exit 1
fi

echo "Checksum succeeds on $CLCLBUILDER_TARBALL_DEST"

if [ -e $CLCLBUILDER_DIST_DIRECTORY ]; then
  echo "Deleting dist directory: $CLCLBUILDER_DIST_DIRECTORY"
  rm -rf $CLCLBUILDER_DIST_DIRECTORY
  if [ $? -ne 0 ]; then
  	echo "Deleting dist directory failed"
  	exit 1
  fi
fi

mkdir $CLCLBUILDER_DIST_DIRECTORY
if [ $? -ne 0 ]; then
  echo "Creating new dist directory failed"
  exit 1
fi

echo "Created new dist directory: $CLCLBUILDER_DIST_DIRECTORY"

if [ ! -d $CLCLBUILDER_SRC_DIRECTORY ]; then
  echo "Cannot find cloud client source directory? $CLCLBUILDER_SRC_DIRECTORY"
  exit 1
fi

cp -a $CLCLBUILDER_SRC_DIRECTORY/* $CLCLBUILDER_DIST_DIRECTORY/
if [ $? -ne 0 ]; then
  echo "cp -a failed, exiting"
  exit 1
fi

mkdir $CLCLBUILDER_DIST_DIRECTORY/history
if [ $? -ne 0 ]; then
  echo "could not created empty history dir inside dist dir, exiting"
  exit 1
fi

chmod +x $CLCLBUILDER_DIST_DIRECTORY/bin/*
if [ $? -ne 0 ]; then
  echo "could not chmod +x the scripts in dist dir, exiting"
  exit 1
fi

# do not proceed if directory exists
if [ -e $GLOBUS_LOCATION ]; then
  echo "Failure, target GLOBUS_LOCATION directory exists?  $GLOBUS_LOCATION"
  exit 1
fi

if [ -d $CLCLBUILDER_WSCORE_DIRNAME ]; then
  echo "Failure, target of the wscore tarball expansion exists?  $CLCLBUILDER_WSCORE_DIRNAME"
  exit 1
fi

tar xzf $CLCLBUILDER_TARBALL_DEST -C $CLCLBUILDER_BASEDIR
if [ $? -ne 0 ]; then
  echo ""
  echo "wscore tar expansion failed, exiting"
  exit 1
fi

mv $CLCLBUILDER_BASEDIR/$CLCLBUILDER_WSCORE_DIRNAME $GLOBUS_LOCATION
if [ $? -ne 0 ]; then
  echo "Creating new embedded globus directory (via mv) failed"
  exit 1
fi

echo "Prepared dist directory, building Java parts of the cloud client into it now:" 

if [ ! -f $NIMBUS_CLIENT_INSTALL_SCRIPT ]; then
  echo "Cannot find Nimbus build and install script?  $NIMBUS_CLIENT_INSTALL_SCRIPT"
  exit 1
fi

/bin/bash $NIMBUS_CLIENT_INSTALL_SCRIPT
if [ $? -ne 0 ]; then
  echo ""
  echo "Client build failed, exiting"
  exit 1
fi

echo ""
echo "Creating dist tar.gz: $CLCLBUILDER_BASEDIR/$CLCLBUILDER_DIST_TARNAME"
echo ""
cd $CLCLBUILDER_BASEDIR
tar czf $CLCLBUILDER_DIST_TARNAME $CLCLBUILDER_RELEASE_NAME
RESULT=$?
if [ $RESULT -ne 0 ]; then
  echo ""
  echo "Tarball creation failed"
else
  echo "Done."
fi
exit $RESULT
