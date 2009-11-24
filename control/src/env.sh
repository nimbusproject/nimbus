# For sourcing before launching an interactive shell, only works from
# base directory:
# "source src/env.sh" 

NIMBUS_CONTROL_DIR=`python -c "import os; print os.path.abspath('.')"`
NIMBUS_CONTROL_PYLIB="$NIMBUS_CONTROL_DIR/lib/python"
NIMBUS_CONTROL_PYSRC="$NIMBUS_CONTROL_DIR/src/python"

PYTHONPATH="$NIMBUS_CONTROL_PYSRC:$NIMBUS_CONTROL_PYLIB:$PYTHONPATH"
export PYTHONPATH
