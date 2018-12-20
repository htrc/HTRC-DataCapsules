#!/bin/bash

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
. $CAPSULE_CONFIG_FILE

usage () {

  echo "Usage: $0 --wdir <Directory for VM>"
  echo ""
  echo "Determines the operational status of the VM in the given directory."
  echo ""
  echo "(--wdir)  Directory: The directory where this VM's data is held."
  echo ""
  echo "--vnc      VNC Port: The port that will be used to serve the VNC service"
  echo ""
  echo "--ssh      SSH Port: The port that will be used to serve the SSH service"
  echo ""
  echo "--deshost  Destination host: The host where the VM is migrated to"
  echo ""
  echo "-h|--help Show help."

}

# Initialize all the option variables.
# This ensures we are not contaminated by variables from the environment.
VM_DIR=
NEW_VNC_PORT=
NEW_SSH_PORT=
DES_HOST=

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
        --vnc)       # Takes an option argument; ensure it has been specified.
            if [ "$2" ]; then
                NEW_VNC_PORT=$2
                shift
            else
                die 'ERROR: "--vnc" requires a non-empty option argument.'
            fi
            ;;
        --vnc=?*)
            NEW_VNC_PORT=${1#*=} # Delete everything up to "=" and assign the remainder.
            ;;
        --vnc=)         # Handle the case of an empty --vnc=
            die 'ERROR: "--vnc" requires a non-empty option argument.'
            ;;
        --ssh)       # Takes an option argument; ensure it has been specified.
            if [ "$2" ]; then
                NEW_SSH_PORT=$2
                shift
            else
                die 'ERROR: "--ssh" requires a non-empty option argument.'
            fi
            ;;
        --ssh=?*)
            NEW_SSH_PORT=${1#*=} # Delete everything up to "=" and assign the remainder.
            ;;
        --ssh=)         # Handle the case of an empty --ssh=
            die 'ERROR: "--ssh" requires a non-empty option argument.'
            ;;
        --deshost)       # Takes an option argument; ensure it has been specified.
            if [ "$2" ]; then
                DES_HOST=$2
                shift
            else
                die 'ERROR: "--deshost" requires a non-empty option argument.'
            fi
            ;;
        --deshost=?*)
            DES_HOST=${1#*=} # Delete everything up to "=" and assign the remainder.
            ;;
        --deshost=)         # Handle the case of an empty --wdir=
            die 'ERROR: "--deshost" requires a non-empty option argument.'
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

if [[ -z "$VM_DIR" || -z "NEW_VNC_PORT" || -z "NEW_SSH_PORT" || -z "DES_HOST" ]]; then
  printf 'WARN: Missing required argument'  >&2
  usage
  exit 1
fi

if ! [[ "$NEW_VNC_PORT" =~ ^[0-9]+$ && "$NEW_VNC_PORT" -ge 5900 && "$NEW_VNC_PORT" -le 65535 ]]; then
     echo "error: provided vnc port ($NEW_VNC_PORT) is invalid;"
     echo "note: the port value must be at least 5900"
     exit 1
fi

if ! [[ "$NEW_SSH_PORT" =~ ^[0-9]+$ && "$NEW_SSH_PORT" -le 65535 ]]; then
     echo "error: provided ssh port ($NEW_SSH_PORT) is invalid;"
     exit 1
fi


if [ ! -d $VM_DIR ] ; then
  echo "Error: Invalid VM directory specified!"
  exit 2
fi

# Check if VM is in SHUTDOWN state
if [[ `$SCRIPT_DIR/vmstatus.sh --wdir $VM_DIR` =~ "Status:  Running" ]]; then
  echo "Error: VM is running!"
  exit 3
fi

# Backup config file
cp $VM_DIR/config $VM_DIR/config_back

# Load config file
. $VM_DIR/config

# Edit config file
#update VNC and SSH ports if they are different
if [[ $VNC_PORT != $NEW_VNC_PORT ]]; then
    sed -i -e 's/'$VNC_PORT'/'$NEW_VNC_PORT'/g' $VM_DIR/config
fi

if [[ $SSH_PORT != $NEW_SSH_PORT ]]; then
    sed -i -e 's/'$SSH_PORT'/'$NEW_SSH_PORT'/g' $VM_DIR/config
fi

#update/add source host
if [[ -z "$SRC_HOST" ]]; then
    echo "SRC_HOST=$HOSTNAME" >> $VM_DIR/config
else
    sed -i -e 's/'$SRS_HOST'/'$(hostname)'/g' $VM_DIR/config
fi


#replace VM_IP_ADDR and VM_MAC_ADDR
#Get the next available Ip from Destination host's FREE_HOSTS file
NEW_VM_IP_ADDR=$(ssh $DES_HOST ". $CAPSULE_CONFIG_FILE; /bin/head -n1 $FREE_HOSTS; sed -i '1d' $FREE_HOSTS;")


if [[ -z $NEW_VM_IP_ADDR ]]; then
  echo "Error: Unable to allocate IP address; IP pool is exhausted"
  exit 5
fi

if [[ $VM_IP_ADDR != $NEW_VM_IP_ADDR ]]; then
    sed -i -e 's/'$VM_IP_ADDR'/'$NEW_VM_IP_ADDR'/g' $VM_DIR/config
    NEW_VM_MAC_ADDR=$(awk -F, '/'"${NEW_VM_IP_ADDR}"'$/{print $1}' $SCRIPT_DIR/dhcp_hosts)
    sed -i -e 's/'$VM_MAC_ADDR'/'$NEW_VM_MAC_ADDR'/g' $VM_DIR/config
fi

#copy VM directory to destination host
scp -r $VM_DIR $DES_HOST:$VM_DIR

logger "$VM_DIR is successfully migrated to $DES_HOST"

#Delete VM from current host
$SCRIPT_DIR/deletevm.sh --wdir $VM_DIR

exit 0