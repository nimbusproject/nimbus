#!/bin/bash

rm /home/bresnaha/grp*

./bin/req.sh localhost 2893 /etc/group /home/bresnaha/grp1 $1 2 &
./bin/req.sh localhost 2893 /etc/group /home/bresnaha/grp3 $1 2
wait
