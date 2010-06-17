#!/bin/bash

BASEDIR=`dirname $0`

PYTHONPATH="$BASEDIR/lib/:$BASEDIR/lib/pylib:$PYTHONPATH"
export PYTHONPATH

python $BASEDIR/lib/nimbus_ctx_retrieve.py -c $BASEDIR/ctx.conf -z -t


