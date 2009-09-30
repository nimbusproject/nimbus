#!/usr/bin/env python

# To generate index, run "generate-index.py < workspacepilot.py"

# If a section is added make sure to add a line for that in the INDEX
# definition in workspacepilot.py or the line numbers will be off.
# Not spending the time to auto replace the INDEX definition, just
# paste in the output.

import sys
begin=0
end=0
insection=False
namedict = {}
romandict = {}
linenumdict = {}
entrynum = 0
longestname = 0
longestroman = 0
for n,line in enumerate(sys.stdin):
    if line.startswith("# #############################") and not insection:
        begin = n
        insection = True
    if n == begin + 1:
        name = line[2:] # strip "# "
        name = name[:-1] # strip newline
        idx = name.rfind(". ")
        roman = name[:idx+1]
        romandict[entrynum] = roman
        if len(roman) > longestroman:
            longestroman = len(roman)
        name = name[idx+2:]
        namedict[entrynum] = name
        if len(name) > longestname:
            longestname = len(name)
    if insection and line.startswith("# }}} END"):
        end = n
        insection = False
        linenumdict[entrynum] = "(lines %d-%d)" % (begin+1, end+1)
        entrynum += 1

# justify right
for n,roman in enumerate(romandict.values()):
    if len(roman) < longestroman:
        diff = longestroman - len(roman)
        romandict[n] = diff * " " + roman 

# justify left
for n,name in enumerate(namedict.values()):
    if len(name) < longestname:
        diff = longestname - len(name)
        namedict[n] = name + diff * " "

for n in romandict.keys():
    print "  %s %s  %s" % (romandict[n], namedict[n], linenumdict[n])


