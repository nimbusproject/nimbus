#!/bin/bash

OWNER_IS_RUNNER_ASSUMPTION="yes"

PYTHON_EXE="/usr/bin/env python -Wignore::DeprecationWarning"

NIMBUS_WEBDIR_REL="`dirname $0`/.."
NIMBUS_WEBDIR=`cd $NIMBUS_WEBDIR_REL; pwd`

NIMBUS_WEBCONF="$NIMBUS_WEBDIR/nimbusweb.conf"
if [ ! -f "$NIMBUS_WEBCONF" ]; then
    echo "Cannot find conf file, exiting. (expected at '$NIMBUS_WEBCONF')"
    exit 1
fi

NIMBUS_CONFIGURED="$NIMBUS_WEBDIR/.nimbusconfigured"
if [ ! -f "$NIMBUS_CONFIGURED" ]; then
    echo "It looks like you have not configured the webapp with nimbus-configure."
    echo "Set 'web.enabled' to true and re-run nimbus-configure with no arguments."
    exit 1
fi

NIMBUS_PYLIB="$NIMBUS_WEBDIR/lib/python"
NIMBUS_PYSRC="$NIMBUS_WEBDIR/src/python"

PYTHONPATH="$NIMBUS_PYSRC:$NIMBUS_PYLIB:$PYTHONPATH"
export PYTHONPATH

# ------------------------------------------------------------------------------

if [ "X$OWNER_IS_RUNNER_ASSUMPTION" = "Xyes" ]; then
    # The following script expands the tarballs in lib/
    $NIMBUS_WEBDIR/sbin/install-deps.sh $DEBUG
    if [ $? -ne 0 ]; then
        echo "Dependencies are not set up properly, exiting."
        exit 3
    fi
fi

$PYTHON_EXE $NIMBUS_PYSRC/nimbusweb/setup/setup.py --checkssl --conf $NIMBUS_WEBCONF --basedir $NIMBUS_WEBDIR
if [ $? -ne 0 ]; then
    echo ""
    echo "SSL is not set up properly, exiting."
    exit 2
fi

if [ "X$OWNER_IS_RUNNER_ASSUMPTION" == "Xyes" ]; then
    
    # This script will make sure the configurations in the webapp directory
    # and cherrypy are correct before launch.  The configurations in the
    # 'settings.py' file are the 'actual' configurations when the webapp runs
    # (it can run outside this standalone framework for example).
    
    $NIMBUS_WEBDIR/sbin/new-conf.sh $DEBUG
    if [ $? -ne 0 ]; then
        echo ""
        echo "Problem configuring webapp, exiting."
        exit 4
    fi
fi

# ------------------------------------------------------------------------------

PORT=`$PYTHON_EXE $NIMBUS_PYSRC/nimbusweb/setup/setup.py --printport --conf $NIMBUS_WEBCONF --basedir $NIMBUS_WEBDIR`
if [ $? -ne 0 ]; then
    exit 5
fi
HOST=`$PYTHON_EXE $NIMBUS_PYSRC/nimbusweb/setup/setup.py --printhost --conf $NIMBUS_WEBCONF --basedir $NIMBUS_WEBDIR`
if [ $? -ne 0 ]; then
    exit 5
fi
CERTPATH=`$PYTHON_EXE $NIMBUS_PYSRC/nimbusweb/setup/setup.py --printcertpath --conf $NIMBUS_WEBCONF --basedir $NIMBUS_WEBDIR`
if [ $? -ne 0 ]; then
    exit 5
fi
KEYPATH=`$PYTHON_EXE $NIMBUS_PYSRC/nimbusweb/setup/setup.py --printkeypath --conf $NIMBUS_WEBCONF --basedir $NIMBUS_WEBDIR`
if [ $? -ne 0 ]; then
    exit 5
fi

echo -e "\n*****************************************************************"
echo -e "*****************************************************************\n"

echo "Point your browser to: https://localhost:$PORT/nimbus/"
echo -e "\nAccept the \"certificate from an unknown authority\""
echo -e "\nUse CTRL-C to stop the server (you can get it to daemonize later)"

echo -e "\n*****************************************************************"
echo -e "*****************************************************************\n"

exec $PYTHON_EXE $NIMBUS_PYSRC/nimbusweb/portal/manage.py runcpserver "host=$HOST" "port=$PORT" "workdir=$NIMBUS_PYSRC/nimbusweb/portal/" "ssl_certificate=$CERTPATH" "ssl_private_key=$KEYPATH"


# Available options:

# 
# Optional CherryPy server settings: (setting=value)
#   host=HOSTNAME          hostname to listen on
#                          Defaults to localhost
#   port=PORTNUM           port to listen on
#                          Defaults to 8088
#   server_name=STRING     CherryPy's SERVER_NAME environ entry
#                          Defaults to localhost
#   daemonize=BOOL         whether to detach from terminal
#                          Defaults to False
#   pidfile=FILE           write the spawned process-id to this file
#   workdir=DIRECTORY      change to this directory when daemonizing
#   threads=NUMBER         Number of threads for server to use
#   ssl_certificate=FILE   SSL certificate file
#   ssl_private_key=FILE   SSL private key file
#   server_user=STRING     user to run daemonized process
#                          Defaults to www-data
#   server_group=STRING    group to daemonized process
#                          Defaults to www-data
#   request_queue_size=INT Size of connections queue


