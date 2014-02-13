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

  echo "Usage: $0 <Directory for VM> --mode <Security Mode> --policy [Policy File]"
  echo ""
  echo "(--wdir)  Directory: The directory where this VM's data will be held?"
  echo ""
  echo "--mode    Boot to Secure Mode: One of 's(ecure)' or 'm(aintenance)', denotes whether the"
  echo "          guest being started should be booted into maintenance or secure mode"
  echo ""
  echo "--policy  Policy File: The file that contains the policy for restricting this VM."
  echo "                       This policy is optional when booting into maintenance mode." 

}

REQUIRED_OPTS="VM_DIR SECURE_MODE"
ALL_OPTS="$REQUIRED_OPTS POLICY"
UNDEFINED=12345capsulesxXxXxundefined54321

for var in $ALL_OPTS; do
  eval $var=$UNDEFINED
done

if [[ $1 && $1 != -* ]]; then
  VM_DIR=$1
  shift
fi

declare -A longoptspec
longoptspec=( [wdir]=1 [mode]=1 [policy]=1 )
optspec=":h-:d:m:p:"
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
    m|mode)
      RAW_MODE=$OPTARG

      # Ensure mode string has proper format
      if [ -z $RAW_MODE -o ${RAW_MODE:0:1} != 's' -a ${RAW_MODE:0:1} != 'm' ]; then
        usage
        exit 1
      fi
      
      [ ${RAW_MODE:0:1} = 's' ]
      SECURE_MODE=$?

      if [ $SECURE_MODE = 0 ]; then
        REQUIRED_OPTS="$REQUIRED_OPTS POLICY"
      fi
      ;;
    p|policy)
      POLICY=$OPTARG
      ;;
    h|help)
      usage;
      exit 1
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

# Check if VM is already in the mode we're switching to
if [[ `cat $VM_DIR/mode` = "Maintenance" && $SECURE_MODE -ne 0 || `cat $VM_DIR/mode` = "Secure" && $SECURE_MODE -eq 0 ]]; then
  echo "Error: VM is already in that mode"
  exit 3
fi

# If secure mode, sync storage, apply policy, take snapshot, mount secure volume, update modefile
if [ $SECURE_MODE = 0 ]; then

  # Sync storage
  echo "commit all" | nc -U $VM_DIR/monitor >/dev/null

  # Apply Firewall Policy
  sudo $SCRIPT_DIR/fw.sh $VM_MAC_ADDR $POLICY
  FW_RES=$?

  if [ $FW_RES -ne 0 ]; then
    echo "Error: Failed to apply firewall policy; error code ($FW_RES)"
    exit 4
  fi

  # Take Capsules Snapshot
  echo "savevm capsules" | nc -U $VM_DIR/monitor >/dev/null

  # Mount Secure Volume
  echo "drive_add 0 id=secure_volume,if=none,file=$VM_DIR/$SECURE_VOL" | nc -U $VM_DIR/monitor >/dev/null
  echo "device_add usb-storage,id=secure_volume,drive=secure_volume" | nc -U $VM_DIR/monitor >/dev/null

  # Update Mode File
  echo "Secure" > $VM_DIR/mode


# If maintenance, unmount secure volume, revert snapshot, remove policy, update modefile
else

  # Unmount Secure Volume
  echo "device_del secure_volume" | nc -U $VM_DIR/monitor >/dev/null

  # Revert Capsules Snapshot
  echo "loadvm capsules" | nc -U $VM_DIR/monitor >/dev/null

  # Remove Firewall Policy
  if [[ $POLICY = $UNDEFINED ]]; then
    sudo $SCRIPT_DIR/fw.sh $VM_MAC_ADDR
  else
    sudo $SCRIPT_DIR/fw.sh $VM_MAC_ADDR $POLICY
  fi

  FW_RES=$?

  if [ $FW_RES -ne 0 ]; then
    echo "Error: Failed to apply/remove firewall policy; error code ($FW_RES)"
    exit 5
  fi

  # Update Mode File
  echo "Maintenance" > $VM_DIR/mode

fi

exit 0
