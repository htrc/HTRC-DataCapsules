#!/usr/bin/env bash

# Copyright 2017 The Trustees of Indiana University
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

  echo "Usage: $0 --wdir <Directory for VM> --pubkey <SSH_KEY_TO_ADD>"
  echo ""
  echo "Add user's public ssh key to data capsule"
  echo ""
  echo "--wdir  Directory: The directory where this VM's data will be held"
  echo ""
  echo "--guid  User's ssh public key to add in the VM"
  echo ""
  echo "--pubkey  User's ssh public key to add in the VM"
  echo ""
  echo "-h|--help Show help."

}

# Initialize all the option variables.
# This ensures we are not contaminated by variables from the environment.
VM_DIR=
SSH_KEY=
GUID=

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
        --guid)       # Takes an option argument; ensure it has been specified.
            if [ "$2" ]; then
                GUID=$2
                shift
            else
                die 'ERROR: "--guid" requires a non-empty option argument.'
            fi
            ;;
        --guid=?*)
            GUID=${1#*=} # Delete everything up to "=" and assign the remainder.
            ;;
        --guid=)         # Handle the case of an empty --guid=
            die 'ERROR: "--guid" requires a non-empty option argument.'
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

if [[ -z "$VM_DIR" || -z "$GUID" ]]; then
  printf 'WARN: Missing required argument'  >&2
  usage
  exit 1
fi

if [[ ! -d "$VM_DIR" ]] ; then
  echo "Error: Invalid VM directory specified!"
  exit 2
fi

# Load config file
. $VM_DIR/config

copy_authorized_keys () {
  # Check if VM is running
  if [[ `$SCRIPT_DIR/vmstatus.sh --wdir $VM_DIR` =~ "Status:  Running" ]]; then
     # Check if VM is in Maintenance mode
     if [ `cat $VM_DIR/mode` =  "Maintenance" ]; then
        logger "$VM_DIR - copying authorized_keys file.."
        scp -o StrictHostKeyChecking=no  -i $ROOT_PRIVATE_KEY $VM_DIR/authorized_keys root@$VM_IP_ADDR:$DC_USER_KEY_FILE >> $VM_DIR/copy_authorized_keys_out 2>&1
     fi
  fi
}

if [[ -f "$VM_DIR/pub_keys/$GUID" ]] ; then
    rm $VM_DIR/pub_keys/$GUID
fi

rm $VM_DIR/authorized_keys

if [[ -d "$VM_DIR/pub_keys" ]] ; then
  for filename in $VM_DIR/pub_keys/*; do
    cat $filename >> $VM_DIR/authorized_keys
  done
fi

$SCRIPT_DIR/updategmckey.sh --wdir $VM_DIR

copy_authorized_keys

exit 0