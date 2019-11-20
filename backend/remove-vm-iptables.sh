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

if [ $(whoami) != "root" ]; then
  echo "$0: Not running as root! (running as $(whoami))"
  exit 1
fi

export PATH=/usr/sbin:$PATH

VM_DIR=$1

if [ ! -d $VM_DIR ] ; then
  echo "Error: Invalid VM directory specified!"
  exit 2
fi

# Load config file
. $VM_DIR/config


# Getting VM IP Address from VM config variables
VM_IP=$VM_IP_ADDR
SSH_PORT=$SSH_PORT

if [ $# -eq 0 ]; then
    echo "Missing VM IP argument.\n\tremove-vm-iptables.sh <VM_IP>"
fi

(
flock -w 120 200

# Finding the tap device the VM is attached
VM_TAP_DEV=`iptables -n -L FORWARD --line-numbers | grep "${VM_IP}_" | head -n 1 | grep -oP 'tap\d+'`

echo "VM with IP $VM_IP uses tap device $VM_TAP_DEV"

IPTBLS_EXECUTABLE=`which iptables`
logger "$VM_DIR - iptables executable $IPTBLS_EXECUTABLE"

function deleteiptablesrules {
    logger "$3 - Deleting $1 rules.."
    GREP_OUT=`iptables -n -L $1 --line-numbers | grep $2`
    if [ $? -eq 1 ]; then
        logger "No $1 chain related rules for VM with IP: $3"
    else
	RULE_LINES=`echo "$GREP_OUT" | awk '{ print $1 }' | sort -nr |tr '\n' ' '`
	for i in $RULE_LINES; do
            iptables -D $1 $i
	done
    fi
}

deleteiptablesrules FORWARD ${VM_TAP_DEV} ${VM_IP} 
deleteiptablesrules INPUT ${VM_TAP_DEV} ${VM_IP}
deleteiptablesrules OUTPUT ${VM_TAP_DEV} ${VM_IP}

logger "$VM_IP - Deleting nat rules.."
# Deleting rules in nat table related to VM
GREP_OUT=`iptables -n -L -t nat --line-numbers | grep $SSH_PORT`
if [ $? -eq 1 ]; then
    logger "No nat table related to VM with IP: $VM_IP"
else
    RULE_LINES=`echo "$GREP_OUT" | awk '{ print $1 }' | sort -nr |tr '\n' ' '`
    for i in $RULE_LINES; do
	iptables -t nat -D PREROUTING $i
    done
fi

logger "$VM_DIR - Deleting chains.."
logger "Deleting iptables chains for VM with IP $VM_IP"
iptables -F "${VM_IP}_FW_F_FORWARD"
iptables -F "${VM_IP}_FW_F_INPUT"
iptables -F "${VM_IP}_FW_F_OUTPUT"
iptables -X "${VM_IP}_FW_F_FORWARD"
iptables -X "${VM_IP}_FW_F_INPUT"
iptables -X "${VM_IP}_FW_F_OUTPUT"
) 200>$SCRIPT_DIR/lock.iptables

exit $?
