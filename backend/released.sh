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

DB_URL=http://thatchpalm.pti.indiana.edu:9001/sloan-ws-1.1-SNAPSHOT/upload

LISTEN_IP=192.168.53.1
LISTEN_PORT=6235

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

  timeout 60 nc -l $LISTEN_IP $LISTEN_PORT > $VM_DIR/release/release.data

  # timeout returns 124 if it times out
  if [[ $? -ne 0 ]]; then
    continue
  fi

  RES_FILENAME=$(head -n 1 $VM_DIR/release/release.data)

  echo "$RES_FILENAME" | grep -q "^[0-9A-Za-z_-]*\.zip$"
  #echo "$RES_FILENAME" | grep -q "^[0-9A-Za-z_-]*\.[0-9A-Za-z]*$"

  if [[ $? -ne 0 || $(echo "$RES_FILENAME" | wc -c) -gt 256 ]]; then
    rm -rf $VM_DIR/release/release.data
    continue
  fi

  sed '1d' $VM_DIR/release/release.data > $VM_DIR/release/$RES_FILENAME

  # Connect to sql server and upload file
  curl -F "file=@$VM_DIR/release/$RES_FILENAME" -F "vmid=$(basename $VM_DIR)" $DB_URL

  if [[ $? -eq 0 ]]; then
    echo "Complete" | nc $VM_IP_ADDR $LISTEN_PORT
  else
    echo "Failed" | nc $VM_IP_ADDR $LISTEN_PORT
  fi

done

exit 0
