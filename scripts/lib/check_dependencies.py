#!/usr/bin/env python

import sys
import os
import subprocess

PYTHON_INFO = """
Nimbus requires Python 2.5 or later on the service node. 
However, the Python 3.x line is not yet supported.

If you have a supported Python installed but it is not first in your PATH, you
can set a PYTHON environment variable with the path the the python binary and
re-run this program.
"""

SQLITE_PYTHON_INFO = """
Your Python must have the sqlite3 module available.
"""

JAVA_INFO = """
Nimbus requires Java 1.5 or later. GCJ is not supported.
"""

JAVA_VERSION_UNKNOWN = """
You appear to have Java available but its version could not be determined.
Please verify it on your own.
"""

ANT_INFO = """
Apache Ant 1.6.2 or later is required.

You must also have the propertyfile task available which is usually installed
separately from Ant itself. On Redhat-compatible systems this is often a
package called ant-nodeps. On Debian/Ubuntu the package is ant-optional. To be
sure, you can check for the presence of a library called ant-nodeps.jar in your
Ant installation.
"""

LINE = """
--------------------------------------------------------------------------------
""".strip()

def problem(msg):
    print >>sys.stderr, LINE
    print >>sys.stderr, msg

def run_cmd(args):
    proc = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    return proc.communicate()

def check_java():
    java_home = os.environ.get('JAVA_HOME')
    if java_home:
        java_bin = os.path.join(java_home, 'bin/java')
        javac_bin = os.path.join(java_home, 'bin/javac')
    else:
        # find these on path
        java_bin = 'java'
        javac_bin = 'javac'

    try:
        out,err = run_cmd([java_bin, '-version'])
    except OSError:
        return "Could not run java executable: " + java_bin + JAVA_INFO
    output = out or err

    version = parse_java_version(output)
    if not version:
        return JAVA_VERSION_UNKNOWN + JAVA_INFO

    if version < (1, 5) or output.lower().find('gcj') != -1:
        return "Found Java: %s\n%s" % (output, JAVA_INFO)

def parse_java_version(output):
    if not output.startswith('java'):
        return None
    topline = output.splitlines()[0]
    ver = topline.split()[-1].strip('"')
    parts = ver.split('.')
    if len(parts) < 3:
        return None
    try:
        return(int(parts[0]), int(parts[1]))
    except ValueError:
        return None

def check_python():
    version = sys.version_info
    if version < (2, 5) or version >= (3,):
        return "Found Python %s\n%s%s" % (sys.version, PYTHON_INFO, 
                SQLITE_PYTHON_INFO)
    else:
        # only check for py modules if the Python is recent enough
        try:
            import sqlite3
        except ImportError:
            return SQLITE_PYTHON_INFO

def check_ant():
    try:
        out, err = run_cmd(['ant', '-version'])
    except OSError:
        return "Could not run ant.\n" + ANT_INFO

def check_nimbus_dependencies():
    problems = []

    for check in (check_python, check_java, check_ant):
        prob = check()
        if prob:
            if getattr(prob, '__iter__', False):
                problems.extend(prob)
            else:
                problems.append(prob)

    if not problems:
        print "Found no dependency problems."
        return 0

    if len(problems) > 1:
        s = 's'
    else:
        s = ''
    print "\nFound %s dependency problem%s:\n" % (len(problems), s)
    for problem in problems:
        print LINE
        print problem
    return len(problems)

def main():
    try:
        return check_nimbus_dependencies()
    
    except SystemExit:
        raise
    except KeyboardInterrupt:
        return 5
    except:
        exception_type = sys.exc_type
        try:
            exceptname = exception_type.__name__
        except AttributeError:
            exceptname = exception_type
        name = str(exceptname)
        err = str(sys.exc_value)
        errmsg = "\n==> Uncaught problem, please report all following output:\n  %s: %s" % (name, err)
        print >>sys.stderr, errmsg
        import traceback
        traceback.print_tb(sys.exc_info()[2])
        return 97

if __name__ == '__main__':
    sys.exit(main())
