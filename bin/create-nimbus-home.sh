#!/bin/sh

CONTAINER_URL="http://www-unix.globus.org/ftppub/gt4/4.0/4.0.8/ws-core/bin/ws-core-4.0.8-bin.tar.gz"
CONTAINER_TARNAME="ws-core-4.0.8-bin.tar.gz"
CONTAINER_DIRNAME="services"

NIMBUS_SRC_REL="`dirname $0`/.."
NIMBUS_SRC=`cd $NIMBUS_SRC_REL; pwd`

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

cp -fr $NIMBUS_SRC/home/* $NIMBUS_HOME/
if [ $? -ne 0 ]; then
    echo "Failed to copy Nimbus home directory"
    exit 1
fi

cp -r $NIMBUS_SRC/web $NIMBUS_HOME/
if [ $? -ne 0 ]; then
    echo "Failed to copy Nimbus web directory"
    exit 1
fi

CONTAINER_DIR="$NIMBUS_HOME/$CONTAINER_DIRNAME"
if [ ! -d $CONTAINER_DIR ]; then
    mkdir $CONTAINER_DIR
    if [ $? -ne 0 ]; then
        echo "Failure, could not create directory: $CONTAINER_DIR"
        exit 1
    fi
fi

# fetch GT container if it doesn't already exist
if [ ! -f $NIMBUS_HOME/$CONTAINER_TARNAME ]; then
    wget -c -O $NIMBUS_HOME/$CONTAINER_TARNAME $CONTAINER_URL

    if [ $? -ne 0 ]; then
        echo "Failed to download container tarball"
        exit 1
    fi
fi

tar xzf $NIMBUS_HOME/$CONTAINER_TARNAME -C $CONTAINER_DIR
if [ $? -ne 0 ]; then
    echo "Failed to expand Nimbus tarbal"
    exit 1
fi
