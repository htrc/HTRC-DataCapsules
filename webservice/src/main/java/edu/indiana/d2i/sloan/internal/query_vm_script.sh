#!/bin/sh

# installation dir
this=$0
SCRIPT_HOME=`dirname "$this"`

# the file path pointing to the shell script
# that conducts error message lookup based
# on error code
GET_MSG_SHELL_FILEPATH="$SCRIPT_HOME/errcode2msg.sh"

# VM query result file name
QUERY_RES_FILENAME="vmstate-query-result.txt";

# should put commons-cli-1.2.jar, commons-lang-2.6.jar,
# commons-io-2.4.jar and sloan-ws-1.0-SNAPSHOT.jar
# under lib folder
LOCALCLASSPATH=`/bin/sh $SCRIPT_HOME/classpath.sh run`

LOG_FILE=$SCRIPT_HOME/log/simulator.log

java -cp $LOCALCLASSPATH edu.indiana.d2i.sloan.internal.QueryVMSimulator $@ >> $LOG_FILE 2>&1

# save the exit code
EXIT_CODE=$?

# print error message if not a success
if [ $EXIT_CODE -ne 0 ]; then
	ERR_MSG=`$GET_MSG_SHELL_FILEPATH $EXIT_CODE`
	# print error code
	echo "$EXIT_CODE"
	# print error message
	echo "Error code: $EXIT_CODE; Error message: $ERR_MSG"
else
	# print the VM state information
	echo "0"
	echo "Success"
	cat "$2/$QUERY_RES_FILENAME"
fi

# exit with the previous exit code
exit $EXIT_CODE
