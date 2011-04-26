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
commands.append('sudo mysql --password=monkey -e "select now();"')

for cmd in commands:
    print cmd
    rc = os.system(cmd)
    if rc != 0:
        print "ERROR! %d" % (rc)
        sys.exit(rc)

print "SUCCESS"
sys.exit(0)
