# Usage:
#
#  $ ./run-tests.sh django-app-name-to-test 
#
#  If you do not specify an app name, all test will run.
#  TODO: let '--verbosity' be passed in as an argument. 
#
PYTHON_EXE="/usr/bin/env python"

NIMBUS_WEBDIR_REL="`dirname $0`/../.."
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

$PYTHON_EXE $NIMBUS_PYSRC/nimbusweb/portal/manage.py test --verbosity=2 $1
