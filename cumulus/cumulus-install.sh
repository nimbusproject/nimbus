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
    PYTHON=`which python`

    $PYTHON -c "import sys; sys.exit(sys.version_info < (2,5))"
    if [ $? -ne 0 ]; then
        echo "ERROR: Your system must have Python version 2.5 or later."
        exit 1
    fi

    if [ "X$PYTHON" == "X" ]; then
        echo "you must have python in your system path for installation"
        exit 1
    fi

    if [ -e $installdir ]; then
        echo "----- WARNING -----"
        echo "Target directory already exists"
        bkup_dir="$installdir".`date +%s`
        echo "moving existing directory to $bkup_dir"
        mv $installdir $bkup_dir
    fi 

    echo "Making the Python virtual environment."
    echo ""
    $PYTHON $source_dir/virtualenv.py -p $PYTHON $installdir
    if [ $? -ne 0 ]; then
        echo "The virtural env installation failed"
        exit 1
    fi

    PYVE=$installdir/bin/python
    PYVEDIR=$installdir
    PIP=$installdir/bin/pip
else
    use_py=$2
    echo "Using provided Python environment: $use_py"
    echo ""

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

source $PYVEDIR/bin/activate

cd $source_dir/deps
if [ $? -ne 0 ]; then
    echo "Could not change to the deps directory"
    exit 1
fi
./get-em.sh
if [ $? -ne 0 ]; then
    echo "get-em failed"
    exit 1
fi

if [ ! -e $PIP ]; then
    cd $source_dir
    tar -zxf pip-0.7.2.tar.gz
    if [ $? -ne 0 ]; then
        echo "unable to untar pip-0.7.2.tar.gz"
        exit 1
    fi
    cd $source_dir/pip-0.7.2
    $PYVE setup.py install
    if [ $? -ne 0 ]; then
        echo "pip was not installed correctly"
        exit 1
    fi
fi

echo ""
echo "-----------------------------------------------------------------"
echo "Installing Cumulus dependencies"
echo "-----------------------------------------------------------------"
echo ""
# install deps
cd $source_dir
$PIP install  --requirement=reqs.txt
if [ $? -ne 0 ]; then
    echo "$PIP failed to install deps"
    exit 1
fi

echo "installing authz"
echo "----------------"
cd authz
$PYVE setup.py install
if [ $? -ne 0 ]; then
    echo "$PIP failed to install authz"
    exit 1
fi
cd $source_dir
echo "installing cb"
echo "-------------"
cd cb
$PYVE setup.py install
if [ $? -ne 0 ]; then
    echo "$PIP failed to install authz"
    exit 1
fi


echo ""
echo "-----------------------------------------------------------------"
echo "Configuring the environment"
echo "-----------------------------------------------------------------"
echo ""
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

echo ""
echo "-----------------------------------------------------------------"
echo "Finalizing the Cumulus install"
echo "-----------------------------------------------------------------"
echo ""
cd $source_dir
cp -r $source_dir/tests $installdir
cp -r $source_dir/docs $installdir
