#!/bin/bash
#rm -f /var/lib/eucalyptus/nimbustests/FILE_here



ssh user04 /home/nimbus/virga/bin/req.sh $ip 2893 /var/lib/eucalyptus/nimbustests/FILE /var/lib/eucalyptus/nimbustests/FILE_here  $1 $2

