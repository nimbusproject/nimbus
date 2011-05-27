#!/bin/bash

tests="cc-prop2-test.py cc-unprop-md5test.py cc-propsamename-test.py cc-unprop-readwrite-test.py cc-prop-test.py cc-unprop-size-test.py cc-submit-common-unprop-size-test.py"

old='<property name="repoScheme" value="scp" />'
new='<property name="repoScheme" value="cp" />'

conf="${NIMBUS_HOME}/services/etc/nimbus/workspace-service/other/authz-callout-ACTIVE.xml"

bkup=`mktemp`
cp $conf $bkup

${NIMBUS_HOME}/bin/nimbusctl restart

sed -i 's^$old^$new^' $conf

echo "setting up nimbus to use the cp propagation adapter"

for tst in $tests
do
    echo "running $tst"
    ./tst

done

echo "return to default propagation adapter"
cp $bkup $conf
${NIMBUS_HOME}/bin/nimbusctl restart
