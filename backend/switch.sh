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

  echo "Usage: $0 --wdir <Directory for VM> --mode <Security Mode> --policy <Policy File> --pubkey <User's ssh key>"
  echo ""
  echo "--wdir   Directory: The directory where this VM's data will be held"
  echo ""
  echo "--mode    Boot to Secure Mode: One of 's(ecure)' or 'm(aintenance)', denotes whether the"
  echo "          guest being started should be booted into maintenance or secure mode"
  echo ""
  echo "--policy  Policy File: The file that contains the policy for restricting this VM."
  echo "--pubkey  User's ssh public key."
  echo ""
  echo "-h|--help Show help."

}

# Initialize all the option variables.
# This ensures we are not contaminated by variables from the environment.
VM_DIR=
RAW_MODE=
POLICY=
SSH_KEY=

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
        --policy)       # Takes an option argument; ensure it has been specified.
            if [ "$2" ]; then
                POLICY=$2
                shift
            else
                die 'ERROR: "--policy" requires a non-empty option argument.'
            fi
            ;;
        --policy=?*)
            POLICY=${1#*=} # Delete everything up to "=" and assign the remainder.
            ;;
        --policy=)         # Handle the case of an empty --policy=
            die 'ERROR: "--policy" requires a non-empty option argument.'
            ;;
        --pubkey)       # Takes an option argument; ensure it has been specified.
            if [ "$2" ]; then
                SSH_KEY=$2
                shift
            fi
            ;;
        --pubkey=?*)
            SSH_KEY=${1#*=} # Delete everything up to "=" and assign the remainder.
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

if [ -z "$RAW_MODE" ]; then
  printf 'WARN: Missing required argument mode (--mode)'  >&2
  usage
  exit 1
fi

# Ensure mode string has proper format
if [ -z $RAW_MODE -o ${RAW_MODE:0:1} != 's' -a ${RAW_MODE:0:1} != 'm' ]; then
   echo "Error: Invalid Mode!"
   usage
   exit 1
fi

if [ -z "$POLICY" ]; then
  printf 'WARN: Missing required argument policy (--policy)'  >&2
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

  # Copy .htrc conf file for HTRC WorksetToolKit. If user has updated htrc package in the maintenance mode, existing .htrc file will be replaced by the default file. We need to copy the correct configuration file with actual configuration file.
  scp -o StrictHostKeyChecking=no  -i $ROOT_PRIVATE_KEY $HTRC_CONFIG root@$VM_IP_ADDR:/home/dcuser/.htrc

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
  sudo $SCRIPT_DIR/fw.sh $VM_DIR $POLICY
  FW_RES=$?

  if [ $FW_RES -ne 0 ]; then
    echo "Error: Failed to apply firewall policy; error code ($FW_RES)"
    exit 6
  fi


  # Start release daemon if not already running
  if [ ! -e $VM_DIR/release_pid ]; then
    nohup $SCRIPT_DIR/released.sh $VM_DIR 2>>$VM_DIR/release_log &
    echo "$!" > $VM_DIR/release_pid
  fi

  # Update Mode File
  echo "Secure" > $VM_DIR/mode

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

  # Add user's ssh key
  if [ "$SSH_KEY" ]; then
     $SCRIPT_DIR/updateuserkey.sh --wdir $VM_DIR --pubkey "$SSH_KEY"
  fi

fi

exit 0
