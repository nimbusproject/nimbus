#!/bin/bash -x

if [ "$1" = "help" ]; then
  echo ""
  echo "IMPORTANT: You must be in the globdev group to run this."
  echo "           If you use the 'withdocbook' flag, you must"
  echo "           be logged in to a node with OK Docbook to HTML"
  echo "           generation libraries."
  echo ""
  echo "Example run (with docbook):"
  echo "NOTE: currently cvs.globus.org is not a good docbook node"
  echo ""
  echo "       ssh pitcairn.mcs.anl.gov"
  echo "       mkdir somedir"
  echo "       cd somedir"
  echo "       export CVSROOT=:pserver:anonymous@cvs.globus.org:/home/globdev/CVS/globus-packages"
  echo "       cvs co -d docscripts workspace/docs/scripts"
  echo "       bash docscripts/push_to_mcs_web.sh noup withdocbook"
  echo ""
  echo "Example run (without docbook):"
  echo ""
  echo "       ssh somewhere.mcs.anl.gov"
  echo "       mkdir somedir"
  echo "       cd somedir"
  echo "       export CVSROOT=:pserver:anonymous@cvs.globus.org:/home/globdev/CVS/globus-packages"
  echo "       cvs co -d docscripts workspace/docs/scripts"
  echo "       bash docscripts/push_to_mcs_web.sh"
  echo ""
  echo "First arg can be update to only cvs up"
  exit 1
fi

# Local dir where the CVS checkout will go to, can be relative.
DIRNAME="workspace_docs"

# final output DIR:
BASE="/www/workspace.globus.org"

# directories to chown/chmod to globdev group
ONLINEDIRS="$BASE/da $BASE/vm $BASE/downloads $BASE/css $BASE/img $BASE/papers $BASE/talks"

# Docbook (not used anymore as of TP2.0)
DOCBOOK="$DIRNAME/docbook/vm/TP1.3.3"
DOCBOOK_OUT="$DIRNAME/toplevel/vm/TP1.3.3/doc"

export CVSROOT=:pserver:anonymous@cvs.globus.org:/home/globdev/CVS/globus-packages

if [ "$1" != "update" ]; then

if [ -d $DIRNAME ]; then
  echo "ERROR: directory \"$DIRNAME\" already exists:"
  echo "       Rather than deal with updates/conflicts/cruft, this"
  echo "       script always checks out docs to a new local directory"
  echo "       which must not exist before checkout (unless you give"
  echo "       the update flag)".
  exit 1
fi

else

if [ ! -d $DIRNAME ]; then
  echo "ERROR: directory \"$DIRNAME\" does not exist but you are"
  echo "       using the \"update\" option"
  exit 1
fi

fi

# assuming CVS HEAD

if [ "$1" != "update" ]; then

  echo ""
  echo "===================="
  echo "| New CVS checkout |"
  echo "===================="
  echo ""

  cvs co -P -d $DIRNAME workspace/docs

else

  echo ""
  echo "=============="
  echo "| CVS update |"
  echo "=============="
  echo ""

  (cd $DIRNAME; cvs -q update)

fi

if [ $? -ne 0 ]; then
  echo "CVS checkout failed, aborting."
  exit 1
fi

if [ ! -d $DIRNAME/scripts ]; then
  echo "ERROR: directory \"$DIRNAME/scripts\" does not exist after cvs co/update"
  exit 1
fi

chmod -R u+w $DIRNAME/toplevel
chmod -R u+w $DIRNAME/m4-processed

if [ "$2" = "withdocbook" ]; then

  echo ""
  echo "==========="
  echo "| Docbook |"
  echo "==========="
  echo ""

  /bin/bash $DIRNAME/scripts/export_docbook_html.sh $DOCBOOK $DOCBOOK_OUT
fi

echo ""
echo "================="
echo "| m4 processing |"
echo "================="
echo ""

/usr/bin/python $DIRNAME/scripts/process_m4.py -i $DIRNAME/toplevel -o $DIRNAME/m4-processed -l $DIRNAME/m4/worksp.lib.m4 -t /tmp/m4-tmp-workspace-site

find $DIRNAME/m4-processed -type d -name CVS -exec rm -rf {} \;

echo ""
echo "=================="
echo "| New or updated |"
echo "=================="
echo ""

# use checksums only for comparison
rsync -crlv $DIRNAME/m4-processed/ $BASE/

echo ""
echo "====================="
echo "| Group permissions |"
echo "====================="
echo ""

find $BASE -exec chown :workspace {} \;
find $BASE -type d -exec chmod 775 {} \;
find $BASE -type f -exec chmod 664 {} \;

echo ""
echo "Done."
