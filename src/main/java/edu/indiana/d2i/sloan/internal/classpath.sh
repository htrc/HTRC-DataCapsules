#!/bin/sh
#
# This script sets required LOCALCLASSPATH and by default CLASSPATH 
# It must be run by source its content to modify current environment
#

LOCALCLASSPATH=.
LOCALCLASSPATH=`echo lib/*.jar | tr ' ' ':'`:$LOCALCLASSPATH

LOCALCLASSPATH=$LOCALCLASSPATH
CLASSPATH=$LOCALCLASSPATH
export CLASSPATH
echo $CLASSPATH