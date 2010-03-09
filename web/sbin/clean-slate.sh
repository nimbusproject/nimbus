#!/bin/bash

NIMBUS_WEBDIR_REL="`dirname $0`/.."
NIMBUS_WEBDIR=`cd $NIMBUS_WEBDIR_REL; pwd`

PYTHONPATH="$BASEDIR/lib/pylib:$PYTHONPATH"
export PYTHONPATH

MARKERFILE="nimbusweb.empty.marker"
DJANGO_TARGET="$NIMBUS_WEBDIR/lib/python/django"
CHERRPY_TARGET="$NIMBUS_WEBDIR/lib/python/cherrypy"
HTTPLIB2_TARGET="$NIMBUS_WEBDIR/lib/python/httplib2"
AUTOCOMMON_JAR_TARGET="$NIMBUS_WEBDIR/lib/java"

# ------------------------------------------------------------------------------

echo -e "\nAre you sure you want to reset everything?\n\nAll accrued users, data, internal certificate authority, etc. will be DELETED.\n"

DO_CLEAN_SLATE="undefined"
count=0
while [ $count -lt 6 ]; do
    count=$((count + 1))
    echo -e "This is your last chance to make a backup. Proceed deleting?  Type y/n: "
    read do_cleanslate
    if [ "$do_cleanslate" = "y" ]; then
        DO_CLEAN_SLATE="y"
        count=10
    elif [ "$do_cleanslate" = "n" ]; then
        DO_CLEAN_SLATE="n"
        count=10
    else
        echo "Please enter 'y' or 'n'"
    fi
done

if [ "undefined" = "$DO_CLEAN_SLATE" ]; then
  echo -e "\nExiting, no response"
  exit 1
fi

if [ ! "$DO_CLEAN_SLATE" = "y" ]; then
    echo -e "\nExiting."
    exit 1
fi

# ------------------------------------------------------------------------------

echo -e "\nCreating a clean slate...\n"

MARKERFILE="nimbusweb.empty.marker"
DJANGO_TARGET="$NIMBUS_WEBDIR/lib/python/django"
CHERRPY_TARGET="$NIMBUS_WEBDIR/lib/python/cherrypy"
HTTPLIB2_TARGET="$NIMBUS_WEBDIR/lib/python/httplib2"
AUTOCOMMON_JAR_TARGET="$NIMBUS_WEBDIR/lib/java"

# ------------------------------------------------------------------------------

echo "Wiping django..."
rm -rf $DJANGO_TARGET
if [ $? -ne 0 ]; then
    echo "Could not remove django, exiting."
    exit 1
fi

mkdir $DJANGO_TARGET
if [ $? -ne 0 ]; then
    echo "Could not make a clean django dir, exiting."
    exit 1
fi
touch $DJANGO_TARGET/$MARKERFILE
if [ $? -ne 0 ]; then
    echo "Could not make a clean django dir, exiting."
    exit 1
fi

# ------------------------------------------------------------------------------

echo "Wiping cherrypy..."
rm -rf $CHERRPY_TARGET
if [ $? -ne 0 ]; then
    echo "Could not remove cherrypy, exiting."
    exit 1
fi

mkdir $CHERRPY_TARGET
if [ $? -ne 0 ]; then
    echo "Could not make a clean cherrypy dir, exiting."
    exit 1
fi
touch $CHERRPY_TARGET/$MARKERFILE
if [ $? -ne 0 ]; then
    echo "Could not make a clean cherrypy dir, exiting."
    exit 1
fi

# ------------------------------------------------------------------------------

echo "Wiping httplib2..."
rm -rf $HTTPLIB2_TARGET
if [ $? -ne 0 ]; then
    echo "Could not remove httplib2, exiting."
    exit 1
fi

mkdir $HTTPLIB2_TARGET
if [ $? -ne 0 ]; then
    echo "Could not make a clean httplib2 dir, exiting."
    exit 1
fi
touch $HTTPLIB2_TARGET/$MARKERFILE
if [ $? -ne 0 ]; then
    echo "Could not make a clean httplib2 dir, exiting."
    exit 1
fi

# ------------------------------------------------------------------------------

echo "Wiping autocommon..."
rm -rf $AUTOCOMMON_JAR_TARGET
if [ $? -ne 0 ]; then
    echo "Could not remove autocommon, exiting."
    exit 1
fi

mkdir $AUTOCOMMON_JAR_TARGET
if [ $? -ne 0 ]; then
    echo "Could not make a clean autocommon dir, exiting."
    exit 1
fi
touch $AUTOCOMMON_JAR_TARGET/$MARKERFILE
if [ $? -ne 0 ]; then
    echo "Could not make a clean autocommon dir, exiting."
    exit 1
fi

# ------------------------------------------------------------------------------

echo "Resetting var directory..."
rm -rf $NIMBUS_WEBDIR/var
if [ $? -ne 0 ]; then
    echo "Could not remove var directory, exiting."
    exit 1
fi

mkdir $NIMBUS_WEBDIR/var
if [ $? -ne 0 ]; then
    echo "Could not make a clean var dir, exiting."
    exit 1
fi
chmod 700 $NIMBUS_WEBDIR/var
if [ $? -ne 0 ]; then
    echo "Could not make a clean var dir, exiting."
    exit 1
fi
touch $NIMBUS_WEBDIR/var/nimbus.sqlite
if [ $? -ne 0 ]; then
    echo "Could not make a clean var dir, exiting."
    exit 1
fi

# ------------------------------------------------------------------------------

if [ -e $NIMBUS_WEBDIR/src/env.sh ]; then
    rm $NIMBUS_WEBDIR/src/env.sh
fi

# ------------------------------------------------------------------------------

echo "Removing generated_settings.py and generated_secrets.py..."

GENSET=$NIMBUS_WEBDIR/src/python/nimbusweb/portal/generated_settings.py
GENSEC=$NIMBUS_WEBDIR/src/python/nimbusweb/portal/generated_secrets.py
if [ -e $GENSET ]; then
    rm $GENSET
fi
if [ -e $GENSEC ]; then
    rm $GENSEC
fi 

# ------------------------------------------------------------------------------

echo "Wiping .pyc/.pyo files..."

find $NIMBUS_WEBDIR/src -iname "*pyc" -print -exec rm -rf {} \; &>/dev/null 
if [ $? -ne 0 ]; then
    echo "Failed to remove src/ pyc files"
fi

find $NIMBUS_WEBDIR/src -iname "*pyo" -print -exec rm -rf {} \; &>/dev/null
if [ $? -ne 0 ]; then
    echo "Failed to remove src/ pyo files"
fi

find $NIMBUS_WEBDIR/lib -iname "*pyc" -print -exec rm -rf {} \; &>/dev/null
if [ $? -ne 0 ]; then
    echo "Failed to remove lib/ pyc files"
fi

find $NIMBUS_WEBDIR/lib -iname "*pyo" -print -exec rm -rf {} \; &>/dev/null
if [ $? -ne 0 ]; then
    echo "Failed to remove lib/ pyo files"
fi

