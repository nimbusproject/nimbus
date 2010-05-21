#!/bin/bash

# my own tool for testing new users

nh=/home/bresnaha/NIM2

cd $nh
email=$1
dn=$2

x=`./cumulus/bin/cumulus-add-user.sh -n -g "$email"`
s3id=`echo $x | awk '{ print $7 }'`
s3pw=`echo $x | awk '{ print $9 }'`

echo $x
echo "s3 user id:  $s3id"
echo "s3 password: $s3pw"

y=`./cumulus/bin/cloudfs-list-user.sh -s $s3id`
canuser=`echo $y | awk '{ print $2 }'`
echo "canonical user is $canuser"

$nh/cumulus/bin/cloudfs-adduser.sh -a "$dn" -t x509 $canuser
$nh/cumulus/bin/cloudfs-list-user.sh -a $canuser

