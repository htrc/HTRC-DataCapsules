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

  echo "Usage: $0 --wdir <Directory for VM>"
  echo ""
  echo "Determines the operational status of the VM in the given directory."
  echo ""
  echo "(--wdir)  Directory: The directory where this VM's data is held."
  echo ""
  echo "-h|--help Show help."

}

# Initialize all the option variables.
# This ensures we are not contaminated by variables from the environment.
VM_DIR=

while :; do
    case $1 in
        -h|-\?|--help)
            usage    # Display a usage synopsis.
            exit
            ;;
        --wdir)       # Takes an option argument; ensure it has been specified.
            if [ "$2" ]; then
                VM_DIR=$2
                shift
            else
                die 'ERROR: "--wdir" requires a non-empty option argument.'
            fi
            ;;
        --wdir=?*)
            VM_DIR=${1#*=} # Delete everything up to "=" and assign the remainder.
            ;;
        --wdir=)         # Handle the case of an empty --wdir=
            die 'ERROR: "--wdir" requires a non-empty option argument.'
            ;;
        --)              # End of all options.
            shift
            break
            ;;
        -?*)
            printf 'WARN: Unknown option (ignored): %s\n' "$1" >&2
            usage
            exit 1
            ;;
        *)               # Default case: No more options, so break out of the loop.
            break
    esac

    shift
done

if [ -z "$VM_DIR" ]; then
  printf 'WARN: Missing required argument working dir (--wdir)'  >&2
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

# Remove iptables rules related to the capsule 
  sudo $SCRIPT_DIR/remove-vm-iptables.sh $VM_DIR

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

    if /usr/sbin/pidof `basename $QEMU` | grep -q $VM_PID; then
      
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

exit 0
