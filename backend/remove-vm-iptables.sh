#!/bin/bash

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

# Finding the tap device the VM is attached
VM_TAP_DEV=`iptables -L FORWARD --line-numbers | grep $VM_IP | head -n 1 | grep -oP 'tap\d+'`

echo "VM with IP $VM_IP uses tap device $VM_TAP_DEV"

IPTBLS_EXECUTABLE=`which iptables`
logger "$VM_DIR - iptables executable $IPTBLS_EXECUTABLE"
logger "$VM_DIR - Deleting forward rules.."
# Deleting FORWARD rules related to VM
while true
do
    GREP_OUT=`iptables -L FORWARD --line-numbers | grep $VM_TAP_DEV`
    if [ $? -eq 1 ]; then
	#echo "Done deleting FORWARD chain related rules for VM with IP: $VM_IP"
	break
    fi
    RULE_LINE=`iptables -L FORWARD --line-numbers | grep $VM_TAP_DEV | head -n 1 | grep -oP '^[^\s]*'`
    iptables -D FORWARD $RULE_LINE
done

logger "$VM_DIR - Deleting input rules.."
# Deleting INPUT rules related to VM
while true
do
    GREP_OUT=`iptables -L INPUT --line-numbers | grep $VM_TAP_DEV`
    if [ $? -eq 1 ]; then
	#echo "Done deleting INPUT chain related rules for VM with IP: $VM_IP"
	break
    fi
    RULE_LINE=`iptables -L INPUT --line-numbers | grep $VM_TAP_DEV | head -n 1 | grep -oP '^[^\s]*'`
    iptables -D INPUT $RULE_LINE
done

logger "$VM_DIR - Deleting output rules.."
# Deleting OUTPUT rules related to VM
while true
do
    GREP_OUT=`iptables -L OUTPUT --line-numbers | grep $VM_TAP_DEV`
    if [ $? -eq 1 ]; then
	#echo "Done deleting OUTPUT chain related rules for VM with IP: $VM_IP"
	break
    fi
    RULE_LINE=`iptables -L OUTPUT --line-numbers | grep $VM_TAP_DEV | head -n 1 | grep -oP '^[^\s]*'`
    iptables -D OUTPUT $RULE_LINE
done

logger "$VM_DIR - Deleting nat rules.."
# Deleting rules in nat table related to VM
while true
do
    GREP_OUT=`iptables -L -t nat --line-numbers | grep $SSH_PORT`
    if [ $? -eq 1 ]; then
        echo "Done deleting rules in nat table related to VM with IP: $VM_IP"
        break
    fi
    RULE_LINE=`iptables -L -t nat --line-numbers | grep $SSH_PORT | head -n 1 | grep -oP '^[^\s]*'`
    iptables -t nat -D PREROUTING $RULE_LINE
done

logger "$VM_DIR - Deleting chains.."
echo "Deleting iptables chains for VM with IP $VM_IP"
iptables -F "${VM_IP}_FW_F_FORWARD"
iptables -F "${VM_IP}_FW_F_INPUT"
iptables -F "${VM_IP}_FW_F_OUTPUT"
iptables -X "${VM_IP}_FW_F_FORWARD"
iptables -X "${VM_IP}_FW_F_INPUT"
iptables -X "${VM_IP}_FW_F_OUTPUT"

