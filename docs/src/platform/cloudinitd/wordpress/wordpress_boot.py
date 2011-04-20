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
  "sudo -E apt-get update",
  "sudo -E apt-get -y -q install wordpress php5-mysql php5 libapache2-mod-proxy-html php-mdb2-driver-mysql",
  "wget http://wordpress.org/latest.tar.gz",
  "tar -zxvf latest.tar.gz",
  "sed -e 's/database_name_here/%s/' -e 's/username_here/%s/' -e 's/password_here/%s/' -e 's/localhost/%s/' wordpress/wp-config-sample.php > wordpress/wp-config.php" % (dbname, dbuser, dbpassword, dbhost),
  "sudo mv wordpress/ /var/www/",
  "sudo -E /etc/init.d/apache2 restart"]

for cmd in commands:
    print cmd
    rc = os.system(cmd)
    if rc != 0:
        print "ERROR! %d" % (rc)
        sys.exit(rc)

print "SUCCESS"
sys.exit(0)

