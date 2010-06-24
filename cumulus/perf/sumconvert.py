#!/usr/bin/python
import sys
import base64

hexstr=sys.argv[1].upper()
bin=base64.b16decode(hexstr)
b64=base64.b64encode(bin)

print b64
