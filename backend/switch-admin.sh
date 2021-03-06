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

  echo "Usage: $0 --wdir <Directory for VM> --mode <Security Mode> --vmtype <Capsule Type>"
  echo ""
  echo "--wdir   Directory: The directory where this VM's data will be held"
  echo ""
  echo "--mode    Boot to Secure Mode: One of 's(ecure)' or 'm(aintenance)', denotes whether the"
  echo "          guest being started should be booted into maintenance or secure mode"
  echo ""
  echo "--vmtype  Capsule Type - DEMO or RESEARCH"
  echo ""
  echo "-h|--help Show help."

}

# Initialize all the option variables.
# This ensures we are not contaminated by variables from the environment.
VM_DIR=
RAW_MODE=
DC_TYPE=

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
        --mode)       # Takes an option argument; ensure it has been specified.
            if [ "$2" ]; then
                RAW_MODE=$2
                shift
            else
                die 'ERROR: "--mode" requires a non-empty option argument.'
            fi
            ;;
        --mode=?*)
            RAW_MODE=${1#*=} # Delete everything up to "=" and assign the remainder.
            ;;
        --mode=)         # Handle the case of an empty --mode=
            die 'ERROR: "--mode" requires a non-empty option argument.'
            ;;
        --vmtype)       # Takes an option argument; ensure it has been specified.
            if [ "$2" ]; then
                DC_TYPE=$2
                shift
            else
                die 'ERROR: "--vmtype" requires a non-empty option argument.'
            fi
            ;;
        --vmtype=?*)
            DC_TYPE=${1#*=} # Delete everything up to "=" and assign the remainder.
            ;;
        --vmtype=)         # Handle the case of an empty --vmtype=
            die 'ERROR: "--vmtype" requires a non-empty option argument.'
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

if [[ -z "$VM_DIR" || -z "$DC_TYPE" || -z "$RAW_MODE" ]]; then
  printf 'WARN: Missing required argument'  >&2
  usage
  exit 1
fi

# Ensure mode string has proper format
if [ -z $RAW_MODE -o ${RAW_MODE:0:1} != 's' -a ${RAW_MODE:0:1} != 'm' ]; then
   echo "Error: Invalid Mode!"
   usage
   exit 1
fi


if [ ! -d $VM_DIR ] ; then
  echo "Error: Invalid VM directory specified!"
  exit 2
fi

[ ${RAW_MODE:0:1} = 's' ]
      SECURE_MODE=$?

# Load config file
. $VM_DIR/config

# Check if VM is running
if [[ `$SCRIPT_DIR/vmstatus.sh --wdir $VM_DIR` =~ "Status:  Not_Running" ]]; then
  echo "Error: VM is not running!"
  exit 3
fi

# Check if VM is already in the mode we're switching to
if [[ `cat $VM_DIR/mode` = "Maintenance" && $SECURE_MODE -ne 0 || `cat $VM_DIR/mode` = "Secure" && $SECURE_MODE -eq 0 ]]; then
  echo "Error: VM is already in that mode"
  exit 3
fi

# If secure mode, sync storage, apply policy, take snapshot, mount secure volume, update modefile
if [ $SECURE_MODE = 0 ]; then

   #Add or remove releaseresult command from the capsule
   $SCRIPT_DIR/manageguest.sh --wdir $VM_DIR --vmtype $DC_TYPE

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

  # Take Capsules Snapshot
  echo "savevm capsules" | nc -U $VM_DIR/monitor >/dev/null

  # Mount Secure Volume
  echo "drive_add 0 if=none,id=secure_volume,file=$VM_DIR/$SECURE_VOL" | nc -U $VM_DIR/monitor >/dev/null
  echo "device_add virtio-blk-pci,id=secure_volume,drive=secure_volume" | nc -U $VM_DIR/monitor >/dev/null

  # Mount Spool Volume
  echo "drive_add 1 id=release_spool,if=none,file=$VM_DIR/spool_volume" | nc -U $VM_DIR/monitor >/dev/null
  echo "device_add virtio-blk-pci,id=release_spool,drive=release_spool" | nc -U $VM_DIR/monitor >/dev/null


  # Automount volumes and fix permissions
  sleep 5
  echo "Automounting disks and fixing permissions"
  #python $SCRIPT_DIR/tools/negotiator-cli/negotiator-cli.py -e fix-securevol-permissions $VM_DIR/negotiator-host-to-guest.sock
  ssh -o StrictHostKeyChecking=no -i $ROOT_PRIVATE_KEY  root@$VM_IP_ADDR "/usr/lib/negotiator/commands/fix-securevol-permissions"


  # Apply Firewall Policy
  sudo $SCRIPT_DIR/fw.sh $VM_DIR $ADMIN_SECURE_POLICY
  FW_RES=$?

  if [ $FW_RES -ne 0 ]; then
    echo "Error: Failed to apply firewall policy; error code ($FW_RES)"
    exit 6
  fi

  # Update Mode File
  echo "Secure" > $VM_DIR/mode
  logger "$VM_DIR Switched to the secure mode"

# If maintenance, unmount secure volume, revert snapshot, remove policy, update modefile
else

  # Unmount Secure Volume and Spool Volume
  echo "device_del secure_volume" | nc -U $VM_DIR/monitor >/dev/null
  echo "drive_del secure_volume" | nc -U $VM_DIR/monitor >/dev/null
  echo "device_del release_spool" | nc -U $VM_DIR/monitor >/dev/null
  echo "drive_del release_spool" | nc -U $VM_DIR/monitor >/dev/null

  # Revert Capsules Snapshot
  echo "loadvm capsules" | nc -U $VM_DIR/monitor >/dev/null

  # Replace Firewall Policy
  sudo $SCRIPT_DIR/fw.sh $VM_DIR $MAINTENANCE_POLICY
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

  # copy authorized_keys file from VM_DIR to VM
  scp -o StrictHostKeyChecking=no  -i $ROOT_PRIVATE_KEY $VM_DIR/authorized_keys root@$VM_IP_ADDR:$DC_USER_KEY_FILE >> $VM_DIR/copy_authorized_keys_out 2>&1

  logger "$VM_DIR Switched to the Maintenance mode"

fi

logger "$VM_DIR switch vm success."
echo "Success"
exit 0
