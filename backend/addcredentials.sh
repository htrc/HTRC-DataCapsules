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

  echo "Usage: $0 --wdir <Directory for VM> --clientid <CAPSULE_AGENT_ID> --clientsecret <CAPSULE_AGENT_SECRET>"
  echo ""
  echo "Add user's public ssh key to data capsule"
  echo ""
  echo "--wdir  Directory: The directory where this VM's data will be held"
  echo ""
  echo "--clientid  User's ssh public key to add in the VM"
  echo ""
  echo "--clientsecret  User's ssh public key to add in the VM"
  echo ""
  echo "-h|--help Show help."

}

# Initialize all the option variables.
# This ensures we are not contaminated by variables from the environment.
VM_DIR=
CLIENT_SECRET=
CLIENT_ID=

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
        --clientid)       # Takes an option argument; ensure it has been specified.
            if [ "$2" ]; then
                CLIENT_ID=$2
                shift
            else
                die 'ERROR: "--clientid" requires a non-empty option argument.'
            fi
            ;;
        --clientid=?*)
            CLIENT_ID=${1#*=} # Delete everything up to "=" and assign the remainder.
            ;;
        --clientid=)         # Handle the case of an empty --clientid=
            die 'ERROR: "--clientid" requires a non-empty option argument.'
            ;;
        --clientsecret)       # Takes an option argument; ensure it has been specified.
            if [ "$2" ]; then
                CLIENT_SECRET=$2
                shift
            else
                die 'ERROR: "--clientsecret" requires a non-empty option argument.'
            fi
            ;;
        --clientsecret=?*)
            CLIENT_SECRET=${1#*=} # Delete everything up to "=" and assign the remainder.
            ;;
        --clientsecret=)         # Handle the case of an empty --clientsecret=
            die 'ERROR: "--clientsecret" requires a non-empty option argument.'
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

if [[ -z "$VM_DIR" || -z "$CLIENT_ID" || -z "$CLIENT_SECRET" ]]; then
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

echo "CLIENT_ID=$CLIENT_ID" >> $VM_DIR/config
echo "CLIENT_SECRET=$CLIENT_SECRET" >> $VM_DIR/config

exit 0
