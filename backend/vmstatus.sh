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

STAT="Status: "
STAT_RUNNING="$STAT Running"
STAT_NOT_RUNNING="$STAT Not_Running"

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


if [ -e $VM_DIR/pid ]; then

  VM_PID=`cat $VM_DIR/pid`
 
  # If no process is running with that pid, then it probably shut down naturally
  if /usr/sbin/pidof `basename $QEMU` | grep -q $VM_PID; then
    echo "$STAT_RUNNING"
  else
    echo "$STAT_NOT_RUNNING"
  fi

else

  # If pid file doesn't exist, then VM isn't running;
  echo "$STAT_NOT_RUNNING"

fi

exit 0
