#!/usr/bin/env python
import string
from random import Random
okchars = string.letters + string.digits + "!@%^_&*+-"
print ''.join( Random().sample(okchars, 25) )
