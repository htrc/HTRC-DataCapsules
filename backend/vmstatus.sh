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

STAT="Status: "
STAT_RUNNING="$STAT Running"
STAT_NOT_RUNNING="$STAT Not_Running"

usage () {

  echo "Usage: $0 <Directory for VM>"
  echo ""
  echo "Determines the operational status of the VM in the given directory."
  echo ""
  echo "(--wdir)  Directory: The directory where this VM's data is held."

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
  echo "Error: Specified VM does not exist"
  exit 2
fi

if [ -e $VM_DIR/pid ]; then

  VM_PID=`cat $VM_DIR/pid`
 
  # If no process is running with that pid, then it probably shut down naturally
  if pidof qemu-system-x86_64 | grep -q $VM_PID; then
    echo "$STAT_RUNNING"
  else
    echo "$STAT_NOT_RUNNING"
  fi

else

  # If pid file doesn't exist, then VM isn't running;
  echo "$STAT_NOT_RUNNING"

fi

exit 0
