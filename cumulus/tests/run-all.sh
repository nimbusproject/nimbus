#!/bin/bash

echo "Running without security"
echo "=========================="
./run-pass.sh
if [ $? -ne 0 ]; then
    echo "Insecure tests failed"
    exit 1
fi
export CUMULUS_TEST_HTTPS="True"
echo "Running with security"
echo "=========================="
./run-pass.sh
if [ $? -ne 0 ]; then
    echo "Secure tests failed"
    exit 1
fi
