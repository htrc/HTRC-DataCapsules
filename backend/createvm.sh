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

  echo "Usage: $0 <Directory for VM> --image <Image Name> --ncpu <Number of CPUs> --mem <Guest Memory Size>"
  echo "       --vnc <VNC Port>   --ssh <SSH Port>        --volsize <Volume Size> "
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
  echo ""
  echo "-h|--help Show help."

}

# Initialize all the option variables.
# This ensures we are not contaminated by variables from the environment.
VM_DIR=
IMAGE=
NUM_VCPU=
MEM_SIZE=
VNC_PORT=
SSH_PORT=
SECURE_VOL_SIZE=
LOGIN_ID=
LOGIN_PWD=

while :; do
    case $1 in
        -h|-\?|--help)
            usage    # Display a usage synopsis.
            exit
            ;;
        --wdir)       # Takes an option argument; ensure it has been specified.
            if [ "$2" ]; then
                VM_DIR=$2
                shift
            else
                die 'ERROR: "--wdir" requires a non-empty option argument.'
            fi
            ;;
        --wdir=?*)
            VM_DIR=${1#*=} # Delete everything up to "=" and assign the remainder.
            ;;
        --wdir=)         # Handle the case of an empty --wdir=
            die 'ERROR: "--wdir" requires a non-empty option argument.'
            ;;
        --image)       # Takes an option argument; ensure it has been specified.
            if [ "$2" ]; then
                IMAGE=$2
                shift
            else
                die 'ERROR: "--image" requires a non-empty option argument.'
            fi
            ;;
        --image=?*)
            IMAGE=${1#*=} # Delete everything up to "=" and assign the remainder.
            ;;
        --image=)         # Handle the case of an empty --image=
            die 'ERROR: "--wdir" requires a non-empty option argument.'
            ;;
        --ncpu)       # Takes an option argument; ensure it has been specified.
            if [ "$2" ]; then
                NUM_VCPU=$2
                shift
            else
                die 'ERROR: "--ncpu" requires a non-empty option argument.'
            fi
            ;;
        --ncpu=?*)
            NUM_VCPU=${1#*=} # Delete everything up to "=" and assign the remainder.
            ;;
        --ncpu=)         # Handle the case of an empty --ncpu=
            die 'ERROR: "--ncpu" requires a non-empty option argument.'
            ;;
        --mem)       # Takes an option argument; ensure it has been specified.
            if [ "$2" ]; then
                MEM_SIZE=$2
                shift
            else
                die 'ERROR: "--mem" requires a non-empty option argument.'
            fi
            ;;
        --mem=?*)
            MEM_SIZE=${1#*=} # Delete everything up to "=" and assign the remainder.
            ;;
        --mem=)         # Handle the case of an empty --mem=
            die 'ERROR: "--mem" requires a non-empty option argument.'
            ;;
        --vnc)       # Takes an option argument; ensure it has been specified.
            if [ "$2" ]; then
                VNC_PORT=$2
                shift
            else
                die 'ERROR: "--vnc" requires a non-empty option argument.'
            fi
            ;;
        --vnc=?*)
            VNC_PORT=${1#*=} # Delete everything up to "=" and assign the remainder.
            ;;
        --vnc=)         # Handle the case of an empty --vnc=
            die 'ERROR: "--vnc" requires a non-empty option argument.'
            ;;
        --ssh)       # Takes an option argument; ensure it has been specified.
            if [ "$2" ]; then
                SSH_PORT=$2
                shift
            else
                die 'ERROR: "--ssh" requires a non-empty option argument.'
            fi
            ;;
        --ssh=?*)
            SSH_PORT=${1#*=} # Delete everything up to "=" and assign the remainder.
            ;;
        --ssh=)         # Handle the case of an empty --ssh=
            die 'ERROR: "--ssh" requires a non-empty option argument.'
            ;;
        --volsize)       # Takes an option argument; ensure it has been specified.
            if [ "$2" ]; then
                SECURE_VOL_SIZE=$2
                shift
            else
                die 'ERROR: "--volsize" requires a non-empty option argument.'
            fi
            ;;
        --volsize=?*)
            SECURE_VOL_SIZE=${1#*=} # Delete everything up to "=" and assign the remainder.
            ;;
        --volsize=)         # Handle the case of an empty --volsize=
            die 'ERROR: "--volsize" requires a non-empty option argument.'
            ;;
        --loginid)       # Takes an option argument; ensure it has been specified.
            if [ "$2" ]; then
                LOGIN_ID=$2
                shift
            else
                die 'ERROR: "--loginid" requires a non-empty option argument.'
            fi
            ;;
        --loginid=?*)
            LOGIN_ID=${1#*=} # Delete everything up to "=" and assign the remainder.
            ;;
        --loginid=)         # Handle the case of an empty --loginid=
            die 'ERROR: "--loginid" requires a non-empty option argument.'
            ;;
        --loginpwd)       # Takes an option argument; ensure it has been specified.
            if [ "$2" ]; then
                LOGIN_PWD=$2
                shift
            else
                die 'ERROR: "--loginpwd" requires a non-empty option argument.'
            fi
            ;;
        --loginpwd=?*)
            LOGIN_PWD=${1#*=} # Delete everything up to "=" and assign the remainder.
            ;;
        --loginpwd=)         # Handle the case of an empty --loginpwd=
            die 'ERROR: "--loginpwd" requires a non-empty option argument.'
            ;;
        --)              # End of all options.
            shift
            break
            ;;
        -?*)
            printf 'WARN: Unknown option (ignored): %s\n' "$1" >&2
            usage
            exit 1
            ;;
        *)               # Default case: No more options, so break out of the loop.
            break
    esac

    shift
done

if [[ -z "$VM_DIR" || -z "$IMAGE" || -z "$NUM_VCPU" || -z "$MEM_SIZE" || -z "$VNC_PORT" || -z "$SSH_PORT" || -z "$SECURE_VOL_SIZE" || -z "$LOGIN_ID" || -z "$LOGIN_PWD" ]]; then
  printf 'WARN: Missing required argument'  >&2
  usage
  exit 1
fi

if ! [[ "$VNC_PORT" =~ ^[0-9]+$ && "$VNC_PORT" -ge 5900 && "$VNC_PORT" -le 65535 ]]; then
     echo "error: provided vnc port ($VNC_PORT) is invalid;"
     echo "note: the port value must be at least 5900"
     exit 1
fi

if ! [[ "$SSH_PORT" =~ ^[0-9]+$ && "$SSH_PORT" -le 65535 ]]; then
     echo "error: provided ssh port ($SSH_PORT) is invalid;"
     exit 1
fi

if [[ "$LOGIN_PWD" -gt 8 ]] ; then
     echo "error: login passwords longer than 8 characters are not currently supported"
     exit 1
fi

fail () {

  rm -rf $VM_DIR
  exit $1

}

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

# Allocate IP Address
VM_IP_ADDR=$(head -n1 $FREE_HOSTS)

if [[ -z $VM_IP_ADDR ]]; then
  echo "Error: Unable to allocate IP address; IP pool is exhausted"
  fail 5
fi

VM_MAC_ADDR=$(awk -F, '/'"${VM_IP_ADDR}"'$/{print $1}' $SCRIPT_DIR/dhcp_hosts)
sed -ni '/'"$VM_IP_ADDR"'$/!p' $FREE_HOSTS

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

logger "$VM_DIR:$IMAGE:$VM_IP_ADDR:$VNC_PORT - Enabling negotiator."
cat <<EOF >> $VM_DIR/config
     NEGOTIATOR_ENABLED=1
EOF

#This is to identify new capsules created after adding passwordless image. In dev-stack, any capsule creates after April 6th 2018 are password less capsules and need to upload user's ssh pub key to access capsules via ssh

cat <<EOF >> $VM_DIR/config
     NO_PASSWORD=1
EOF

# Create the VM's secure volume
IMG_RES=$(qemu-img create -f raw $VM_DIR/${SECURE_VOL_NAME}.tmp $SECURE_VOL_SIZE 2>&1)

if [ $? -ne 0 ]; then
  echo "Error creating secure volume for VM: $IMG_RES"
  fail 6
fi

SPOOL_IMG_RES=$(qemu-img create -f raw $VM_DIR/spool_volume 10240M 2>&1)

if [ $? -ne 0 ]; then
  echo "Error creating spool volume for VM: $SPOOL_IMG_RES"
  fail 8
fi

#Create  mounts for the secure volume and the spool volume 
MKFS_RES=$(echo "y" | /sbin/mkfs.ext4 -F -L "secure_volume" $VM_DIR/${SECURE_VOL_NAME}.tmp 2>&1)

if [ $? -ne 0 ]; then
	echo "Error formatting secure volume for VM: $MKFS_RES"
	fail 7
fi

SPOOL_MKFS_RES=$(echo "y" | /sbin/mkfs.ext4 -F -L "release_spool" $VM_DIR/spool_volume 2>&1)

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
