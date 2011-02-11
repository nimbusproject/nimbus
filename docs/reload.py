import sys
import os
import tempfile

(osh, tmp_file)  = tempfile.mkstemp()
old_nimbus = sys.argv[1]
new_nimbus = sys.argv[2]

cmd="%s/bin/nimbusctl stop" % (old_nimbus)
os.system(cmd)
cmd="%s/bin/nimbusctl stop" % (new_nimbus)
os.system(cmd)
cmd="%s/bin/nimbusctl start" % (old_nimbus)
os.system(cmd)
cmd="%s/bin/nimbus-nodes --list -b -D , -o %s" % (old_nimbus, tmp_file)
os.system(cmd)
cmd="%s/bin/nimbusctl stop" % (old_nimbus)
os.system(cmd)
cmd="%s/bin/nimbusctl start" % (new_nimbus)
os.system(cmd)

f = open(tmp_file, "r")

for line in f:
    parts = line.split(',')

    new_cmd = "%s/bin/nimbus-nodes --add '%s' -p '%s' -m '%s' -n '%s'" % (new_nimbus, parts[0], parts[1], parts[2], parts[3])

    os.system(new_cmd)

cmd="%s/bin/nimbusctl stop" % (new_nimbus)
os.system(cmd)
