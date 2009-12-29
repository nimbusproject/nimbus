#!/bin/bash
# Running under cherrypy doesn't give you the nice python source reloading that
# Django's development server does.
#

echo -e "\n******************"
echo      "THIS IS NOT SECURE"
echo -e   "******************\n"


PYTHON_EXE="/usr/bin/env python"

NIMBUS_WEBDIR_REL="`dirname $0`/.."
NIMBUS_WEBDIR=`cd $NIMBUS_WEBDIR_REL; pwd`

NIMBUS_WEBCONF="$NIMBUS_WEBDIR/nimbusweb.conf"
if [ ! -f "$NIMBUS_WEBCONF" ]; then
    echo ""
    echo "Cannot find conf file, exiting. (expected at '$NIMBUS_WEBCONF')"
    exit 1
fi

NIMBUS_PYLIB="$NIMBUS_WEBDIR/lib/python"
NIMBUS_PYSRC="$NIMBUS_WEBDIR/src/python"

PYTHONPATH="$NIMBUS_PYSRC:$NIMBUS_PYLIB:$PYTHONPATH"
export PYTHONPATH


$NIMBUS_WEBDIR/sbin/install-deps.sh $DEBUG
if [ $? -ne 0 ]; then
    echo ""
    echo "Dependencies are not set up properly, exiting."
    exit 3
fi

$NIMBUS_WEBDIR/sbin/new-conf.sh $DEBUG --insecuremode
if [ $? -ne 0 ]; then
    echo ""
    echo "Problem configuring webapp, exiting."
    exit 4
fi

PORT=`$PYTHON_EXE $NIMBUS_PYSRC/nimbusweb/setup/setup.py --printport --conf $NIMBUS_WEBCONF --basedir $NIMBUS_WEBDIR`
if [ $? -ne 0 ]; then
    exit 5
fi


echo -e "\n******************"
echo      "THIS IS NOT SECURE"
echo -e   "******************\n"

echo "Point your browser to: http://localhost:$PORT/nimbus/"


$PYTHON_EXE $NIMBUS_PYSRC/nimbusweb/portal/manage.py runserver $PORT
