if DN == "/goodguy":
    print "we like this client no matter what: " + DN
    decision = PERMIT
elif req.memory <= 256:
    decision = PERMIT
else:
    decision = DENY

