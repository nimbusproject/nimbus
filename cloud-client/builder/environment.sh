if [ ! "X$CLCLBUILDER_ENVIRONMENT_DEFINED" = "X" ]; then
  return 0
fi

export CLCLBUILDER_RELEASE_NAME="nimbus-cloud-client-020"

# #########################################################

BASEDIR_REL="`dirname $0`/.."
export CLCLBUILDER_BASEDIR=`cd $BASEDIR_REL; pwd`

# #########################################################

export CLCLBUILDER_SRC_DIRECTORY="$CLCLBUILDER_BASEDIR/nimbus-cloud-client-src"
export CLCLBUILDER_DIST_DIRECTORY="$CLCLBUILDER_BASEDIR/$CLCLBUILDER_RELEASE_NAME"
export CLCLBUILDER_DIST_TARNAME="$CLCLBUILDER_RELEASE_NAME.tar.gz"

# #########################################################

export CLCLBUILDER_WSCORE_URL="http://www-unix.globus.org/ftppub/gt4/4.0/4.0.8/ws-core/bin/ws-core-4.0.8-bin.tar.gz"
export CLCLBUILDER_WSCORE_URL="https://github.com/downloads/nimbusproject/nimbus/ws-core-4.0.8-bin.tar.gz"

export CLCLBUILDER_WSCORE_MD5SUM="04563872e23fe5f7a033e26067ac141d"
export CLCLBUILDER_WSCORE_TARNAME="ws-core-4.0.8-bin.tar.gz"
export CLCLBUILDER_WSCORE_DIRNAME="ws-core-4.0.8"

# #########################################################

export CLCLBUILDER_TARBALL_DEST="$CLCLBUILDER_BASEDIR/$CLCLBUILDER_WSCORE_TARNAME"
export CLCLBUILDER_TARBALL_DEST2="$CLCLBUILDER_BASEDIR/../tmp/$CLCLBUILDER_WSCORE_TARNAME"

# #########################################################

export GLOBUS_LOCATION="$CLCLBUILDER_DIST_DIRECTORY/lib/globus"

# #########################################################

export NIMBUS_CLIENT_INSTALL_SCRIPT="$CLCLBUILDER_BASEDIR/../scripts/gt/clients-only-build-and-install.sh"

# #########################################################

export CLCLBUILDER_ENVIRONMENT_DEFINED="X"
