#!/bin/sh

CONTAINER_URL="http://www-unix.globus.org/ftppub/gt4/4.0/4.0.8/ws-core/bin/ws-core-4.0.8-bin.tar.gz"
CONTAINER_TARNAME="ws-core-4.0.8-bin.tar.gz"
CONTAINER_UNTARREDNAME="ws-core-4.0.8"

# destination directory inside $NIMBUS_HOME
CONTAINER_DIRNAME="services"

NIMBUS_SRC_REL="`dirname $0`/.."
NIMBUS_SRC=`cd $NIMBUS_SRC_REL; pwd`

TMPDIR="$NIMBUS_SRC/bin/tmp"

if [ ! -d $TMPDIR ]; then
    mkdir $TMPDIR
    if [ $? -ne 0 ]; then
        echo "Failed to create temp directory: $TMPDIR"
        exit 1
    fi
fi

if [ "X$1" == "X" ]; then
    echo "\nUsage: $0 destination_dir"
    echo "\tYou must specify the destination directory.\n"
    exit 1
fi

NIMBUS_HOME=$1

if [ ! -d $NIMBUS_HOME ]; then
    PARENT_DIR=`dirname $NIMBUS_HOME`
    
    if [ -d $PARENT_DIR ]; then
        
        echo "Creating destination directory: $NIMBUS_HOME"
        mkdir $NIMBUS_HOME

        if [ $? -ne 0 ]; then
            echo "Failed to create destination directory!"
            exit 1
        fi
    else
        echo "Parent dir of destination does not exist: $PARENT_DIR"
        exit 1
    fi
fi

echo "Deploying skeleton directory structure.."
cp -fr $NIMBUS_SRC/home/* $NIMBUS_HOME/
if [ $? -ne 0 ]; then
    echo "Failed to copy Nimbus home directory"
    exit 1
fi

echo "Deploying web application.."
cp -r $NIMBUS_SRC/web $NIMBUS_HOME/
if [ $? -ne 0 ]; then
    echo "Failed to copy Nimbus web directory"
    exit 1
fi

CONTAINER_DIR="$NIMBUS_HOME/$CONTAINER_DIRNAME"
if [ ! -d $CONTAINER_DIR ]; then

    echo "Downloading and installing service container.."

    # fetch GT container if it doesn't already exist
    if [ ! -f $TMPDIR/$CONTAINER_TARNAME ]; then
        wget -c -O $TMPDIR/$CONTAINER_TARNAME $CONTAINER_URL

        if [ $? -ne 0 ]; then
            echo "Failed to download container tarball"
            exit 1
        fi
    fi

    tar xzf $TMPDIR/$CONTAINER_TARNAME -C $TMPDIR
    if [ $? -ne 0 ]; then
        echo "Failed to expand Nimbus tarball"
        exit 1
    fi

    mv $TMPDIR/$CONTAINER_UNTARREDNAME $CONTAINER_DIR
    if [ $? -ne 0 ]; then
        echo "Failed to move container directory to $CONTAINER_DIR"
        exit 1
    fi
else
    echo "Service container already exists at $CONTAINER_DIR"
fi

echo "Building and installing Nimbus to service container.."

GLOBUS_LOCATION=$CONTAINER_DIR
export GLOBUS_LOCATION

$NIMBUS_SRC/scripts/all-build-and-install.sh
if [ $? -ne 0 ]; then
    echo "Build and install FAILED!"
    exit 1
fi
