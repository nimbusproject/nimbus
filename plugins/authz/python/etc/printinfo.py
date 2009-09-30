# This example just prints out everything known about the subject
# and request to stdout.

# If "$GLOBUS_LOCATION/container-log4j.properties" includes:
#
# log4j.category.org.globus.jython.log=INFO
#
# then the authorization module will log the stdout print
# statements in this script.
#
# For processing time and stderr log statements use:
#
# log4j.category.org.globus.jython.log=DEBUG

print "\nSUBJECT:"

if DN:
    print "DN: " + DN

if voms:
    print "voms VO: %s" + voms.VO
    print "voms hostport: %s" + voms.hostport
    print "found %d VOMS attributes" % len(voms.attributes)
    for i in voms.attributes:
        print i

if shib:
    print "found %d SAML attributes" % len(shib.attributes)
    for i in shib.attributes:
        print "     name: %s" % i.name
        print "namespace: %s" % i.namespace
        if i.values:
            print "   values:"
            for j in i.values:
                print "         : %s" % j

print "\nREQUEST:"

print "req.memory: %d" % req.memory
print "req.duration_secs: %d" % req.duration_secs

if len(req.images) > 0:
    print "req.images[0] (root disk): " + req.images[0]

if req.kernel:
    print "req.kernel: " + req.kernel

if req.kernelParams:
    print "req.kernelParams: " + req.kernelParams

print "found %d NIC(s)" % len(req.nics)
for i in req.nics:
    print "NIC:"
    print "         ip : %s" % i.ip
    print "       name : %s" % i.name
    print "association : %s" % i.association
    print "   hostname : %s" % i.hostname
    print "    gateway : %s" % i.gateway
    print "       mode : %s" % i.mode
    print "     method : %s" % i.method

decision = DENY
