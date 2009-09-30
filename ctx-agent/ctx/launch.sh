#!/bin/sh

BASEDIR=`dirname $0`

PYTHONPATH="$BASEDIR/lib/pylib:$PYTHONPATH"
export PYTHONPATH

python $BASEDIR/lib/workspace_ctx_retrieve.py -c $BASEDIR/ctx.conf -z -t


