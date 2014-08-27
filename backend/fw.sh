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

VM_MAC_ADDR=$1
POLICY_FILE=$2

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

(
flock -w 30 200

# Reset firewall chain
iptables -t filter -D INPUT -s ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_F_INPUT >/dev/null 2>&1
iptables -t filter -D INPUT -d ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_F_INPUT >/dev/null 2>&1
iptables -t filter -F ${VM_IP_ADDR}_FW_F_INPUT >/dev/null 2>&1
iptables -t filter -X ${VM_IP_ADDR}_FW_F_INPUT >/dev/null 2>&1

iptables -t filter -D FORWARD -s ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_F_FORWARD >/dev/null 2>&1
iptables -t filter -D FORWARD -d ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_F_FORWARD >/dev/null 2>&1
iptables -t filter -F ${VM_IP_ADDR}_FW_F_FORWARD >/dev/null 2>&1
iptables -t filter -X ${VM_IP_ADDR}_FW_F_FORWARD >/dev/null 2>&1

iptables -t filter -D OUTPUT -s ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_F_OUTPUT >/dev/null 2>&1
iptables -t filter -D OUTPUT -d ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_F_OUTPUT >/dev/null 2>&1
iptables -t filter -F ${VM_IP_ADDR}_FW_F_OUTPUT >/dev/null 2>&1
iptables -t filter -X ${VM_IP_ADDR}_FW_F_OUTPUT >/dev/null 2>&1

iptables -t nat -D PREROUTING -s ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_N_PRE >/dev/null 2>&1
iptables -t nat -D PREROUTING -d ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_N_PRE >/dev/null 2>&1
iptables -t nat -F ${VM_IP_ADDR}_FW_N_PRE >/dev/null 2>&1
iptables -t nat -X ${VM_IP_ADDR}_FW_N_PRE >/dev/null 2>&1

iptables -t nat -D INPUT -s ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_N_INPUT >/dev/null 2>&1
iptables -t nat -D INPUT -d ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_N_INPUT >/dev/null 2>&1
iptables -t nat -F ${VM_IP_ADDR}_FW_N_INPUT >/dev/null 2>&1
iptables -t nat -X ${VM_IP_ADDR}_FW_N_INPUT >/dev/null 2>&1

iptables -t nat -D OUTPUT -s ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_N_OUTPUT >/dev/null 2>&1
iptables -t nat -D OUTPUT -d ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_N_OUTPUT >/dev/null 2>&1
iptables -t nat -F ${VM_IP_ADDR}_FW_N_OUTPUT >/dev/null 2>&1
iptables -t nat -X ${VM_IP_ADDR}_FW_N_OUTPUT >/dev/null 2>&1

iptables -t nat -D POSTROUTING -s ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_N_POST >/dev/null 2>&1
iptables -t nat -D POSTROUTING -d ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_N_POST >/dev/null 2>&1
iptables -t nat -F ${VM_IP_ADDR}_FW_N_POST >/dev/null 2>&1
iptables -t nat -X ${VM_IP_ADDR}_FW_N_POST >/dev/null 2>&1

# Create new tables for rules
if [[ -n "$POLICY_FILE" ]]; then

  # Note that the filter "FORWARD" chain has a rule preventing communication
  # within the bridge interface;  that rule takes precedence, hence the '2';
  iptables -t filter -N ${VM_IP_ADDR}_FW_F_INPUT && \
  iptables -t filter -I INPUT 1 -s ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_F_INPUT && \
  iptables -t filter -I INPUT 1 -d ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_F_INPUT && \
  \
  iptables -t filter -N ${VM_IP_ADDR}_FW_F_FORWARD && \
  iptables -t filter -I FORWARD 2 -s ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_F_FORWARD && \
  iptables -t filter -I FORWARD 2 -d ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_F_FORWARD && \
  \
  iptables -t filter -N ${VM_IP_ADDR}_FW_F_OUTPUT && \
  iptables -t filter -I OUTPUT 1 -s ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_F_OUTPUT && \
  iptables -t filter -I OUTPUT 1 -d ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_F_OUTPUT && \
  \
  iptables -t nat -N ${VM_IP_ADDR}_FW_N_PRE && \
  iptables -t nat -I PREROUTING 1 -s ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_N_PRE && \
  iptables -t nat -I PREROUTING 1 -d ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_N_PRE && \
  \
  iptables -t nat -N ${VM_IP_ADDR}_FW_N_INPUT && \
  iptables -t nat -I INPUT 1 -s ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_N_INPUT && \
  iptables -t nat -I INPUT 1 -d ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_N_INPUT && \
  \
  iptables -t nat -N ${VM_IP_ADDR}_FW_N_OUTPUT && \
  iptables -t nat -I OUTPUT 1 -s ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_N_OUTPUT && \
  iptables -t nat -I OUTPUT 1 -d ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_N_OUTPUT && \
  \
  iptables -t nat -N ${VM_IP_ADDR}_FW_N_POST && \
  iptables -t nat -I POSTROUTING 1 -s ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_N_POST && \
  iptables -t nat -I POSTROUTING 1 -d ${VM_IP_ADDR} -j ${VM_IP_ADDR}_FW_N_POST

  if [ $? -ne 0 ]; then
    echo "Error: iptables rules failed to execute"
    exit 3
  fi

  # Apply firewall
  FW_RES=$(awk '
  BEGIN {
    def_prefix="_FW_"
  }
  /\*nat/ {
    prefix=def_prefix "N_"
  }
  /\*filter/ {
    prefix=def_prefix "F_"
  }
  /INPUT|OUTPUT|FORWARD|PREROUTING|POSTROUTING/ {
    sub(/PREROUTING/, "%IP%" prefix "PRE");
    sub(/POSTROUTING/, "%IP%" prefix "POST");
    sub(/INPUT|OUTPUT|FORWARD/, "%IP%" prefix "&");
  }
  {
    print $0
  }' $POLICY_FILE | sed "s/%IP%/$VM_IP_ADDR/g" | iptables-restore -n 2>&1)

  if [ $? -ne 0 ]; then
    echo "Error: Unable to apply firewall policy: $FW_RES"
    exit 4
  fi

fi

) 200>$SCRIPT_DIR/lock.iptables

exit 0
