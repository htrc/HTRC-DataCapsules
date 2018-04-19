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

  echo "Usage: $0 <Directory for VM> --pubkey <SSH_KEY_TO_ADD>"
  echo ""
  echo "(--wdir)  Directory: The directory where this VM's data will be held"
  echo ""
  echo "--pubkey  User's ssh public key to add in the VM"

}

REQUIRED_OPTS="VM_DIR SSH_KEY"
ALL_OPTS="$REQUIRED_OPTS"
UNDEFINED=12345capsulesxXxXxundefined54321

for var in $ALL_OPTS; do
  eval $var=$UNDEFINED
done


if [[ $1 && $1 != -* ]]; then
  VM_DIR=$1
  shift
fi


declare -A longoptspec
longoptspec=( [wdir]=1 [pubkey]=1)
optspec=":h-:d:k:"
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
        k|pubkey)
          SSH_KEY=$4
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

# Make sure capsule is running and in maintenance mode
if [ ! -e $VM_DIR/pid ]; then
    echo "Error: Capsule is not running. "
    logger "Cannot add ssh key. Capsule is not running."
    exit 3
fi

if [ `cat $VM_DIR/mode` =  "Secure" ]; then
    echo "Error: Capsule is not in the Maintenance mode. "
    logger "Cannot add ssh key. Capsule is not in the Maintenance mode. "
    exit 3
fi

DC_USER_HOME=/home/dcuser
DEMO_USER_HOME=/home/demouser
DC_USER_KEY_FILE=$DC_USER_HOME/.ssh/authorized_keys
DEMO_USER_KEY_FILE=$DEMO_USER_HOME/.ssh/authorized_keys

logger "$VM_DIR - Adding SSH public key.."

#Copy user's ssh key to dcuser home folder in the capsule
if [ $NO_PASSWORD ]; then
      ssh -o StrictHostKeyChecking=no  -i $ROOT_PRIVATE_KEY root@$VM_IP_ADDR " echo $GMC_PUB_KEY > $DC_USER_KEY_FILE ; echo $SSH_KEY >> $DC_USER_KEY_FILE"
else
   sshpass -p 'dcuser' ssh -o StrictHostKeyChecking=no  dcuser@$VM_IP_ADDR " mkdir -p $DC_USER_HOME/.ssh; echo $GMC_PUB_KEY > $DEMO_USER_KEY_FILE ; echo $SSH_KEY >> $DEMO_USER_KEY_FILE; echo "dcuser" | sudo -S loginctl unlock-sessions "
fi

exit 0