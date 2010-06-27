#! /bin/bash

if ([ "X$1" == "X--help" ] || [ "X$1" == "X-h" ]); then
    echo "cumulus-install.sh <installation directory> [<path to python install directory>]"
    echo "  The python path should be the path to the install.  ./bin/python and ./bin/pip will be appened." 
    echo "  If not path to python is specified a virtual environment will be created"
    exit 0
fi

installdir=$1
start_dir=`pwd`
source_dir=`dirname $0`
cd $source_dir
source_dir=`pwd`

# if no
if [ "X$2" == "X" ]; then
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
    $PYTHON $source_dir/virtualenv.py -p $PYTHON $installdir

    if [ ! -e $HOME/.nimbus/ ]; then
        mkdir $HOME/.nimbus/
        if [ $? -ne 0 ]; then
            echo "get-em failed"
            exit 1
        fi
    fi
    if [ -e $HOME/.nimbus/cumulus.ini ]; then 
        echo "----- WARNING -----"
        bkup=$HOME/.nimbus/cumulus.ini.`date +%s`
        echo "$HOME/.nimbus/cumulus.ini exists, moving it to $bkup"
        mv $HOME/.nimbus/cumulus.ini $bkup
    fi

    PYVEDIR=$installdir
    PYVE=$installdir/bin/python
    PIP=$installdir/bin/pip
else
    use_py=$2
    echo "====================================="
    echo "Using the provided python $use_py"
    echo "====================================="

    PYVEDIR=$use_py
    PYVE=$use_py/bin/python
    PIP=$use_py/bin/pip
    $PYVE -c "import sys; sys.exit(sys.version_info < (2,5))"
    if [ $? -ne 0 ]; then
        echo $use_py
        $use_py --version
        echo "ERROR: Your system must have Python version 2.5 or later."
        exit 1
    fi

fi

cd $source_dir/deps
if [ $? -ne 0 ]; then
    echo "Could not change to the deps directory"
    exit 1
fi
pwd
./get-em.sh
if [ $? -ne 0 ]; then
    echo "get-em failed"
    exit 1
fi

echo "====================================="
echo "Installing the dependencies"
echo "====================================="
# install deps
cd $source_dir
$PIP install  --requirement=reqs.txt
if [ $? -ne 0 ]; then
    echo "$PIP failed to install deps"
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
if [ $? -ne 0 ]; then
    echo "setup.py"
    exit 1
fi

echo "====================================="
echo "Configuring the environment"
echo "====================================="
cd $source_dir/conf
./configure --prefix=$installdir  --with-ve=$PYVEDIR
if [ $? -ne 0 ]; then
    echo "configure failed"
    exit 1
fi
make install
if [ $? -ne 0 ]; then
    echo "make install failed"
    exit 1
fi

echo "====================================="
echo "Final copies"
echo "====================================="
cd $source_dir
cp -r $source_dir/tests $installdir
cp -r $source_dir/docs $installdir
cp -r $source_dir/bin $installdir

