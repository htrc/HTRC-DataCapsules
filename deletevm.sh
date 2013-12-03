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

usage () {

  echo "Usage: $0 <Directory for VM>"
  echo ""
  echo "Permanently deletes a VM from the machine by deleting the directory"
  echo " that contains all of its data."
  echo ""
  echo "(--wdir)  Directory: The directory, to be deleted, where this VM's data is held."

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

# TODO: Must check if VM is running before we delete it
#if [ `./vmstatus.sh $VM_DIR` = "RUNNING" ]; then
#  STOP_RES=$(./stopvm.sh $VM_DIR)
#
#  if [ $? -ne 0 ]; then
#    echo "Error: VM is currently running and could not be stopped (reason follows):"
#    echo "$STOP_RES"
#fi

if [ -e $VM_DIR ]; then

  RM_RES=$(rm -rf $VM_DIR 2>&1)

  if [ $? -ne 0 ]; then
    echo "Error: Could not delete VM directory: $RM_RES"
    exit 2
  else
    exit 0
  fi

else

  echo "Error: Directory for VM, '$VM_DIR' does not exist on this machine."
  exit 3

fi


