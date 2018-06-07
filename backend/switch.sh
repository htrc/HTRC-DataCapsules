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

. utils.sh

SCRIPT_DIR=$(cd $(dirname $0); pwd)
. $SCRIPT_DIR/capsules.cfg

usage () {

  echo "Usage: $0 <Directory for VM> --mode <Security Mode> --policy <Policy File> --pubkey <User's ssh key>"
  echo ""
  echo "(--wdir)  Directory: The directory where this VM's data will be held"
  echo ""
  echo "--mode    Boot to Secure Mode: One of 's(ecure)' or 'm(aintenance)', denotes whether the"
  echo "          guest being started should be booted into maintenance or secure mode"
  echo ""
  echo "--policy  Policy File: The file that contains the policy for restricting this VM."
  echo "--pubkey  User's ssh public key."

}

REQUIRED_OPTS="VM_DIR SECURE_MODE POLICY"
ALL_OPTS="$REQUIRED_OPTS"
UNDEFINED=12345capsulesxXxXxundefined54321

UBUNTU_12_04_IMAGE=uncamp2015-demo.img

for var in $ALL_OPTS; do
  eval $var=$UNDEFINED
done

if [[ $1 && $1 != -* ]]; then
  VM_DIR=$1
  shift
fi

declare -A longoptspec
longoptspec=( [wdir]=1 [mode]=1 [policy]=1 [pubkey]=1 )
optspec=":h-:d:m:p:k:"
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
    k|pubkey)
      SSH_KEY=${@: -1}
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

  # Wait for secure volume to finish being created (in case it hasn't yet by createvm)
  for time in $(seq 1 30); do
    if [ -e $VM_DIR/$SECURE_VOL ]; then
      break
    fi
    sleep 1
  done

  if [ ! -e $VM_DIR/$SECURE_VOL ]; then
    echo "Error: CreateVM failed to create secure volume; unable to enter secure mode!"
    exit 5
  fi

  # Sync storage
  echo "commit all" | nc -U $VM_DIR/monitor >/dev/null

  # Apply Firewall Policy
  sudo $SCRIPT_DIR/fw.sh $VM_DIR $POLICY
  FW_RES=$?

  if [ $FW_RES -ne 0 ]; then
    echo "Error: Failed to apply firewall policy; error code ($FW_RES)"
    exit 6
  fi

  # Take Capsules Snapshot
  echo "savevm capsules" | nc -U $VM_DIR/monitor >/dev/null

  # Mount Secure Volume
  echo "drive_add 0 if=none,id=secure_volume,file=$VM_DIR/$SECURE_VOL" | nc -U $VM_DIR/monitor >/dev/null
  if beginswith $UBUNTU_12_04_IMAGE $IMAGE || [ -z ${NEGOTIATOR_ENABLED+x} ] || [ $NEGOTIATOR_ENABLED -eq 0 ]; then
      echo "device_add usb-storage,id=secure_volume,drive=secure_volume" | nc -U $VM_DIR/monitor >/dev/null
  else
      echo "device_add virtio-blk-pci,id=secure_volume,drive=secure_volume" | nc -U $VM_DIR/monitor >/dev/null
  fi
  #

  # Mount Spool Volume
  echo "drive_add 1 id=spool,if=none,file=$VM_DIR/spool_volume" | nc -U $VM_DIR/monitor >/dev/null
  if beginswith $UBUNTU_12_04_IMAGE $IMAGE || [ -z ${NEGOTIATOR_ENABLED+x} ] || [ $NEGOTIATOR_ENABLED -eq 0 ]; then
      echo "device_add usb-storage,id=spool,drive=spool" | nc -U $VM_DIR/monitor >/dev/null
  else
      echo "device_add virtio-blk-pci,id=spool,drive=spool" | nc -U $VM_DIR/monitor >/dev/null    
  fi

  if ! beginswith $UBUNTU_12_04_IMAGE $IMAGE && [ -n "$NEGOTIATOR_ENABLED" ]  && [ $NEGOTIATOR_ENABLED -eq 1 ]; then
      # Automount volumes and fix permissions
      sleep 5
      echo "Automounting disks and fixing permissions"
      python $SCRIPT_DIR/tools/negotiator-cli/negotiator-cli.py -e fix-securevol-permissions $VM_DIR/negotiator-host-to-guest.sock
  fi


  # Start release daemon if not already running
  if [ ! -e $VM_DIR/release_pid ]; then
    nohup $SCRIPT_DIR/released.sh $VM_DIR 2>>$VM_DIR/release_log >>$VM_DIR/release_log &
    echo "$!" > $VM_DIR/release_pid
  fi

  # Update Mode File
  echo "Secure" > $VM_DIR/mode

# If maintenance, unmount secure volume, revert snapshot, remove policy, update modefile
else

  # Unmount Secure Volume and Spool Volume
  echo "device_del secure_volume" | nc -U $VM_DIR/monitor >/dev/null
  echo "drive_del secure_volume" | nc -U $VM_DIR/monitor >/dev/null
  echo "device_del spool" | nc -U $VM_DIR/monitor >/dev/null
  echo "drive_del spool" | nc -U $VM_DIR/monitor >/dev/null

  # Revert Capsules Snapshot
  echo "loadvm capsules" | nc -U $VM_DIR/monitor >/dev/null

  # Replace Firewall Policy
  sudo $SCRIPT_DIR/fw.sh $VM_DIR $POLICY
  FW_RES=$?

  if [ $FW_RES -ne 0 ]; then
    echo "Error: Failed to replace firewall policy; error code ($FW_RES)"
    exit 7
  fi

  # The devices have been removed already,
  # this just resets things for future secure transitions
  echo "usb_del 0.0" | nc -U $VM_DIR/monitor >/dev/null

  # Update Mode File
  echo "Maintenance" > $VM_DIR/mode

  # Add user's ssh key and guacamole client's ssh key
  if [ "$SSH_KEY" ]; then
     $SCRIPT_DIR/updatekey.sh --wdir $VM_DIR --pubkey "$SSH_KEY"
  fi

fi

exit 0
