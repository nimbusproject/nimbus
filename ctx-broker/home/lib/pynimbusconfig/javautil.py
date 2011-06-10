import os
from pynimbusconfig import pathutil
from pynimbusconfig import runutil
from pynimbusconfig.setuperrors import IncompatibleEnvironment

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
    return runutil.runexe(cmd, log, killtime=0)

def jarsdirs(basedir):
    jarsdirs = []
    for dirname in ["lib/java", "services/lib"]:
        d = pathutil.pathjoin(basedir, dirname)
        if os.path.exists(d):
            jarsdirs.append(d)
    return jarsdirs
    
def classpath(basedir):
    opt = "."
    libdirs = jarsdirs(basedir)
    for libdir in libdirs:
        fnames = os.listdir(libdir)
        for fname in fnames:
            if fname.endswith(".jar"):
                opt += ":" + pathutil.pathjoin(libdir, fname)
    return opt
