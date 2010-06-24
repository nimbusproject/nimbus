#!/bin/bash

cd ..
source env.sh
cd -

echo "Cleaning up"
echo "==========="

sqlite3 -line $NIMBUS_AUTHZ_DB "delete from user_alias where friendly_name like '%nosetests.nimbus.org'"


echo "Running without security"
echo "=========================="

export CUMULUS_SETTINGS_FILE=${CUMULUS_HOME}/etc/cumulus_tests.ini
./run-pass.sh
if [ $? -ne 0 ]; then
    echo "Insecure tests failed"
    exit 1
fi

export CUMULUS_SETTINGS_FILE=${CUMULUS_HOME}/etc/cumulus_tests_https.ini
export CUMULUS_TEST_HTTPS="True"
echo "Running with security"
echo "=========================="
./run-pass.sh
if [ $? -ne 0 ]; then
    echo "Secure tests failed"
    exit 1
fi
