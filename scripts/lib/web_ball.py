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
    current_commit_hash = _pull_and_reset(builddir, gitref)
    result_dir = _make_dist(builddir, current_commit_hash)
    (filelist, md5sums) = _make_available(result_dir, webdir)
    
    seppi = "----------------------------------------------------------------"
    print "\n\n%s" % seppi
    print "Ref given to tarball creator: %s" % gitref
    print "Commit that tarballs are based on: %s" % current_commit_hash
    print "%s\nDownloads:" % seppi
    for f in filelist:
        print "%s%s/%s" % (printbase, os.path.basename(result_dir), f)
    print "%s\nTarball MD5 sums:" % seppi
    print md5sums
    print seppi
    
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
        
        # use pull ant not just fetch so that HEAD becomes relevant if that
        # is what is supplied as the gitref
        run("git pull")
        run("git reset --hard %s" % gitref)
        
        # HEAD here is whatever tip is after the reset, report back exactly
        # what ended up being used
        current_commit_hash = run("git rev-parse HEAD")
        return current_commit_hash

def _make_dist(builddir, current_commit_hash):
    print "\n* Running make-dist"
    
    # make a unique directory for all of the tarballs, easier to reason
    # about the result this way
    now = datetime.now()
    basename = "%.4d-%.2d-%.2d_%.2d%.2d_%s" % (now.year, now.month, now.day, now.hour, now.minute, current_commit_hash[:8])
    result_dir = os.path.join("/tmp/", basename)
    
    with cd(builddir):
        run("mkdir %s" % result_dir)
        run("./scripts/make-dist.sh %s %s" % (result_dir, builddir))
        run("./scripts/broker-make-dist.sh %s %s" % (result_dir, builddir))

    return result_dir

def _make_available(result_dir, webdir):
    
    target_dir = os.path.join(webdir, os.path.basename(result_dir))
    
    # fail if target directory exists:
    run("[ ! -e %s ] || /bin/false" % target_dir)
    
    run("mv %s %s" % (result_dir, webdir))
    
    with cd(target_dir):
        files = run("ls -1")
        filelist = files.split("\n")
        md5sums = run("md5sum *")

    return (filelist, md5sums)
    
