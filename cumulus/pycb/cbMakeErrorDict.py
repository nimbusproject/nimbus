import string
import random
import os
import sys
from ConfigParser import SafeConfigParser


filename = sys.argv[1]
distName = sys.argv[2]

fptr = open(filename, 'r')
lines = fptr.readlines()
fptr.close()

count = 0
for l in lines:
    lA = l.split(' ', 1)
    codeStr = lA[0]
    restStr = lA[1].strip()

    # find the first digit
    ndx = -1
    for i in range(0, len(restStr)):
        ch = restStr[i]
        if ch.isdigit() and ndx == -1:
            ndx = i
           
    codeMsg = restStr[:ndx].strip()
    restStr = restStr[ndx:].strip()

    http_code = restStr[:3]
    restStr = restStr[3:].strip()
    lA = restStr.rsplit(' ', 1)
    httpMsg = lA[0].strip()

    print "    # errror type %d %s" % (count, codeStr)
    print "    %sCode[\'%s\'] = '%s'" % (distName, codeStr, codeMsg)
    print "    %sHttpCode[\'%s\'] = %s" % (distName, codeStr, http_code)
    print "    %sHttpMsg[\'%s\'] = '%s'" % (distName, codeStr, httpMsg)
    print ""

    count = count + 1
    
