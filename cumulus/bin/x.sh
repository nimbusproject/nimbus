#!/bin/sh

for x in `./cloudfs-list-user.sh  | awk  '{ print $2 }'` 
do 
    ./cloudfs-adduser.sh -r $x
done
