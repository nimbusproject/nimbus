#! /bin/bash

if ([ "X$1" == "X--help" ] || [ "X$1" == "X-h" ]); then
    echo "cumulus-install.sh [<installation directory>]"
    exit 0
fi

installdir=$1
start_dir=`pwd`
source_dir=`dirname $0`

echo $installdir

if [ -e $installdir ]; then
    echo "----- WARNING -----"
    echo "Target directory already exists"
    bkup_dir="$installdir".`date -I`
    echo "moving existing directory to $bkup_dir"
    mv $installdir $bkup_dir
fi 

if [ -e $HOME/.nimbus/cumulus.ini ]; then 
    echo "----- WARNING -----"
    bkup=$HOME/.nimbus/cumulus.ini.`date -I`
    echo "$HOME/.nimbus/cumulus.ini exists, moving it to $bkup"
    mv $HOME/.nimbus/cumulus.ini $bkup
fi

./configure --prefix=$installdir
make install
