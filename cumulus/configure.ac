AC_INIT("cumulus", 0.1)

AM_INIT_AUTOMAKE

AC_CHECK_HEADERS(openssl/ssl.h, [], [AC_MSG_ERROR(cannot find openssl headers)])

AC_PATH_PROG(SQLITE,sqlite3,no)
if test "$SQLITE" == no ; then
    AC_MSG_ERROR(Cannot find sqlite3.)
fi
AC_PATH_PROG(PYTHON,python2.5,no)
if test "$PYTHON" == no ; then
    AC_MSG_ERROR(Cannot find python2.5.)
fi

HOSTNAME=`hostname`
INSTALLDIR=$prefix
AC_SUBST(INSTALLDIR)
AC_SUBST(HOSTNAME)


AC_OUTPUT(Makefile 
    src/etc/cumulus.ini 
    src/etc/cumulus_tests.ini
    src/etc/cumulus_tests_https.ini
    src/etc/env.sh
    )
