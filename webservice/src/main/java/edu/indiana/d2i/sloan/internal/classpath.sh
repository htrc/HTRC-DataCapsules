#!/bin/sh
#
# This script sets required LOCALCLASSPATH and by default CLASSPATH 
# It must be run by source its content to modify current environment
#

# installation dir
this=$0
SCRIPT_HOME=`dirname "$this"`

LOCALCLASSPATH=.
LOCALCLASSPATH=`echo $SCRIPT_HOME/lib/*.jar | tr ' ' ':'`:$LOCALCLASSPATH

LOCALCLASSPATH=$LOCALCLASSPATH
CLASSPATH=$LOCALCLASSPATH
export CLASSPATH
echo $CLASSPATH
