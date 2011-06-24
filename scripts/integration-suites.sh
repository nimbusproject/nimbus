#!/bin/bash


ALL_TEST_SUITES="basic01 basic02 basic03 basic04 basic05 basic06 basic07 failure01 failure02 spot01 spot02 spot03 spot04 spot05 spot06"


if [ "X" == "X$1" ]; then
  echo "Must supply argument: test report directory absolute path"
  exit 1
fi

REPORTS_DIR=$1

# remaining arguments are specific tests to run. If unspecified, all will be run.
shift 1
RUN_TEST_SUITES=$@

if [ "X$RUN_TEST_SUITES" = "X" ]; then
    RUN_TEST_SUITES=$ALL_TEST_SUITES
fi

if [ -e $REPORTS_DIR ]; then
  echo "Directory exists, use a different argument: $REPORTS_DIR"
  exit 1
else
  echo "Creating reports directory: $REPORTS_DIR"
  mkdir $REPORTS_DIR
  if [ $? -ne 0 ]; then
  	echo "Failed to create reports directory: $REPORTS_DIR"
  	exit 1
  fi
fi

THISDIR_REL="`dirname $0`"
THISDIR=`cd $THISDIR_REL; pwd`
ANTFILE="$THISDIR/../service/service/java/tests/suites/build.xml"

BANNER="Nimbus -"
NIMBUS_PRINTNAME="Internal Service Integration Test Suites"

echo ""
echo " *** $BANNER $NIMBUS_PRINTNAME:"

ant -Dnimbussuites.test.reports.dir=$REPORTS_DIR -f $ANTFILE clean depclean
RET=$?
if [ $RET -ne 0 ]; then
  echo "PROBLEM: could not clean? exit code $RET - $BANNER $NIMBUS_PRINTNAME"
  exit $RET
fi

count=0
error_count=0
possible_count=0
FAILED_SUITES=""
TIMINGS="Timings:\n"
total_time=0

for test_suite in $RUN_TEST_SUITES; do
  possible_count=`expr $possible_count + 1`
done
for test_suite in $RUN_TEST_SUITES; do

  echo ""
  echo ""
  echo ""
  echo "****************************************************************************"
  echo "*********************    NIMBUS TEST SUITE    ******************************"
  echo "****************************************************************************"
  echo ""
  echo ""
  
  ms_before=`python -c "import time; print int(time.time()*1000)"`

  ant -Dnimbussuites.test.reports.dir=$REPORTS_DIR -f $ANTFILE $test_suite
  RET=$?

  if [ $RET -eq 0 ]; then
    echo "Test Suite Passed: $test_suite"
    count=`expr $count + 1`
  else
    echo "Test Suite Failed: $test_suite"
	error_count=`expr $error_count + 1`
	FAILED_SUITES="$test_suite $FAILED_SUITES"
  fi
  
  ms_after=`python -c "import time; print int(time.time()*1000)"`
  ms_this=`expr $ms_after - $ms_before`
  TIMINGS="$TIMINGS - $test_suite: $ms_this ms\n"
  total_time=`expr $total_time + $ms_this`
done


# Don't print this every time, mainly because it only uses the key names, not
# the real suite names.
#echo -e $TIMINGS


# Print result and exit with error if any suite failed.

echo -e "\n\n** $BANNER $NIMBUS_PRINTNAME:\n"
echo " - Took $total_time milliseconds."

if [ $possible_count -eq 1 ]; then
  echo " - 1 possible test suite."
else
  echo " - $possible_count possible test suites."
fi

if [ $count -eq 1 ]; then
  echo " - 1 test suite succeeded."
else
  echo " - $count test suites succeeded."
fi

if [ $error_count -eq 0 ]; then
  echo " - 0 test suites failed."
elif [ $error_count -eq 1 ]; then
  echo " - 1 test suite failed: $FAILED_SUITES"
else
  echo " - $error_count test suites failed: $FAILED_SUITES"
fi
echo ""

exit $error_count
