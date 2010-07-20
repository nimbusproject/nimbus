#!/bin/bash

if ([ "X$1" == "X--help" ] || [ "X$1" == "X-h" ] || [ $# -ne 1 ]); then
    echo "Ensures all classes in all jars in a directory work with Java 1.5"
    echo "Usage:"
    echo "$0 <path to a directory (like a Nimbus installation)>"
    exit 0
fi

if [ ! -d $1 ]; then
    echo "Not a directory: $1"
    exit 1
fi

BASEDIR_REL="`dirname $0`/.."
BASEDIR=`cd $BASEDIR_REL; pwd`


# Great program, and Apache2 licensed:
# http://alumnus.caltech.edu/~leif/opensource/bcver/BcVerApp.html

BCVER="$BASEDIR/lib/test/bcver.jar"
EXE="java -jar $BCVER -c -max=49.0 +j"

for f in `find $1 -iname "*jar"`; do 
    echo "Checking $f"
    $EXE $f
    if [ $? -ne 0 ]; then
        echo -e "\n*** Incompatible: $f\n"
    fi
done

