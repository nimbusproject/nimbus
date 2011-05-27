#!/bin/bash

tests="cc-prop2-test.py cc-unprop-md5test.py cc-propsamename-test.py cc-unprop-readwrite-test.py cc-prop-test.py cc-unprop-size-test.py cc-submit-common-unprop-size-test.py"

old='scp'
new='cp'

conf="${NIMBUS_HOME}/services/etc/nimbus/workspace-service/other/authz-callout-ACTIVE.xml"

bkup=`mktemp`
cp $conf $bkup

echo "setting up nimbus to use the cp propagation adapter"

cmd="sed -i s^$old^$new^ $conf"
echo $cmd
$cmd
${NIMBUS_HOME}/bin/nimbusctl restart

function on_exit()
{
    echo "return to default propagation adapter"
    mv $bkup $conf
    ${NIMBUS_HOME}/bin/nimbusctl restart
}

trap on_exit EXIT

for tst in $tests
do
    echo "running $tst"
    ./$tst
    if [ $? -ne 0 ]; then
       echo "FAILED"
       exit 1
    fi
done

exit 0
