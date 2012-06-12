#/bin/bash
urls="http://www.nimbusproject.org/downloads/cumulus-deps.tar.gz http://github.com/downloads/nimbusproject/nimbus/cumulus-deps.tar.gz"

if [ -f cumulus-deps.tar.gz ]; then
    exit 0
fi
for url in $urls
do
    echo "trying to get from $url"
    wget --no-check-certificate $url
    if [ $? -ne 0 ]; then
        echo "wget failed"
        continue
    fi
    tar -C ../ -zxvf cumulus-deps.tar.gz
    if [ $? -ne 0 ]; then
        echo "untar failed"
        rm cumulus-deps.tar.gz
        continue
    fi
    exit 0
done
exit 1
