#!/usr/bin/env python

import sys
import os
import simplejson as json

f = open("bootconf.json", "r")
vals_dict = json.load(f)
f.close()

os.putenv('DEBIAN_FRONTEND', 'noninteractive')
os.putenv('TERM', 'dumb')

password=vals_dict['dbpassword']
dbname=vals_dict['dbname']

commands = []
commands.append('sudo -E apt-get -y -q install mysql-server-5.1')
commands.append('sudo -E mysqladmin -u root password %s' % (password))
commands.append('sudo -E mysqladmin --password=%s create %s' % (password, dbname))
commands.append("sudo -E mysql --password=%s -e \"GRANT Select, Insert, Update ON *.* TO 'root'@'%%' IDENTIFIED BY '%s';\"" % (password, password))
commands.append("sudo -E sed -i 's/bind-address.*/bind-address = 0.0.0.0/' /etc/mysql/my.cnf")
commands.append("sudo -E restart mysql")

for cmd in commands:
    print cmd
    rc = os.system(cmd)
    if rc != 0:
        print "ERROR! %d" % (rc)
        sys.exit(rc)

print "SUCCESS"
sys.exit(0)
