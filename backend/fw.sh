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

export PATH="/usr/sbin:/usr/bin:/sbin:/bin"

if [ $(whoami) != "root" ]; then
  echo "$0: Not running as root! (running as $(whoami))"
  exit 1
fi

SCRIPT_DIR=$(cd $(dirname $0); pwd)
. $SCRIPT_DIR/capsules.cfg

VM_DIR=$1
POLICY_FILE=$2

if [ ! -d $VM_DIR ] ; then
  echo "Error: Invalid VM directory specified!"
  exit 2
fi

# Load config file
. $VM_DIR/config

if [[ $(id | sed 's/uid=\([0-9]*\).*/\1/') != 0 ]]; then
  echo "Error: Firewall script is not running as root.  You probably have not"
  echo "finished configuring the sudoers file to run this script without a password"

  exit 1
fi

if [[ -z "$VM_IP_ADDR" ]]; then
  echo "Error: Unable to find VM IP address in VM configuration file!"

  exit 2
fi

if [[ -z "$SSH_PORT" ]]; then
  echo "Error: Unable to find VM SSH port in VM configuration file!"

  exit 2
fi

(
flock -w 30 200

# Reset firewall chain
RESET_CHAINS=$(cat <<EOF
*nat

*filter
${VM_IP_ADDR}_FW_F_INPUT
${VM_IP_ADDR}_FW_F_FORWARD
${VM_IP_ADDR}_FW_F_OUTPUT

EOF
)

# Default for following loop
table="filter"

# Read lines from $RESET_CHAINS and process them using iptables
while read -r rule; do

  if [[ "$rule" =~ \*[A-Za-z]+ ]]; then
    table=${rule:1}
    continue
  fi

  if [[ "$rule" == "" ]]; then
    continue
  fi

  IPTABLES_CALL="iptables -t $table -F $rule"
  RESET_RES=$($IPTABLES_CALL 2>&1)
  RES=$?

  if echo "$RESET_RES" | grep -q "iptables: No chain/target/match by that name."; then

    IPTABLES_CALL="iptables -t $table -N $rule"
    RESET_RES=$($IPTABLES_CALL 2>&1)
    RES=$?

    if [[ $RES -ne 0 ]]; then
      echo "Error: iptables chain reset rules failed to execute (error code $RES); Error output below:"
      echo "'$IPTABLES_CALL'"
      echo "$RESET_RES"
      exit 3
    fi

    continue
  fi

  if [[ $RES -ne 0 ]]; then
    echo "Error: iptables chain reset rules failed to execute (error code $RES); Error output below:"
    echo "'$IPTABLES_CALL'"
    echo "$RESET_RES"
    exit 3
  fi

done <<< "$RESET_CHAINS"

# Create new tables for rules
if [[ -n "$POLICY_FILE" ]]; then

  # Apply firewall
  FW_RES=$(awk '
  BEGIN {
    def_prefix="_FW_"
  }
  #/\*nat/ {
  #  prefix=def_prefix "N_"
  #}
  /\*filter/ {
    prefix=def_prefix "F_"
  }
  /INPUT|OUTPUT|FORWARD|PREROUTING|POSTROUTING/ {
#    sub(/PREROUTING/, "%IP%" prefix "PRE");
#    sub(/POSTROUTING/, "%IP%" prefix "POST");
    sub(/INPUT|OUTPUT|FORWARD/, "%IP%" prefix "&");
  }
  {
    print $0
  }' $POLICY_FILE | sed "s/%IP%/$VM_IP_ADDR/g; s/%SSH_PORT%/$SSH_PORT/g" | iptables-restore -n 2>&1)

  RES=$?

  if [ $RES -ne 0 ]; then
    echo "Error: Unable to apply firewall policy: $FW_RES"
    exit 4
  fi

fi

) 200>$SCRIPT_DIR/lock.iptables

exit $?
