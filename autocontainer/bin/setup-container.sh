#!/bin/bash

echo "AutoContainer 4.0.8 v1.0 -- Setup container and certificates"

# -----------------------------------------------------------------------------
# {{{  get auto-container environment
# -----------------------------------------------------------------------------

THISDIR="`dirname $0`"
LIBDIR="$THISDIR/../lib/"
if [ ! -f $LIBDIR/common-env.sh ]; then
  echo "Failure, cannot find environment definitions"
  exit 1
fi
source $LIBDIR/common-env.sh

# }}}

# -----------------------------------------------------------------------------
# {{{  dependency checks
# -----------------------------------------------------------------------------

JAVA_BIN="java"

function nobin() {
  echo -e "\nERROR: cannot find $1\n - install $1\n - OR adjust the configuration value at the top of this script to point to $1\n"
  exit 1
}

CAPTURE=`$JAVA_BIN -version 2>&1`
if [ $? -ne 0 ]; then
  nobin java
fi

$JAVA_BIN $JAVA_OPTS $EXE_JVMCHECK
if [ $? -ne 0 ]; then
  exit 1
fi

# }}}

# -----------------------------------------------------------------------------
# {{{  hostname decision
# -----------------------------------------------------------------------------

echo -e "\nSetting up hostname that clients can use to contact the container."

HOSTGUESS=`$JAVA_BIN $JAVA_OPTS $EXE_HOSTGUESS`

if [ "localhost" = "$HOSTGUESS" ]; then

  echo -e "\nIt does not look like you have a hostname set up, the best guess is 'localhost' but this will probably not work from other hosts.  That is fine if you just want to test locally."

elif [ "X" = "X$HOSTGUESS" ]; then

  echo -e "\nProblem guessing hostname."

else

  echo -e "\nIt looks like you have a hostname set up: $HOSTGUESS"

fi

DO_NEWHOST="undefined"
count=0
while [ $count -lt 6 ]; do
  count=$((count + 1))
  echo -e "\nWould you like to manually enter a different hostname? y/n: "
  read do_newhostname
  if [ "$do_newhostname" = "y" ]; then
    DO_NEWHOST="y"
    count=10
  elif [ "$do_newhostname" = "n" ]; then
    DO_NEWHOST="n"
    count=10
  else
    echo "Please enter 'y' or 'n'"
  fi
done

if [ "undefined" = "$DO_NEWHOST" ]; then
  echo -e "\nExiting, no response"
  exit 1
fi

CONTAINER_HOSTNAME="$HOSTGUESS"
if [ "$DO_NEWHOST" = "y" ]; then

  count=0
  while [ $count -lt 6 ]; do
    count=$((count + 1))
    echo -e "\nPlease enter the hostname to use: "
    read newhostname
    if [ "X$newhostname" = "X" ]; then
      echo "Please enter something."
    else
      CONTAINER_HOSTNAME=$newhostname
      count=10
    fi
  done
fi

echo -e "\nUsing hostname: $CONTAINER_HOSTNAME"

# }}}

# -----------------------------------------------------------------------------
# {{{  hostname related adjustments in container config etc.
# -----------------------------------------------------------------------------

echo ""
echo "Adjusting the container configuration to match it:"

$JAVA_BIN $JAVA_OPTS $EXE_LOGICAL_HOST $CONTAINER_HOSTNAME $CONF_SERVERCONFIG
if [ $? -ne 0 ]; then
  echo "Problem, exiting."
  exit 1
fi
echo " - Directed container to use $CONTAINER_HOSTNAME (logicalHost setting)"

$JAVA_BIN $JAVA_OPTS $EXE_PUBLISH_HOST true $CONF_SERVERCONFIG
if [ $? -ne 0 ]; then
  echo "Problem, exiting."
  exit 1
fi
echo " - Directed container to publish $CONTAINER_HOSTNAME in advertised URLs"

echo "\$GLOBUS_LOCATION/bin/counter-client -s https://$CONTAINER_HOSTNAME:8443/wsrf/services/SecureCounterService" >> $THISDIR/test-container.sh
if [ $? -ne 0 ]; then
  echo "Problem editing test-container.sh" 
  exit 1
fi
echo "" >> $THISDIR/test-container.sh
if [ $? -ne 0 ]; then
  echo "Problem editing test-container.sh" 
  exit 1
fi

# }}}

# -----------------------------------------------------------------------------
# {{{  generate new auto-container directory
# -----------------------------------------------------------------------------

echo -e "\nSetting up auto-container configuration directory"
NEW_CONTAINER_DIR=`$JAVA_BIN $JAVA_OPTS $EXE_CREATE_CONTAINER_DIR ~/.globus auto-container-`
if [ $? -ne 0 ]; then
  echo "Problem, exiting."
  exit 1
fi
echo " - created container configuration directory: $NEW_CONTAINER_DIR"

chmod 700 $NEW_CONTAINER_DIR
if [ $? -ne 0 ]; then
  echo "Problem setting permissions on $NEW_CONTAINER_DIR"
  exit 1
fi

mkdir $NEW_CONTAINER_DIR/ca
if [ $? -ne 0 ]; then
  echo "Problem creating $NEW_CONTAINER_DIR/ca"
  exit 1
fi

mkdir $NEW_CONTAINER_DIR/hostcert
if [ $? -ne 0 ]; then
  echo "Problem creating $NEW_CONTAINER_DIR/hostcert"
  exit 1
fi

mkdir $NEW_CONTAINER_DIR/trusted-certs
if [ $? -ne 0 ]; then
  echo "Problem creating $NEW_CONTAINER_DIR/trusted-certs"
  exit 1
fi

mkdir $NEW_CONTAINER_DIR/user-certs
if [ $? -ne 0 ]; then
  echo "Problem creating $NEW_CONTAINER_DIR/user-certs"
  exit 1
fi

$JAVA_BIN $JAVA_OPTS $EXE_GLOBUS_SECDESC $CONF_SECDESC $CONF_SERVERCONFIG
if [ $? -ne 0 ]; then
  echo "Problem, exiting."
  exit 1
fi
echo " - activated new container security settings @ $CONF_SECDESC"

touch $NEW_CONTAINER_DIR/grid-mapfile
if [ $? -ne 0 ]; then
  echo "Problem creating $NEW_CONTAINER_DIR/grid-mapfile"
  exit 1
fi
CONTAINER_GRID_MAPFILE=$NEW_CONTAINER_DIR/grid-mapfile
echo " - created container authorization file: $NEW_CONTAINER_DIR/grid-mapfile"

$JAVA_BIN $JAVA_OPTS $EXE_NEW_GRIDMAPFILE $CONTAINER_GRID_MAPFILE $CONF_SECDESC
if [ $? -ne 0 ]; then
  echo "Problem, exiting."
  exit 1
fi
echo " - directed container to use that authorization file"

# }}}

# -----------------------------------------------------------------------------
# {{{  generate new certificate authority
# -----------------------------------------------------------------------------

echo ""

UUID=`uuidgen`
if [ $? -ne 0 ]; then
  echo "Problem creating UUID name for new certificate authority"
  exit 1
fi

$JAVA_BIN $JAVA_OPTS $EXE_CREATE_NEW_CA $NEW_CONTAINER_DIR/ca $UUID
if [ $? -ne 0 ]; then
  echo "Problem creating new certificate authority, exiting."
  exit 1
fi

CA_PUBPEM="$NEW_CONTAINER_DIR/ca/$UUID.pem"
CA_PUBPEM2="$UUID.0"
CA_PRIVPEM="$NEW_CONTAINER_DIR/ca/private-key-$UUID.pem"
CA_SIGNING_POLICY="$NEW_CONTAINER_DIR/ca/$UUID.signing_policy"

chmod 400 $CA_PRIVPEM
if [ $? -ne 0 ]; then
  echo "Problem setting permissions on $CA_PRIVPEM"
  exit 1
fi

$JAVA_BIN $JAVA_OPTS $EXE_WRITE_SIGNING_POLICY $CA_PUBPEM $CA_SIGNING_POLICY
if [ $? -ne 0 ]; then
  echo "Problem creating new certificate authority signing policy, exiting."
  exit 1
fi

echo "Created certificate authority @ $NEW_CONTAINER_DIR/ca"

# }}}

# -----------------------------------------------------------------------------
# {{{  activate trusted certs
# -----------------------------------------------------------------------------

cp $CA_PUBPEM $NEW_CONTAINER_DIR/trusted-certs/
if [ $? -ne 0 ]; then
  echo "Problem copying new CA cert to $NEW_CONTAINER_DIR/trusted-certs"
  exit 1
fi
cp $CA_PUBPEM $NEW_CONTAINER_DIR/trusted-certs/$CA_PUBPEM2
if [ $? -ne 0 ]; then
  echo "Problem copying new CA cert (2) to $NEW_CONTAINER_DIR/trusted-certs"
  exit 1
fi

echo "X509_CERT_DIR=$NEW_CONTAINER_DIR/trusted-certs" >> $THISDIR/source-me.sh
if [ $? -ne 0 ]; then
  echo "Problem editing source-me.sh"
  exit 1
fi
echo "export X509_CERT_DIR" >> $THISDIR/source-me.sh
if [ $? -ne 0 ]; then
  echo "Problem editing source-me.sh"
  exit 1
fi
echo "" >> $THISDIR/source-me.sh
if [ $? -ne 0 ]; then
  echo "Problem editing source-me.sh"
  exit 1
fi
echo "GLOBUS_LOCATION=$GLOBUS_LOCATION" >> $THISDIR/source-me.sh
if [ $? -ne 0 ]; then
  echo "Problem editing source-me.sh"
  exit 1
fi
echo "export GLOBUS_LOCATION" >> $THISDIR/source-me.sh
if [ $? -ne 0 ]; then
  echo "Problem editing source-me.sh"
  exit 1
fi
echo "" >> $THISDIR/source-me.sh
if [ $? -ne 0 ]; then
  echo "Problem editing source-me.sh"
  exit 1
fi

echo " - set up trusted certificates directory with this CA in it @ $NEW_CONTAINER_DIR/trusted-certs"


# }}}

# -----------------------------------------------------------------------------
# {{{  generate new host cert
# -----------------------------------------------------------------------------

echo ""

HOSTDN=`$JAVA_BIN $JAVA_OPTS $EXE_CREATE_NEW_CERT $NEW_CONTAINER_DIR/hostcert $CONTAINER_HOSTNAME hostcert.pem hostkey.pem $CA_PUBPEM $CA_PRIVPEM`
if [ $? -ne 0 ]; then
  echo "Problem creating host cert, exiting."
  exit 1
fi

HOST_PUBPEM="$NEW_CONTAINER_DIR/hostcert/hostcert.pem"
HOST_PRIVPEM="$NEW_CONTAINER_DIR/hostcert/hostkey.pem"

chmod 400 $HOST_PRIVPEM
if [ $? -ne 0 ]; then
  echo "Problem setting permissions on $HOST_PRIVPEM"
  exit 1
fi

echo "Created host certificate for '$CONTAINER_HOSTNAME' @ $NEW_CONTAINER_DIR/hostcert"

# }}}

# -----------------------------------------------------------------------------
# {{{  activate host cert
# -----------------------------------------------------------------------------

$JAVA_BIN $JAVA_OPTS $EXE_NEW_HOSTCERTFILE $HOST_PUBPEM $CONF_SECDESC
if [ $? -ne 0 ]; then
  echo "Problem, exiting."
  exit 1
fi
$JAVA_BIN $JAVA_OPTS $EXE_NEW_HOSTKEYFILE $HOST_PRIVPEM $CONF_SECDESC
if [ $? -ne 0 ]; then
  echo "Problem, exiting."
  exit 1
fi
echo " - directed container to use that host certificate"


# }}}

# -----------------------------------------------------------------------------
# {{{  generate user cert
# -----------------------------------------------------------------------------

echo ""

$THISDIR/$EXE_CREATE_NEW_CERT_SHELL_SCRIPT_NAME $NEW_CONTAINER_DIR/ca $CONTAINER_GRID_MAPFILE dummyval
if [ $? -ne 0 ]; then
  exit 1
fi

echo -e "\n===\n\nTo create more users with this certificate authority, run this in the future:"
echo ""
echo "\$ $BASEDIR/bin/$EXE_CREATE_NEW_CERT_SHELL_SCRIPT_NAME $NEW_CONTAINER_DIR/ca"
echo ""
echo ""
echo "Or like so to automatically authorize the newly generated credential:"
echo ""
echo "\$ $BASEDIR/bin/$EXE_CREATE_NEW_CERT_SHELL_SCRIPT_NAME $NEW_CONTAINER_DIR/ca $CONTAINER_GRID_MAPFILE"

sleep 3


# }}}

echo -e "\n===\n\nCongratulations.\n"

echo "In one terminal, run:"
echo ""
echo "\$ $BASEDIR/bin/start-container.sh"

echo ""
echo -e "\nIn another terminal, run:"
echo ""
echo "\$ source $BASEDIR/bin/source-me.sh"
cat $NEW_CONTAINER_DIR/user-certs/latest-gpi.txt
echo "\$ $BASEDIR/bin/test-container.sh"

echo -e "\nBye!"





