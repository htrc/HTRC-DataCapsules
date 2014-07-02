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

DD_BLOCK_SIZE=2048k

SCRIPT_DIR=$(cd $(dirname $0); pwd)
. $SCRIPT_DIR/capsules.cfg

usage () {

  echo "Usage: $0 <Directory for VM> --image <Image Name> --ncpu <Number of CPUs> --mem <Guest Memory Size>"
  echo "       --vnc <VNC Port>   --ssh <SSH Port>        --volsize <Volume Size>"
  echo ""
  echo "Creates a new VM by allocating a directory for it and instantiating configuration files."
  echo ""
  echo "(--wdir)   Directory for VM: The directory where this VM's data will be held"
  echo ""
  echo "--image    Image Name: Image should be a bootable disk image compatible with qemu"
  echo ""
  echo "--ncpu     Number of CPUs: The number of virtual CPUs that will be allocated to this VM"
  echo ""
  echo "--mem      Guest Memory Size: May be specified with a qualifier (e.g. G, M), otherwise assumed to be in megabytes"
  echo ""
  echo "--vnc      VNC Port: The port that will be used to serve the VNC service"
  echo ""
  echo "--ssh      SSH Port: The port that will be used to serve the SSH service"
  echo ""
  echo "--volsize  Volume Size: The size of the secure volume that will be accessed by this VM when in the Secure state"
  echo ""
  echo "--loginid  Login ID: (optional) User ID to be used to log in to VNC sessions"
  echo ""
  echo "--loginpwd Login Password: (optional) Password to be used to log in to VNC sessions"

}

fail () {

  rm -rf $VM_DIR
  exit $1

}

# Name of the secure volume is fixed for now;
SECURE_VOL_NAME=secure_volume

REQUIRED_OPTS="IMAGE NUM_VCPU MEM_SIZE VM_DIR VNC_PORT SSH_PORT SECURE_VOL_SIZE"
ALL_OPTS="$REQUIRED_OPTS LOGIN_ID LOGIN_PWD"
UNDEFINED=12345capsulesxXxXxundefined54321

for var in $ALL_OPTS; do
  eval $var=$UNDEFINED
done

if [[ $1 && $1 != -* ]]; then
  VM_DIR=$1
  shift
fi

declare -A longoptspec
longoptspec=( [wdir]=1 [image]=1 [img]=1 [num-cpu]=1 [ncpu]=1 [vcpu]=1 [mem]=1 [memory]=1 [vnc]=1 [ssh]=1 [volsize]=1 [loginid]=1 [loginpwd]=1 )
optspec=":h-:d:i:c:m:v:s:"
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
    i|img|image)
      IMAGE=$OPTARG
      ;;
    c|num-cpu|ncpu|vcpu)
      NUM_VCPU=$OPTARG
      ;;
    m|mem|memory)
      MEM_SIZE=$OPTARG
      ;;
    v|vnc)
      if ! [[ $OPTARG =~ ^[0-9]+$ && $OPTARG -ge 5900 && $OPTARG -le 65535 ]]; then
        echo "error: provided vnc port ($OPTARG) is invalid;"
        echo "note: the port value must be at least 5900"
        exit 1
      fi
      VNC_PORT=$OPTARG
      ;;
    s|ssh)
      if ! [[ $OPTARG =~ ^[0-9]+$ && $OPTARG -le 65535 ]]; then
        echo "error: provided ssh port ($OPTARG) is invalid"
        exit 1
      fi
      SSH_PORT=$OPTARG
      ;;
    volsize)
      SECURE_VOL_SIZE=$OPTARG
      ;;
    loginid)
      LOGIN_ID=$OPTARG
      REQUIRED_OPTS="$REQUIRED_OPTS LOGIN_PWD"
      ;;
    loginpwd)
      if [[ ${#OPTARG} -gt 8 ]] ; then
        echo "error: login passwords longer than 8 characters are not currently supported"
        exit 1
      fi
      LOGIN_PWD=$OPTARG
      ;;
    h|help)
      usage
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
    echo "error: $var not given"
    MISSING_ARGS=1
  fi
done

if [[ $MISSING_ARGS -eq 1 ]]; then
  usage
  exit 1
fi

if [ -e $VM_DIR ]; then
  echo "Error: VM directory already exists"
  exit 2
fi

# Attempt to create working directory for new guest
MKDIR_RES=$(mkdir -p $VM_DIR 2>&1)

if [ $? -ne 0 ]; then
  echo "Error creating directory for VM: $MKDIR_RES"
  fail 3
fi

# This should only happen on the first use of the scripts;
# Be sure these files are initialized before opening system to real users!
if [[ ! (-e $SCRIPT_DIR/dhcp_hosts && -e $SCRIPT_DIR/free_hosts) ]]; then
  rm -f $SCRIPT_DIR/{dhcp_hosts,free_hosts}

  for SUFFIX in $(seq 2 254); do
    # Generate a locally-administered MAC address
    MAC_ADDR=$(printf '%1x2:%02x:%02x:%02x:%02x:%02x\n' $((RANDOM%16)) $((RANDOM%256)) $((RANDOM%256)) $((RANDOM%256)) $((RANDOM%256)) $((RANDOM%256)))
    IP_ADDR="192.168.53.$SUFFIX"

    echo "$MAC_ADDR,$IP_ADDR" >> $SCRIPT_DIR/dhcp_hosts
    echo "$IP_ADDR" >> $SCRIPT_DIR/free_hosts
  done

  # Push dhcp_hosts configuration to dnsmasq
  if [ -e /var/run/qemu-dnsmasq-br0.pid ]; then
    if pidof dnsmasq | grep -q $(cat /var/run/qemu-dnsmasq-br0.pid); then
      kill -HUP $(cat /var/run/qemu-dnsmasq-br0.pid)
    fi
  fi
fi

# Allocate IP Address
VM_IP_ADDR=$(head -n1 $SCRIPT_DIR/free_hosts)

if [[ -z $VM_IP_ADDR ]]; then
  echo "Error: Unable to allocate IP address; IP pool is exhausted"
  fail 5
fi

VM_MAC_ADDR=$(awk -F, '/'"${VM_IP_ADDR}"'$/{print $1}' $SCRIPT_DIR/dhcp_hosts)
sed -ni '/'"$VM_IP_ADDR"'/!p' $SCRIPT_DIR/free_hosts

# Copy temporary delta image to newly created working directory
CP_RES=$(qemu-img create -o backing_file=$(readlink -f $IMAGE) -f qcow2 $VM_DIR/$(basename $IMAGE).diff)

if [ $? -ne 0 ]; then
  echo "Error copying image file for VM: $CP_RES"
  fail 4
fi

# Copy the full image asynchronously, so that we can return quickly
nohup /usr/bin/nice -n 10 md5sum $IMAGE </dev/null >$VM_DIR/$(basename $IMAGE).sum 2>>$VM_DIR/kvm_console &
(nohup /usr/bin/nice -n 10 dd if=$IMAGE of=$VM_DIR/$(basename $IMAGE) bs=$DD_BLOCK_SIZE 2>>$VM_DIR/kvm_console >>$VM_DIR/kvm_console; \
       /usr/bin/nice -n 10 md5sum $VM_DIR/$(basename $IMAGE) >$VM_DIR/$(basename $IMAGE).newsum 2>>$VM_DIR/kvm_console) </dev/null >/dev/null 2>/dev/null &

# Record configuration parameters to config file
cat <<EOF > $VM_DIR/config

IMAGE=$(basename $IMAGE).diff
SECURE_VOL=$SECURE_VOL_NAME
VM_MAC_ADDR=$VM_MAC_ADDR
VM_IP_ADDR=$VM_IP_ADDR

NUM_VCPU=$NUM_VCPU
MEM_SIZE=$MEM_SIZE
VNC_PORT=$VNC_PORT
SSH_PORT=$SSH_PORT

EOF

# If one is defined, the other must be (as we've checked earlier),
# so this adds a vnc login id and password if given as args
if [[ $LOGIN_PWD != $UNDEFINED ]]; then

  if [[ $LOGIN_ID = $UNDEFINED ]]; then
    LOGIN_ID=""
  fi

  cat <<EOF >> $VM_DIR/config

VNC_LOGIN=1
LOGIN_ID=$LOGIN_ID
LOGIN_PWD=$LOGIN_PWD

EOF

fi

# Create the VM's secure volume
IMG_RES=$(qemu-img create -f raw $VM_DIR/${SECURE_VOL_NAME}.tmp $SECURE_VOL_SIZE 2>&1)

if [ $? -ne 0 ]; then
  echo "Error creating secure volume for VM: $IMG_RES"
  fail 6
fi

MKFS_RES=$(echo "y" | mkfs.ntfs -F -f -L "Secure Volume" $VM_DIR/${SECURE_VOL_NAME}.tmp 2>&1)

if [ $? -ne 0 ]; then
  echo "Error formatting secure volume for VM: $MKFS_RES"
  fail 7
fi

SPOOL_IMG_RES=$(qemu-img create -f raw $VM_DIR/spool_volume 100M 2>&1)

if [ $? -ne 0 ]; then
  echo "Error creating spool volume for VM: $SPOOL_IMG_RES"
  fail 8
fi

SPOOL_MKFS_RES=$(echo "y" | mkfs.ntfs -F -f -L "release_spool" $VM_DIR/spool_volume 2>&1)

if [ $? -ne 0 ]; then
  echo "Error formatting spool volume for VM: $SPOOL_MKFS_RES"
  fail 9
fi

# This conversion to qcow2 format saves significant disk space (for example, 10GB file -> 53MB file)
(nohup qemu-img convert -f raw -O qcow2 $VM_DIR/${SECURE_VOL_NAME}.tmp $VM_DIR/${SECURE_VOL_NAME}.done 2>$VM_DIR/kvm_console >/dev/null \
    && rm -rf $VM_DIR/${SECURE_VOL_NAME}.tmp 2>$VM_DIR/kvm_console >/dev/null \
    && mv $VM_DIR/${SECURE_VOL_NAME}.done $VM_DIR/$SECURE_VOL_NAME 2>$VM_DIR/kvm_console >/dev/null ) </dev/null >/dev/null 2>/dev/null &

# Return results (only reaches here if no errors occur)
exit 0
