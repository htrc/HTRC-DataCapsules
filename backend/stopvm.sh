#!/bin/bash

# Copyright 2013 University of Michigan
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#     http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

SCRIPT_DIR=$(cd $(dirname $0); pwd)
. $SCRIPT_DIR/capsules.cfg

usage () {

  echo "Usage: $0 <Directory for VM>"
  echo ""
  echo "Brings a running VM instance immediately to the Stopped state by killing its"
  echo " corresponding qemu process.  To the end user, this has the same effect as"
  echo " 'pulling the plug' on the guest machine."
  echo ""
  echo "(--wdir)  Directory: The directory where this VM's data will be held"

}

REQUIRED_OPTS="VM_DIR"
UNDEFINED=12345capsulesxXxXxundefined54321

for var in $REQUIRED_OPTS; do
  eval $var=$UNDEFINED
done

if [[ $1 && $1 != -* ]]; then
  VM_DIR=$1
  shift
fi

declare -A longoptspec
longoptspec=( [wdir]=1 )
optspec=":h-:d:"
while getopts "$optspec" OPT; do

  if [[ "x${OPT}x" = "x-x" ]]; then
    if [[ "${OPTARG}" =~ .*=.* ]]; then
      OPT=${OPTARG/=*/}
      OPTARG=${OPTARG#*=}
      ((OPTIND--))
    else #with this --key value1 value2 format multiple arguments are possible
      OPT="$OPTARG"
      OPTARG=(${@:OPTIND:$((longoptspec[$OPT]))})
    fi
    ((OPTIND+=longoptspec[$OPT]))
  fi

  case "${OPT}" in
    d|wdir)
      VM_DIR=$OPTARG
      ;;
    h|help)
      usage
      exit 0
      ;;
    *)
      echo "error: Invalid argument '--${OPT}'"
      usage
      exit 1
      ;;
  esac
done

MISSING_ARGS=0
for var in $REQUIRED_OPTS; do
  if [[ ${!var} = $UNDEFINED ]]; then
    echo "error: $var not set"
    MISSING_ARGS=1
  fi
done

if [[ $MISSING_ARGS -eq 1 ]]; then
  usage
  exit 1
fi

if [ ! -d $VM_DIR ] ; then
  echo "Error: Invalid VM directory specified!"
  exit 2
fi

# Load config file
. $VM_DIR/config

# Make sure there's actually something to kill
if [ -e $VM_DIR/pid ]; then

  VM_PID=`cat $VM_DIR/pid`
 
  if kill -0 $VM_PID 2>&1 | grep -q "No such process" ; then
    echo "Error: There appears to be no VM instance running"
    exit 3

  else

    if [ -e $VM_DIR/mode ] ; then
      CUR_MODE=`cat $VM_DIR/mode`
    else
      CUR_MODE="Maintenance"
    fi

    if [[ $CUR_MODE = "Maintenance" ]] ; then
      echo "commit all" | nc -U $VM_DIR/monitor >/dev/null
    #else
      #echo "stop" | nc -U $VM_DIR/monitor >/dev/null
      #$SCRIPT_DIR/switch.sh $VM_DIR --mode m
      #SWITCH_RES=$?

      #if [ $SWITCH_RES -ne 0 ]; then
      #  echo "Error: Failed to return VM to maintenance mode; error code ($SWITCH_RES)"
      #  exit 4
      #fi
    fi

    echo "quit" | nc -U $VM_DIR/monitor >/dev/null

    if pidof `basename $QEMU` | grep -q $VM_PID; then
      
      KILL_RES=$(kill $VM_PID 2>&1)
  
      if [ $? -ne 0 ]; then
        echo "Error killing qemu process: $KILL_RES"
        exit 5
      fi

    fi
  fi

  FILES_TO_DELETE=""

  if [ -e $VM_DIR/release_mon ] ; then
    FILES_TO_DELETE="$FILES_TO_DELETE $VM_DIR/release_mon"
  fi

  if [ -e $VM_DIR/pid ] ; then
    FILES_TO_DELETE="$FILES_TO_DELETE $VM_DIR/pid"
  fi

  if [ -e $VM_DIR/mode ] ; then
    FILES_TO_DELETE="$FILES_TO_DELETE $VM_DIR/mode"
  fi

  RM_RES=$(rm -rf $FILES_TO_DELETE 2>&1)

  if [ $? -ne 0 ]; then
    echo "Error deleting related files: $RM_RES"
    exit 6
  fi

else

  echo "Warning: VM is not currently running"
  exit 9

fi

# Bring down firewall and ssh port forwarding
#sudo $SCRIPT_DIR/fw.sh $VM_DIR
#FW_RES=$?

#if [ $FW_RES -ne 0 ]; then
#  echo "Error: Failed to remove firewall policy after stopping VM; error code ($FW_RES)"
#  exit 7
#fi

sudo $SCRIPT_DIR/remove-vm-iptables.sh $VM_DIR

exit 0
