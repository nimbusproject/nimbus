#!/bin/bash

if [ "X$1" == "X-h" ]; then
    echo "[<user to run the service under>]"
    exit 0
fi
if [ "X$1" == "X" ]; then
    who=`whoami`
    exit 1
else
    who=$1
fi

src_dir=`dirname $0`
cd $src_dir
LANTORRENT_HOME=`pwd`

sed -e "s/@PORT@/2893/" -e "s/@SERVICENAME@/lantorrent/" -e "s/@WHO@/$who/" -e "s^@LANTORRENT_HOME@^$LANTORRENT_HOME^" etc/lantorrent.inet.in > lantorrent.inet
rc=$?
if [ $rc -ne 0 ]; then
    echo "Failed to create the inetd entry"
    exit $rc
fi

echo "Install complete.  Copy $src_dir/lantorrent.inet into /etc/xinetd.d/ and restart xinetd"

exit 0
