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

# Timeout for boot, in seconds
TIMEOUT=120

usage () {

  echo "Usage: $0 <Directory for VM> --mode <Security Mode> --policy [Policy File]"
  echo ""
  echo "Launches a VM instance for the VM in the given directory."
  echo ""
  echo "(--wdir)  Directory: The directory where this VM's data will be held?"
  echo ""
  echo "--mode    Boot to Secure Mode: One of 's(ecure)' or 'm(aintenance)', denotes whether the"
  echo "          guest being started should be booted into maintenance or secure mode"
  echo ""
  echo "--policy  Policy File: The file that contains the policy for restricting this VM when in the Secure state"

}

REQUIRED_OPTS="VM_DIR BOOT_SECURE"
ALL_OPTS="$REQUIRED_OPTS POLICY"
UNDEFINED=12345capsulesxXxXxundefined54321

for var in $ALL_OPTS; do
  eval $var=$UNDEFINED
done

if [[ $1 && $1 != -* ]]; then
  VM_DIR=$1
  shift
fi

declare -A longoptspec
longoptspec=( [wdir]=1 [mode]=1 [policy]=1 )
optspec=":h-:d:m:p:"
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
    m|mode)
      RAW_BOOT_MODE=$OPTARG

      # Ensure mode string has proper format
      if [ -z $RAW_BOOT_MODE -o ${RAW_BOOT_MODE:0:1} != 's' -a ${RAW_BOOT_MODE:0:1} != 'm' ]; then
        usage
        exit 1
      fi
      
      [ ${RAW_BOOT_MODE:0:1} = 's' ]
      BOOT_SECURE=$?

      if [ $BOOT_SECURE = 0 ]; then
        REQUIRED_OPTS="$REQUIRED_OPTS POLICY"
      fi
      ;;
    p|policy)
      POLICY=$OPTARG
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

# TODO: check if VM is already running

# Load config file
. $VM_DIR/config

if [ -e $VM_DIR/last_run ] ; then
  cat $VM_DIR/last_run >> $VM_DIR/kvm_console
  rm $VM_DIR/last_run
fi

cat <<EOF >> $VM_DIR/kvm_console

|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
-------------------------------------------------------------------------------
    Starting New VM Instance; `date`
-------------------------------------------------------------------------------
|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||

EOF

# Start guest process
nohup $SCRIPT_DIR/tapinit qemu-system-x86_64					\
		   -enable-kvm							\
		   -snapshot							\
		   -no-shutdown							\
		   -m $MEM_SIZE							\
		   -smp $NUM_VCPU						\
		   -pidfile $VM_DIR/pid						\
		   -monitor unix:$VM_DIR/monitor,server				\
		   -serial file:$VM_DIR/guest_out				\
		   -usb								\
		   -net nic,vlan=0,macaddr=$VM_MAC_ADDR				\
		   -net tap,vlan=0,fd=%FD%					\
		   -hda $VM_DIR/$IMG						\
		   -vnc :$(( $VNC_PORT - 5900 ))${VNC_LOGIN:+",password"}	\
		   >>$VM_DIR/last_run						\
		   2>&1 &

# Store the PID of the process for later
KVM_PID=$!

# For some reason, a connection to the monitor seems to be needed
# to trigger VM startup
sleep 2
echo "" | nc -U $VM_DIR/monitor >/dev/null

if [[ $VNC_LOGIN = 1 ]] ; then
  echo "set_password vnc $LOGIN_PWD" | nc -U $VM_DIR/monitor >/dev/null
fi

# Make sure VM actually started
if kill -0 $KVM_PID 2>&1 | grep -q "No such process" ; then
  echo "Error starting guest VM:"
  cat $VM_DIR/last_run
  exit 4
fi

for time in $(seq 1 $TIMEOUT); do
  if ping -c1 -w1 $VM_IP_ADDR >/dev/null 2>&1; then
    break
  fi
#  if [ -e $VM_DIR/guest_out ]; then
#    if grep -q "FINISHED BOOTING" $VM_DIR/guest_out; then
#      break
#    fi
#  fi
#  sleep 1
done

ping -c1 -w1 $VM_IP_ADDR >/dev/null 2>&1

if [ $? -ne 0 ]; then
  echo "Guest start-up timeout; guest failed to boot within $TIMEOUT seconds"
  #$SCRIPT_DIR/stopvm.sh $VM_DIR
  exit 5
fi

# Setup SSH port forwarding
SSH_RES=$(sudo $SCRIPT_DIR/sshfwd.sh up $VM_MAC_ADDR $SSH_PORT 2>&1)

if [ $? -ne 0 ]; then
  echo "$SSH_RES"
  exit 6
fi

# All VMs start in Maintenance mode initially
echo "Maintenance" > $VM_DIR/mode

# Switch machine into secure mode or apply firewall policy as needed
if [[ $BOOT_SECURE = 0 ]]; then 
  $SCRIPT_DIR/switch.sh $VM_DIR --mode s --policy $POLICY
  SWITCH_RES=$?

  if [ $SWITCH_RES -ne 0 ]; then
    echo "Error: Failed to switch VM to security mode on startup; error code ($SWITCH_RES)"
    exit 7
  fi

else

  if [[ $POLICY != $UNDEFINED ]]; then
    sudo $SCRIPT_DIR/fw.sh $VM_MAC_ADDR $POLICY
    POLICY_RES=$?

    if [ $POLICY_RES -ne 0 ]; then
      echo "Error: Failed to apply firewall policy on startup; error code ($POLICY_RES)"
      exit 8
    fi
  fi

fi

# Return successfully (only reaches here if no errors occur)
exit 0
