#!/usr/bin/env python

import sys
import os

f = open("hello.html", "w")
f.write("<html><body>Hello cloudinit.d!</body></html>")
f.close()

cmd = "sudo cp hello.html /var/www/"
os.system(cmd)

sys.exit(0)
