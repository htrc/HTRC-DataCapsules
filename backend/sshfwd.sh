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

ACTION=$1
VM_MAC_ADDR=$2
SSH_PORT=$3

VM_IP_ADDR=$(cat $SCRIPT_DIR/dhcp_hosts | awk -F, "/$VM_MAC_ADDR/ "'{print $2}')

if [[ $(id | sed 's/uid=\([0-9]*\).*/\1/') != 0 ]]; then
  echo "Error: Firewall script is not running as root.  You probably have not"
  echo "finished configuring the sudoers file to run this script without a password"

  exit 1
fi

if [[ -z "$VM_IP_ADDR" ]]; then
  echo "Error: Unable to resolve MAC address to IP address"

  exit 2
fi

if [[ "$ACTION" = "up" ]]; then

  iptables -t nat -A PREROUTING -p tcp --dport $SSH_PORT -j DNAT --to-destination $VM_IP_ADDR:22 && \
  iptables -t nat -A POSTROUTING -j MASQUERADE

  if [ $? -ne 0 ]; then
    echo "Error: iptables ssh rules failed to execute (error code $?)"
    exit 3
  fi

else

  iptables -t nat -D PREROUTING -p tcp --dport $SSH_PORT -j DNAT --to-destination $VM_IP_ADDR:22
  iptables -t nat -D POSTROUTING -j MASQUERADE

  if [ $? -ne 0 ]; then
    echo "Error: iptables ssh rule removal failed to execute (error code $?)"
    exit 4
  fi

fi
