#! /bin/bash

if ([ "X$1" == "X--help" ] || [ "X$1" == "X-h" ]); then
    echo "cumulus-install.sh [<installation directory>]"
    exit 0
fi

installdir=$1
start_dir=`pwd`
source_dir=`dirname $0`
cd $source_dir
source_dir=`pwd`

PYTHON=`which python2.5`

if [ "X$PYTHON" == "X" ]; then
    echo "you must have python2.5 in your system path for installation"
    exit 1
fi

if [ -e $installdir ]; then
    echo "----- WARNING -----"
    echo "Target directory already exists"
    bkup_dir="$installdir".`date +%s`
    echo "moving existing directory to $bkup_dir"
    mv $installdir $bkup_dir
fi 

echo "====================================="
echo "Making the python virtual env for cumulus"
echo "====================================="
$PYTHON $source_dir/virtualenv.py -p $PYTHON --no-site-packages $installdir

if [ -e $HOME/.nimbus/cumulus.ini ]; then 
    echo "----- WARNING -----"
    bkup=$HOME/.nimbus/cumulus.ini.`date +%s`
    echo "$HOME/.nimbus/cumulus.ini exists, moving it to $bkup"
    mv $HOME/.nimbus/cumulus.ini $bkup
fi

PYVE=$installdir/bin/python

echo "====================================="
echo "Installing the dependencies"
echo "====================================="
# install deps
cd $source_dir
$installdir/bin/pip install  --requirement=reqs.txt
if [ $? -ne 0 ]; then
    echo "pip failed to install deps"
    exit 1
fi

echo "====================================="
echo "Installing the authz package"
echo "====================================="
cd $source_dir/authz/
$PYVE ./setup.py install 

echo "====================================="
echo "Installing the cumulus package"
echo "====================================="
cd $source_dir/cb
$PYVE ./setup.py install 


echo "====================================="
echo "Configuring the environment"
echo "====================================="
cd $source_dir/conf
./configure --prefix=$installdir
make install

echo "====================================="
echo "Final copies"
echo "====================================="
cd $source_dir
cp -r $source_dir/tests $installdir
cp -r $source_dir/docs $installdir

