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

  echo "Usage: $0 <Directory for VM>"
  echo ""
  echo "Launches daemon that listens for result release data"
  echo ""
  echo "(--wdir)  Directory: The directory where this VM's data will be held"
  echo ""
  echo "--db      Database: The database to upload the results to"

}

REQUIRED_OPTS="VM_DIR"

if [[ "$DB_URL" == "" ]]; then
  REQUIRED_OPTS="$REQUIRED_OPTS DB_URL"
fi

ALL_OPTS="$REQUIRED_OPTS DB_SERVER"
UNDEFINED=12345capsulesxXxXxundefined54321

for var in $ALL_OPTS; do
  eval $var=$UNDEFINED
done

if [[ $1 && $1 != -* ]]; then
  VM_DIR=$1
  shift
fi

declare -A longoptspec
longoptspec=( [wdir]=1 [db]=1 )
optspec=":h-:d:s:"
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
    s|db)
      DB_SERVER=$OPTARG
      ;;
    h|help)
      usage;
      exit 0
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

mkdir $VM_DIR/release

while [[ `$SCRIPT_DIR/vmstatus.sh $VM_DIR` =~ "Status:  Running" ]]; do

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
  curl -F "file=@$VM_DIR/release/$RES_FILENAME" -F "vmid=$(basename $VM_DIR)" $DB_URL
  UPLOAD_RES=$?

  if [[ $UPLOAD_RES -ne 0 ]]; then
    echo "Failed to release file '$RES_FILENAME' (error code $UPLOAD_RES)" >> $VM_DIR/last_run
  else
    touch $VM_DIR/release/done
  fi

  sudo umount $VM_DIR/release/

done

rm -rf $VM_DIR/release_pid $VM_DIR/release

exit 0
