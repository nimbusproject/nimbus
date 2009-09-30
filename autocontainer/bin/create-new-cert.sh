#!/bin/bash

# if a dummy value is present, don't print this header (embedded call)
if [ "X" = "X$3" ]; then
  echo "AutoContainer 4.0.8 v1.0 -- Create new certificate"
fi

# -----------------------------------------------------------------------------
# {{{  help system
# -----------------------------------------------------------------------------

function help() {
  echo -e "\nThis script requires at least one argument:\n - The auto-container certificate authority you would like to use\nFor example, ~/.globus/auto-container-01/ca"
  echo -e "\nOptional second argument:\n - grid-mapfile path to add authorization for this new certificate"
  exit 1
}

if [ "X$1" = "X" ]; then
  help
fi

if [ "X$1" = "X-h" ]; then
  help
fi

if [ "X$1" = "X-help" ]; then
  help
fi

if [ "X$1" = "X--h" ]; then
  help
fi

if [ "X$1" = "X--help" ]; then
  help
fi

# }}}

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
# {{{  find CA files
# -----------------------------------------------------------------------------

CA_PUBPEM=`$JAVA_BIN $JAVA_OPTS $EXE_FIND_CA_PUBPEM $1`
if [ $? -ne 0 ]; then
  exit 1
fi

CA_PRIVPEM=`$JAVA_BIN $JAVA_OPTS $EXE_FIND_CA_PRIVPEM $1`
if [ $? -ne 0 ]; then
  exit 1
fi

# }}}

# -----------------------------------------------------------------------------
# {{{  get CN
# -----------------------------------------------------------------------------

SOMETHING_ENTERED="undefined"
count=0
while [ $count -lt 6 ]; do
  count=$((count + 1))
  echo "Please enter the name for a new certificate (for example, 'John Smith'): "
  read newname
  if [[ "X$newname" != "X" ]]; then
    SOMETHING_ENTERED="defined"
    TARGETCN="$newname"
    count=10
  else
    echo "Please enter something."
  fi
done

if [ "undefined" = "$SOMETHING_ENTERED" ]; then
  echo -e "\nExiting, no response"
  exit 1
fi

# }}}

# -----------------------------------------------------------------------------
# {{{  find OK usercert name
# -----------------------------------------------------------------------------

BASENAME="usercert"
BASEKEYNAME="userkey"
CERTDIR=`dirname ~/.globus/23fno32ifn32oifn`

PUBPEM=""
PRIVPEM=""

if [ ! -f $CERTDIR/$BASENAME.pem ]; then
  if [ ! -f $CERTDIR/$BASEKEYNAME.pem ]; then
    PUBPEM="$BASENAME.pem"
    PRIVPEM="$BASEKEYNAME.pem"
  fi
fi

DEFAULT_CERT="y"

if [ "X" = "X$PUBPEM" ]; then
    DEFAULT_CERT="n"

    count=0
    while [ $count -lt $AUTO_CONTAINER_MAX_CERT_TRIES ]; do
      count=$((count + 1))
      
      if [ ! -f $CERTDIR/$BASENAME-$count.pem ]; then
        if [ ! -f $CERTDIR/$BASEKEYNAME-$count.pem ]; then
          PUBPEM="$BASENAME-$count.pem"
          PRIVPEM="$BASEKEYNAME-$count.pem"
        fi
      fi
      
      if [ "X" != "X$PUBPEM" ]; then
        count=$((AUTO_CONTAINER_MAX_CERT_TRIES + 1))
      fi
    done
fi

if [ "X" = "X$PUBPEM" ]; then
  echo "Could not find a good name for the new certificate"
  exit 1
fi

# }}}

# -----------------------------------------------------------------------------
# {{{  generate cert
# -----------------------------------------------------------------------------

DN=`$JAVA_BIN $JAVA_OPTS $EXE_CREATE_NEW_CERT $CERTDIR "$TARGETCN" $PUBPEM $PRIVPEM $CA_PUBPEM $CA_PRIVPEM`
if [ $? -ne 0 ]; then
  echo "Problem creating certificate, exiting."
  exit 1
fi

chmod 400 ~/.globus/$PRIVPEM
if [ $? -ne 0 ]; then
  echo "Problem setting permissions on ~/.globus/$PRIVPEM"
  exit 1
fi

echo -e "\nCreated certificate @ ~/.globus/$PUBPEM"

echo -e "\nThe new 'DN' string is: $DN"

# }}}

# -----------------------------------------------------------------------------
# {{{  save backup cert into auto-container directory, helps correlate later
# -----------------------------------------------------------------------------

USERCERT_DIR=`dirname $CA_PUBPEM`/../user-certs
USERCERT_DIR=`cd $USERCERT_DIR; pwd`
if [ -d $USERCERT_DIR ]; then
  cp ~/.globus/$PUBPEM $USERCERT_DIR/
  cp ~/.globus/$PRIVPEM $USERCERT_DIR/
  echo -e "\nBacked up usercert+key to $USERCERT_DIR"
  
  if [ "$DEFAULT_CERT" = "y" ]; then
    echo "\$ $GLOBUS_LOCATION/bin/grid-proxy-init" > $USERCERT_DIR/latest-gpi.txt
  else
    echo "\$ $GLOBUS_LOCATION/bin/grid-proxy-init -cert ~/.globus/$PUBPEM -key ~/.globus/$PRIVPEM" > $USERCERT_DIR/latest-gpi.txt
  fi
fi


# }}}

# -----------------------------------------------------------------------------
# {{{  optionally add to grid-mapfile
# -----------------------------------------------------------------------------

if [ "X" != "X$2" ]; then

  if [ ! -f "$2" ]; then
    echo "You supplied a grid-mapfile argument '$2' but it does not exist."
    exit 1
  fi
  
  echo -e "\n\"$DN\" not_a_real_account\n" >> $2
  if [ $? -ne 0 ]; then
    echo "Problem adding the new DN to $2"
    exit 1
  fi
  echo -e "\nAuthorized the new credential in the grid-mapfile: $2"
fi

# }}}


