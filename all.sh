#!/bin/bash

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

PRGDIR=`dirname "$PRG"`
BASEDIR=`cd "$PRGDIR" >/dev/null; pwd`

mkdir -p $BASEDIR/output

$BASEDIR/run.sh -basedir $BASEDIR -configfile config/config-esnet.xml
$BASEDIR/run.sh -basedir $BASEDIR -configfile config/config-manlan.xml
$BASEDIR/run.sh -basedir $BASEDIR -configfile config/config-caltech.xml
$BASEDIR/run.sh -basedir $BASEDIR -configfile config/config-umich.xml
$BASEDIR/run.sh -basedir $BASEDIR -configfile config/config-wix.xml

