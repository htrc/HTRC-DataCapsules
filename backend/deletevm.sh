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

usage () {

  echo "Usage: $0 <Directory for VM>"
  echo ""
  echo "Permanently deletes a VM from the machine by deleting the directory"
  echo " that contains all of its data."
  echo ""
  echo "(--wdir)  Directory: The directory, to be deleted, where this VM's data is held."

}

REQUIRED_OPTS="VM_DIR"
ALL_OPTS="$REQUIRED_OPTS FORCE_STOP"
UNDEFINED=12345capsulesxXxXxundefined54321

for var in $ALL_OPTS; do
  eval $var=$UNDEFINED
done

if [[ $1 && $1 != -* ]]; then
  VM_DIR=$1
  shift
fi

declare -A longoptspec
longoptspec=( [wdir]=1 )
optspec=":h-:d:f"
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
    f)
      FORCE_STOP=1
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

if [ -d $VM_DIR ]; then

  # Load config file
  . $VM_DIR/config

  # Must check if VM is running before we delete it
  if [[ `$SCRIPT_DIR/vmstatus.sh $VM_DIR` =~ "Status:  Running" ]]; then
  
    if [ $FORCE_STOP = 1 ]; then

      STOP_RES=$($SCRIPT_DIR/stopvm.sh $VM_DIR)

      if [ $? -ne 0 ]; then
        echo "Error: VM is currently running and could not be stopped (reason follows):"
        echo "$STOP_RES"
        exit 2
      fi

    else
      echo "VM is currently running.  To force deletion, add -f to delete command"
      exit 2
    fi

  fi

  RM_RES=$(rm -rf $VM_DIR 2>&1)

  if [ $? -ne 0 ]; then
    echo "Error: Could not delete VM directory: $RM_RES"
    exit 3
  else
    # Return IP address to free pool
    echo "$VM_IP_ADDR" >> $SCRIPT_DIR/free_hosts
    exit 0
  fi

else

  echo "Error: Directory for VM, '$VM_DIR' does not exist on this machine."
  exit 4

fi
