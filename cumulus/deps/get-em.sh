#/bin/bash

if [ ! -f cumulus-deps.tar.gz ]; then
    wget http://www.mcs.anl.gov/~bresnaha/cumulus/cumulus-deps.tar.gz
    if [ $? -ne 0 ]; then
        echo "wget failed"
        exit 1
    fi
    tar -C ../ -zxvf cumulus-deps.tar.gz
    if [ $? -ne 0 ]; then
        echo "wget failed"
        exit 1
    fi
fi
exit 0
