#!/usr/bin/python

import os, sys, re, shutil, string, getopt

M4PATH = "/usr/bin/m4"
CPPATH = "/bin/cp"

# These three can be overriden by commandline arguments
# Relative paths are taken from cwd of program caller, not program
DEFAULT_INPUTPATH = "../src"
DEFAULT_TEMPPATH = "../m4-tmp"
DEFAULT_OUTPUTPATH = "../html"
DEFAULT_STDLIBPATH = "../m4/worksp.lib.m4"

verbose_mode = False
changed_header_count = 0
inserted_header_count = 0
processed_count = 0
nonprocessed_count = 0

# with os.path.walk
def print_filenames(arg, dirname, names):
    for name in names:
        if name[-5:] == ".html":
            print name
            
# with os.path.walk
def delete_CVS_dir(arg, dirname, names):
    if dirname[-3:] == "CVS":
        shutil.rmtree(dirname)
            
# with os.path.walk
def add_m4_include(arg, dirname, names):

    global changed_header_count
    global inserted_header_count

    #print "dir: " + dirname
    if dirname[-3:] == "CVS":
        return

    if not arg:
        raise Exception("m4 lib path not configured, see example in main()")

    regex = re.compile("m4_include")
    for name in names:
        if name[-5:] != ".html":
            continue
            
        path = os.path.join(dirname, name)
        try:
            f = open(path)
            oldlines = f.readlines()
            f.close()
            if len(oldlines) < 1:
                print "found empty file '%s', not processing it" % path
            else:
                mobj = regex.search(oldlines[0])
                newlines = None
                # if an m4 include statement is present, change it if necc.
                if mobj:
                    shouldbe = "m4_include(%s)\n" % arg
                    newlines = oldlines
                    if newlines[0] != shouldbe:
                        newlines[0] = shouldbe
                        changed_header_count += 1
                        if verbose_mode:
                            print "Changing m4 header: %s" % path
                    else:
                        newlines = None
                
                # if not present, insert it
                else:
                    newlines = ["m4_include(%s)\n" % arg]
                    newlines.extend(oldlines)
                    if verbose_mode:
                        print "Inserting m4 header: %s" % path
                    inserted_header_count += 1
                    
                if newlines:
                    f = open(path, 'w')
                    for line in newlines:
                        f.write(line)
        finally:
            f.close()
                
# with os.path.walk
def process_m4(args, dirname, names):
    """So much nicer than a Makefile"""

    global processed_count
    global nonprocessed_count

    if len(args) < 2:
        raise Exception("in or out path not configured, see example in main()")

    if not args[0] or not args[1]:
        raise Exception("in or out path not configured, see example in main()")

    inputdir = args[0]
    outputdir = args[1]

    #print "dir: " + dirname
    if dirname[-3:] == "CVS":
        return
    
    regex = re.compile("(.*)(%s)(.*)" % inputdir)
    mobj = regex.search(dirname)
    if mobj:
        outputdir = outputdir + mobj.group(3)
    else:
        raise Exception("no mobj?")
        
    if not os.path.exists(outputdir):
        os.mkdir(outputdir)
        if verbose_mode:
            print "Created directory %s" % outputdir
        
    for name in names:
        path = os.path.join(dirname, name)
        outpath = os.path.join(outputdir, name)
        if os.path.isdir(path):
            continue
        
        if name[-5:] != ".html":
            cmd = "%s %s %s" % (CPPATH, path, outpath)
            ret = os.system(cmd)
            if ret:
                print "cmd failed: %s" % cmd
            else:
                nonprocessed_count += 1
                if verbose_mode:
                    print "Added %s" % outpath
        else:
            cmd = "%s -P <%s >%s" % (M4PATH, path, outpath)
            ret = os.system(cmd)
            if ret:
                print "cmd failed: %s" % cmd
            else:
                processed_count += 1
                if verbose_mode:
                    print "Processed %s" % outpath


class UsageError(Exception):
    def __init__(self, msg):
        self.msg = msg
    def __str__(self):
        return self.msg

def absolute(path):
    x = string.strip(path)
    return os.path.realpath(x)

def help():
    print "Usage: %s [options]\n" % sys.argv[0]
    print "  -i,--inputdir   $path     Overrides doc source directory"
    print "                            Default: %s" % absolute(DEFAULT_INPUTPATH)
    print "\n  -o,--outputdir  $path     Overrides doc output directory"
    print "                            Default: %s" % absolute(DEFAULT_OUTPUTPATH)
    print "\n  -t,--tempdir  $path       Overrides doc temp directory"
    print "                            Default: %s" % absolute(DEFAULT_TEMPPATH)
    print "\n  -l,--stdlibpath $path     Overrides m4 library path"
    print "                            Default: %s" % absolute(DEFAULT_STDLIBPATH)
    print "\n  -v,--verbose            Verbose mode"

def main(argv=None):
    if argv is None:
        argv = sys.argv

    inputdir = None
    outputdir = None
    stdlibpath = None
    tempdir = None

    global verbose_mode
    
    try:
        ## Collect ##
        #if len(argv) < 2:
        #    raise UsageError("arguments?")
        try:
            opts, args = getopt.getopt(argv[1:],
                                       "hi:o:l:t:v",
                                       ["help", "inputdir=", "outputdir=",
                                        "stdlibpath=", "tempdir=", "verbose"])
        except getopt.error, msg:
             raise UsageError(msg)

        ## Assign ##
        for o,a in opts:
            if o in ("-h", "--help"):
                help()
                return 0
            elif o in ("-i", "--inputdir"):
                inputdir = a
            elif o in ("-o", "--outputdir"):
                outputdir = a
            elif o in ("-l", "--stdlibpath"):
                stdlibpath = a
            elif o in ("-t", "--tempdir"):
                tempdir = a
            elif o in ("-v", "--verbose"):
                verbose_mode = True

        if inputdir:
            inputdir = absolute(inputdir)
        else:
            inputdir = absolute(DEFAULT_INPUTPATH)

        if outputdir:
            outputdir = absolute(outputdir)
        else:
            outputdir = absolute(DEFAULT_OUTPUTPATH)

        if stdlibpath:
            stdlibpath = absolute(stdlibpath)
        else:
            stdlibpath = absolute(DEFAULT_STDLIBPATH)

        for i in (inputdir, outputdir, stdlibpath):
            if not i:
                raise Exception("don't erase defaults")

            if not os.path.exists(i):
                raise Exception("'%s' does not exist" % i)
            x = os.access(i, os.R_OK)
            if not x:
                raise Exception("'%s' exists on the filesystem but is not readable" % i)

        x = os.access(inputdir, os.X_OK | os.R_OK)
        if not x:
            raise Exception("'%s' exists on the filesystem but is not rx-able" % inputdir)

        x = os.access(outputdir, os.W_OK | os.X_OK | os.R_OK)
        if not x:
            raise Exception("'%s' exists on the filesystem but is not rwx-able" % outputdir)

        if tempdir:
            tempdir = absolute(tempdir)
        else:
            tempdir = absolute(DEFAULT_TEMPPATH)

        if os.path.exists(tempdir):
            raise Exception("'%s' exists already?" % tempdir)

        print "Input  directory: %s" % inputdir
        print "Temp directory: %s" % tempdir
        print "Output directory: %s" % outputdir
        print "m4 macro library: %s" % stdlibpath

        for i in (M4PATH, CPPATH):
            if not os.path.exists(i):
                raise Exception("'%s' does not exist" % i)

            x = os.access(i, os.X_OK)
            if not x:
                raise Exception("'%s' exists on the filesystem but is not executable" % i)

        print "\n*** Copying source to temp directory... ",
        sys.stdout.flush()
        shutil.copytree(inputdir, tempdir)
        print "done."
        
        print "*** Removing CVS dirs from temp directory... ",
        sys.stdout.flush()
        os.path.walk(tempdir, delete_CVS_dir, None)
        print "done."
        
        # must be run from scripts subdir for now
        # cvs.globus.org has Python version < 2.3 which is when
        # the "os.walk" function was introduced, so instead using
        # os.path.walk

        if verbose_mode:
            print "\n\n"
        print "*** Adding/changing m4 library references"
        os.path.walk(tempdir, add_m4_include, stdlibpath)
        if verbose_mode:
            print "\n\n"

        if verbose_mode:
            print "\n\n"
        print "*** Processing m4"
        os.path.walk(tempdir, process_m4, (tempdir,outputdir))
        if verbose_mode:
            print "\n\n"

        shutil.rmtree(tempdir)
        print "*** Removed temp directory"
        print "*** Summary:"
        print "    New m4 headers     : %d" % inserted_header_count
        print "    Altered m4 headers : %d" % changed_header_count
        print "    m4 processed files : %d" % processed_count
        print "    Other files        : %d" % nonprocessed_count

    except UsageError,err:
        print >>sys.stderr, err.msg
        print >>sys.stderr, "for help use --help"
        return 2

    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__
        except AttributeError:
            exceptname = exception_type
        print >>sys.stderr, "ERROR: %s: %s" % (str(exceptname), str(sys.exc_value))
        return 1

if __name__ == '__main__':
    sys.exit(main())
    
    