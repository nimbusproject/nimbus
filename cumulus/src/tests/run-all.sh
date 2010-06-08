#!/bin/bash

cd ..
source env.sh
cd -

echo "Running without security"
echo "=========================="

export CUMULUS_SETTINGS_FILE=${CUMULUS_HOME}/tests/cumulus.ini
./run-pass.sh
if [ $? -ne 0 ]; then
    echo "Insecure tests failed"
    exit 1
fi

export CUMULUS_SETTINGS_FILE=${CUMULUS_HOME}/tests/cumulus_https.ini
export CUMULUS_TEST_HTTPS="True"
echo "Running with security"
echo "=========================="
./run-pass.sh
if [ $? -ne 0 ]; then
    echo "Secure tests failed"
    exit 1
fi
