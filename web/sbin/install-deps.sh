#!/bin/bash

# Called from ../bin/run-standalone-*

# TODO: consider switching to embedded scons?

NIMBUS_WEBDIR_REL="`dirname $0`/.."
NIMBUS_WEBDIR=`cd $NIMBUS_WEBDIR_REL; pwd`

MARKERFILE="nimbusweb.empty.marker"
DJANGO_TARGET="$NIMBUS_WEBDIR/lib/python/django"
CHERRPY_TARGET="$NIMBUS_WEBDIR/lib/python/cherrypy"
AUTOCOMMON_JAR_TARGET="$NIMBUS_WEBDIR/lib/java"
PROCESSMAN_TARGET="$NIMBUS_WEBDIR/lib/python/ProcessManager.py"

if [ ! -f "$DJANGO_TARGET/$MARKERFILE" ] &&
   [ ! -f "$CHERRPY_TARGET/$MARKERFILE" ] &&
   [ ! -f "$AUTOCOMMON_JAR_TARGET/$MARKERFILE" ] &&
   [ -f $PROCESSMAN_TARGET ]; then # processman is only a single file
     
    #echo "debug: no marker files found"
    exit 0
fi

# ------------------------------------------------------------------------------
# ------------------------------------------------------------------------------

TMPDIR="$NIMBUS_WEBDIR/var/tmplibdir"

DJANGO_ARCHIVE="$NIMBUS_WEBDIR/lib/Django-1.1.1.tar.gz"
CHERRPY_ARCHIVE="$NIMBUS_WEBDIR/lib/CherryPy-3.1.2.tar.gz"
AUTOCOMMON_ARCHIVE="$NIMBUS_WEBDIR/lib/nimbus-autocommon.tar.gz"
PROCESSMAN_SRCFILE="$NIMBUS_WEBDIR/lib/ProcessManager.py"

if [ -e $TMPDIR ]; then
    echo "Cannot proceed, the temp directory exists: $TMPDIR"
    exit 1
fi

mkdir $TMPDIR
if [ $? -ne 0 ]; then
    echo "Could not create temp directory: $TMPDIR"
    exit 1
fi

if [ -f "$DJANGO_TARGET/$MARKERFILE" ]; then
    
    if [ ! -f $DJANGO_ARCHIVE ]; then
        echo "Could not find django archive: $DJANGO_ARCHIVE"
        exit 1
    fi
    
    mkdir $TMPDIR/django
    if [ $? -ne 0 ]; then
        echo "Could not create temp directory: $TMPDIR/django"
        exit 1
    fi
    
    tar xzf $DJANGO_ARCHIVE -C $TMPDIR/django
    if [ ! -e $TMPDIR/django/Django-1.1.1/django ]; then
        echo "Could not find the expanded django lib"
        exit 1
    fi
    
    mv $TMPDIR/django/Django-1.1.1/django/* $DJANGO_TARGET/
    if [ $? -ne 0 ]; then
        echo "Could not install the expanded django lib"
        exit 1
    fi
    
    rm $DJANGO_TARGET/$MARKERFILE
    if [ $? -ne 0 ]; then
        echo "Could not remove $DJANGO_TARGET/$MARKERFILE"
        exit 1
    fi
fi

if [ -f "$CHERRPY_TARGET/$MARKERFILE" ]; then
    
    if [ ! -f $CHERRPY_ARCHIVE ]; then
        echo "Could not find cherrypy archive: $CHERRPY_ARCHIVE"
        exit 1
    fi
    
    mkdir $TMPDIR/cherrypy
    if [ $? -ne 0 ]; then
        echo "Could not create temp directory: $TMPDIR/cherrypy"
        exit 1
    fi
    
    tar xzf $CHERRPY_ARCHIVE -C $TMPDIR/cherrypy 
    if [ ! -e $TMPDIR/cherrypy/CherryPy-3.1.2/cherrypy ]; then
        echo "Could not find the expanded cherrypy lib"
        exit 1
    fi
    
    mv $TMPDIR/cherrypy/CherryPy-3.1.2/cherrypy/* $CHERRPY_TARGET/
    if [ $? -ne 0 ]; then
        echo "Could not install the expanded cherrypy lib"
        exit 1
    fi
    
    rm $CHERRPY_TARGET/$MARKERFILE
    if [ $? -ne 0 ]; then
        echo "Could not remove $CHERRPY_TARGET/$MARKERFILE"
        exit 1
    fi
fi

if [ -f "$AUTOCOMMON_JAR_TARGET/$MARKERFILE" ]; then
    
    if [ ! -f $AUTOCOMMON_ARCHIVE ]; then
        echo "Could not find autocommon archive: $AUTOCOMMON_ARCHIVE"
        exit 1
    fi
    
    mkdir $TMPDIR/autocommon
    if [ $? -ne 0 ]; then
        echo "Could not create temp directory: $TMPDIR/autocommon"
        exit 1
    fi
    
    tar xzf $AUTOCOMMON_ARCHIVE -C $TMPDIR/autocommon
    if [ ! -e $TMPDIR/autocommon/nimbus-autocommon/dist ]; then
        echo "Could not find the expanded autocommon lib"
        exit 1
    fi
    
    mv $TMPDIR/autocommon/nimbus-autocommon/dist/*jar $AUTOCOMMON_JAR_TARGET/
    if [ $? -ne 0 ]; then
        echo "Could not install the expanded autocommon dist-lib"
        exit 1
    fi
    mv $TMPDIR/autocommon/nimbus-autocommon/lib-compile/*jar $AUTOCOMMON_JAR_TARGET/
    if [ $? -ne 0 ]; then
        echo "Could not install the expanded autocommon lib-lib"
        exit 1
    fi
    
    rm $AUTOCOMMON_JAR_TARGET/$MARKERFILE
    if [ $? -ne 0 ]; then
        echo "Could not remove $AUTOCOMMON_JAR_TARGET/$MARKERFILE"
        exit 1
    fi
fi

if [ ! -f $PROCESSMAN_TARGET ]; then
    
    if [ ! -f $PROCESSMAN_SRCFILE ]; then
        echo "Could not find ProcessManager source file: $PROCESSMAN_SRCFILE"
        exit 1
    fi
    
    cp $PROCESSMAN_SRCFILE $PROCESSMAN_TARGET
    
fi

rm -rf $TMPDIR
if [ $? -ne 0 ]; then
    echo "Could not remove temp directory: $TMPDIR"
    exit 1
fi
