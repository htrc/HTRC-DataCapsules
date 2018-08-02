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
  echo "Launches daemon that listens for result release data"
  echo ""
  echo "--wdir  Directory: The directory where this VM's data will be held"
  echo ""
  echo "--db      Database: The database to upload the results to"
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
        --db)       # Takes an option argument; ensure it has been specified.
            if [ "$2" ]; then
                $DB_URL=$2
                shift
            fi
            ;;
        --db=?*)
            $DB_URL=${1#*=} # Delete everything up to "=" and assign the remainder.
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

if [ -z "$DB_URL" ]; then
  printf 'WARN: Missing required value DB url'  >&2
  usage
  exit 1
fi

if [ ! -d $VM_DIR ] ; then
  echo "Error: Invalid VM directory specified!"
  exit 2
fi


# Load config file
. $VM_DIR/config

mkdir $VM_DIR/release

while [[ `$SCRIPT_DIR/vmstatus.sh --wdir $VM_DIR` =~ "Status:  Running" ]]; do

  timeout 60 /bin/bash -c "tail -F -n0 $VM_DIR/release_mon | head -n0"

  # timeout returns 124 if it times out
  if [[ $? -ne 0 || `cat $VM_DIR/mode` =~ "Maintenance" ]]; then
    continue
  fi

  RES_FILENAME=$(tail -n1 $VM_DIR/release_mon | sed -e 's/^[ \r\n]*//' -e 's/[ \r\n]*$//')

  echo "$RES_FILENAME" | grep -q "^[0-9A-Za-z_-]*\.zip$"

  if [[ $? -ne 0 || $(echo "$RES_FILENAME" | wc -c) -gt 256 ]]; then
    continue
  fi

  sudo mount -o loop,rw $VM_DIR/spool_volume $VM_DIR/release/

  # Connect to sql server and upload file
  if [[ -n "$KEY" || -n "$CERT" ]]; then
    curl -F "file=@$VM_DIR/release/$RES_FILENAME" -F "vmid=$(basename $VM_DIR)" --key $DC_API_CLIENT_KEY --cert $DC_API_CLIENT_CERT  $DB_URL
    UPLOAD_RES=$?
  else
    curl -F "file=@$VM_DIR/release/$RES_FILENAME" -F "vmid=$(basename $VM_DIR)" $DB_URL
    UPLOAD_RES=$?
  fi

  if [[ $UPLOAD_RES -ne 0 ]]; then
    echo "Failed to release file '$RES_FILENAME' (error code $UPLOAD_RES)" >> $VM_DIR/last_run
  else
    touch $VM_DIR/release/done
  fi

  sudo umount $VM_DIR/release/

done

rm -rf $VM_DIR/release_pid
rm -rf $VM_DIR/release

exit 0
