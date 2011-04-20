#!/usr/bin/env python

import sys
import os
import simplejson as json

f = open("bootconf.json", "r")
vals_dict = json.load(f)
f.close()

os.putenv('DEBIAN_FRONTEND', 'noninteractive')
os.putenv('TERM', 'dumb')

dbname=vals_dict['mysql_dbname']
dbuser=vals_dict['mysql_dbuser']
dbpassword=vals_dict['mysql_dbpassword']
dbhost=vals_dict['mysql_dbhost']

commands = [
  'sudo mysql -h %s --database=%s --password=%s -e "select now();"' % (dbhost, dbname, dbpassword),
  "wget http://localhost/wordpress/wp-admin/install.php"]

for cmd in commands:
    print cmd
    rc = os.system(cmd)
    if rc != 0:
        print "ERROR! %d" % (rc)
        sys.exit(rc)

print "SUCCESS"
sys.exit(0)

