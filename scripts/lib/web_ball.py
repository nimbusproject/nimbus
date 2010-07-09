from fabric.api import cd,run
from datetime import datetime
import os
import uuid

def newball(builddir=None, webdir=None, printbase=None, gitref=None):
    
    if not builddir:
        raise Exception("builddir required")
    if not webdir:
        raise Exception("webdir required")
    if not printbase:
        raise Exception("printbase required")
    if not gitref:
         raise Exception("gitref required")   
        
    _checknode(builddir)
    _pull_and_reset(builddir, gitref)
    result_dir = _make_dist(builddir)
    filelist = _make_available(result_dir, webdir)
    
    print "\n\n"
    for f in filelist:
        print "%s%s/%s" % (printbase, os.path.basename(result_dir), f)
    print "\n\n"
    
    
# ---------------------------------------------------------------------

def _checknode(builddir):
    print "\n* Remote node status"
    with cd(builddir):
        run("git log | head -n 6")

def _pull_and_reset(builddir, gitref):
    print "\n* Resetting git against '%s'" % gitref
    with cd(builddir):
        
        # first make sure nothing is tainting it, these two commands will
        # return non-zero if there is something uncommitted in the directory
        # for some weird reason
        run("git diff --quiet --exit-code")
        run("git diff --quiet --exit-code --cached")
        
        # use pull ant not just fetch so that HEAD becomes relevant
        run("git pull")
        run("git reset --hard %s" % gitref)

def _make_dist(builddir):
    print "\n* Running make-dist"
    
    # make a unique directory for all of the tarballs, easier to reason
    # about the result this way
    now = datetime.now()
    basename = "%.4d-%.2d-%.2d__%.2d-%.2d__%s" % (now.year, now.month, now.day, now.hour, now.minute, str(uuid.uuid4()))
    result_dir = os.path.join("/tmp/", basename)
    
    with cd(builddir):
        run("mkdir %s" % result_dir)
        run("./scripts/make-dist.sh %s %s" % (result_dir, builddir))

    return result_dir

def _make_available(result_dir, webdir):
    
    target_dir = os.path.join(webdir, os.path.basename(result_dir))
    
    # fail if target directory exists:
    run("[ ! -e %s ] || /bin/false" % target_dir)
    
    run("mv %s %s" % (result_dir, webdir))
    
    with cd(target_dir):
        files = run("ls")
        filelist = files.split("\n")

    return filelist
    