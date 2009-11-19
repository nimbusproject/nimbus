import os
import pathutil
import runutil
from setuperrors import *

EXE_JVMCHECK="org.nimbustools.auto_common.JVMCheck"

def check(basedir, log):
    (exitcode, stdout, stderr) = run(basedir, log, EXE_JVMCHECK)
    if exitcode != 0:
        msg = "Java problem:\n" + stderr + "\n"
        if stdout:
            msg += "(stdout '%s')" % stdout
        raise IncompatibleEnvironment(msg)

def run(basedir, log, klass, args=[]):
    cmd = "java -classpath %s" % classpath(basedir)
    cmd += " " + klass
    for arg in args:
        cmd += " \"" + arg + "\""
    return runutil.runexe(cmd, log)

def jarsdir(basedir):
    return pathutil.pathjoin(basedir, "lib/java")
    
def classpath(basedir):
    opt = "."
    libdir = jarsdir(basedir)
    dirs = os.listdir(libdir)
    for fname in dirs:
        opt += ":" + pathutil.pathjoin(libdir, fname)
    return opt
