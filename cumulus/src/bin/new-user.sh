#!/bin/bash

# my own tool for testing new users

nh=/home/bresnaha/NIM

cd $nh
cd cumulus
source env.sh
email=$1
dn=$2

$nh/cumulus/bin/cumulus-add-user "$email"
s3id=`echo $x | awk -F , '{ print $7 }'`
x=`$nh/cumulus/bin/cumulus-list-users -b -r ID,password,canonical_id "$email"`

echo $x
s3id=`echo $x | awk -F , '{ print $1 }'`
s3pw=`echo $x | awk -F , '{ print $2 }'`
canuser=`echo $x | awk -F , '{ print $3 }'`
echo "s3 user id:  $s3id"
echo "s3 password: $s3pw"
echo "canonical id: $canuser"

$nh/cumulus/bin/nimbusauthz-add-user -a "$dn" -t x509 $canuser
$nh/cumulus/bin/nimbusauthz-list-users -a $canuser

