AC_INIT("cumulus", 0.1)

AM_INIT_AUTOMAKE

AC_CHECK_HEADERS(openssl/ssl.h, [], [AC_MSG_ERROR(cannot find openssl headers)])

AC_PATH_PROG(SQLITE,sqlite3,no)
if test "$SQLITE" == no ; then
    AC_MSG_ERROR(Cannot find sqlite3.)
fi

AC_ARG_WITH([ve],
            [AS_HELP_STRING([--with-ve],
              [specify the virtual env directory.  Default is prefix])],
            [with_ve=$withval],
            [with_ve=$prefix])

AUTHZDB=$prefix/etc/authz.db
if test "X$CUMULUS_AUTHZDB" != "X" ; then
    AUTHZDB=$CUMULUS_AUTHZDB
fi

VEDIR=$with_ve          
PYVERSUB=`$VEDIR/bin/python -c 'import sys; print "python%d.%d" % (sys.version_info[[0]], sys.version_info[[1]]); sys.exit(0);'`
# must verify cumulus and pynimbusauthz are installed
HOSTNAME=`hostname`
INSTALLDIR=$prefix
AC_SUBST(INSTALLDIR)
AC_SUBST(HOSTNAME)
AC_SUBST(VEDIR)
AC_SUBST(PYVERSUB)
AC_SUBST(AUTHZDB)

AC_OUTPUT(Makefile 
    etc/cumulus.ini 
    etc/cumulus_tests.ini
    etc/cumulus_tests_https.ini
    etc/env.sh
    )
