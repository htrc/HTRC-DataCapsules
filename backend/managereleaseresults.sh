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

  echo "Usage: $0 --wdir <Directory for VM> "
  echo ""
  echo "Add user's public ssh key to data capsule"
  echo ""
  echo "--wdir  Directory: The directory where this VM's data will be held"
  echo ""
  echo "-h|--help Show help."

}

# Initialize all the option variables.
# This ensures we are not contaminated by variables from the environment.
VM_DIR=
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

if [[ -z "$VM_DIR" || -z "$DC_TYPE" ]]; then
  printf 'WARN: Missing required argument'  >&2
  usage
  exit 1
fi

if [ ! -d $VM_DIR ] ; then
  echo "Error: Invalid VM directory specified!"
  exit 2
fi

# Load config file
. $VM_DIR/config

# Check if VM is running
if [[ `$SCRIPT_DIR/vmstatus.sh --wdir $VM_DIR` =~ "Status:  Not_Running" ]]; then
  echo "Error: VM is not running!"
  exit 3
fi

# Check if VM is in Maintenance mode
if [ `cat $VM_DIR/mode` =  "Secure" ]; then
    echo "Error: Capsule is not in the Maintenance mode. "
    logger "Cannot add or remove  releaseresult command. Capsule is not in the Maintenance mode. "
    exit 4
fi

# Remove release_results script if capsule type is DEMO and not disabled yet
if [[ "$DC_TYPE" = "$DEMO_TYPE" ]]; then
    if [[ ! -e $VM_DIR/release_results || `cat $VM_DIR/release_results` == "Enabled" ]]; then
        ssh -o StrictHostKeyChecking=no  -i $ROOT_PRIVATE_KEY root@$VM_IP_ADDR "chmod 744 /usr/local/bin/releaseresults && chown root:root /usr/local/bin/releaseresults"
        echo "Disabled" > $VM_DIR/release_results
    fi
# Add release_results script if capsule type is RESEARCH and disabled
elif [[ "$DC_TYPE" = "$RESEARCH_TYPE" || "$DC_TYPE" = "$RESEARCH_IC_TYPE" ]]; then
    if [[ `cat $VM_DIR/release_results` == "Disabled" ]]; then
      ssh -o StrictHostKeyChecking=no  -i $ROOT_PRIVATE_KEY root@$VM_IP_ADDR "chmod 755 /usr/local/bin/releaseresults && chown root:root /usr/local/bin/releaseresults"
      echo "Enabled" > $VM_DIR/release_results
    fi
else
     logger "Invalid DC Type - $DC_TYPE. VM Directory - $VM_DIR "
     echo "Invalid DC Type - $DC_TYPE. Please select $DEMO_TYPE or $RESEARCH_TYPE or $RESEARCH_IC_TYPE."
fi

exit 0