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

# Timeout for boot, in seconds
TIMEOUT=30

usage () {

  echo "Usage: $0 <Directory for VM> --mode <Security Mode> --starget <Secure Mode Server> --policy [Policy File] --pubkey <User's ssh key>"
  echo ""
  echo "Launches a VM instance for the VM in the given directory."
  echo ""
  echo "(--wdir)  Directory: The directory where this VM's data will be held"
  echo ""
  echo "--mode    Boot to Secure Mode: One of 's(ecure)' or 'm(aintenance)', denotes whether the"
  echo "          guest being started should be booted into maintenance or secure mode"
  echo ""
#  echo "--starget Secure Mode Target Server: URL target (and port if applicable) for secure mode server"
#  echo ""
  echo "--policy  Policy File: The file that contains the policy for restricting this VM."
  echo "--pubkey  User's ssh public key."

}

#REQUIRED_OPTS="VM_DIR BOOT_SECURE SECURE_TARGET"
REQUIRED_OPTS="VM_DIR BOOT_SECURE POLICY"
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
longoptspec=( [wdir]=1 [mode]=1 [policy]=1 [starget]=1 [pubkey]=1)
optspec=":h-:d:m:s:p:"
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
    s|starget)
      SECURE_TARGET=$OPTARG
      ;;
    p|policy)
      POLICY=$OPTARG
      ;;
    k|pubkey)
      SSH_KEY=${@: -1}
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

# Check if VM is already running
if [[ `$SCRIPT_DIR/vmstatus.sh $VM_DIR` =~ "Status:  Running" ]]; then
  echo "Error: VM is already running!"
  exit 10
fi

# Load config file
. $VM_DIR/config

# Check if full disk image is done copying
if [[ "$IMAGE" = *.diff  && -e $VM_DIR/${IMAGE%.diff} && -s $VM_DIR/${IMAGE%.diff}.newsum ]]; then
  if [[   $(cat $VM_DIR/${IMAGE%.diff}.newsum | awk '{print $1}') \
        = $(cat $VM_DIR/${IMAGE%.diff}.sum | awk '{print $1}') ]]; then
    # integrate diff file to new image
    IMAGE=${IMAGE%.diff}
    qemu-img rebase -u -b $(readlink -f $VM_DIR/$IMAGE) $VM_DIR/${IMAGE}.diff
    qemu-img commit $VM_DIR/${IMAGE}.diff 2>&1 >/dev/null
    sed -i 's#IMAGE=.*$#IMAGE='"$IMAGE"'#' $VM_DIR/config
    rm $VM_DIR/${IMAGE}.{diff,sum,newsum}
  else
    BASE_IMAGE=$(qemu-img info $VM_DIR/$IMAGE | sed -n 's/backing file: \(.*\)$/\1/p')
    if [[ $(md5sum $BASE_IMAGE | awk '{print $1}') \
          = $(cat $VM_DIR/${IMAGE%.diff}.sum | awk '{print $1}') ]]; then
      nohup dd if=$BASE_IMAGE of=$VM_DIR/$IMAGE bs=$DD_BLOCK_SIZE >>$VM_DIR/last_run 2>&1 &
    else
      echo "Error: Unable to copy base image; it has been changed since the VM was created!"
      exit 11
    fi
  fi
fi

if [ -e $VM_DIR/last_run ] ; then
  cat $VM_DIR/last_run >> $VM_DIR/kvm_console
  rm $VM_DIR/last_run
else
  FIRST_RUN=true
fi

cat <<EOF >> $VM_DIR/kvm_console

|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
-------------------------------------------------------------------------------
    Starting New VM Instance; `date`
-------------------------------------------------------------------------------
|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||

EOF

rm -rf $VM_DIR/release_mon

sudo $SCRIPT_DIR/launchkvm.sh

if [ $? -ne 0 ]; then
  echo "Failed to initialize KVM"
  exit 9
fi

# Initialize maintenance mode firewall
sudo $SCRIPT_DIR/fw.sh $VM_DIR $POLICY
POLICY_RES=$?

if [ $POLICY_RES -ne 0 ]; then
  echo "Error: Failed to apply initial firewall policy on startup; error code ($POLICY_RES)"
  exit 8
fi


# Echo process command to logs for debugging purposes
START_VM_COMMAND="nohup $SCRIPT_DIR/tapinit $SCRIPT_DIR $VM_IP_ADDR $QEMU	\
		   -enable-kvm							\
		   -snapshot							\
		   -no-shutdown							\
		   -m $MEM_SIZE							\
		   -smp $NUM_VCPU						\
                   ${VM_NAME:+-name "$VM_NAME"}					\
		   -pidfile $VM_DIR/pid						\
		   -monitor unix:$VM_DIR/monitor,server,nowait			\
		   -serial file:$VM_DIR/release_mon 				\
		   -usb								\
		   -net nic,vlan=0,macaddr=$VM_MAC_ADDR				\
		   -net tap,vlan=0,fd=%FD%					\
		   -hda $VM_DIR/$IMAGE						\
		   -vga vmware                                                  \
		   -vnc :$(( $VNC_PORT - 5900 ))${VNC_LOGIN:+,password}		\
		   >>$VM_DIR/last_run						\
		   2>&1 &"
#		   -drive file=$VM_DIR/$IMAGE,if=virtio				\
#                   ${FIRST_RUN:+-drive file=$VM_DIR/seed.iso,if=virtio}		\

echo "$START_VM_COMMAND" | sed 's/[\t\n ]\+/ /g' >>$VM_DIR/last_run

# Start guest process
nohup $SCRIPT_DIR/tapinit $SCRIPT_DIR $VM_IP_ADDR $QEMU				\
		   -enable-kvm							\
		   -snapshot							\
		   -no-shutdown							\
		   -m $MEM_SIZE							\
		   -smp $NUM_VCPU						\
                   ${VM_NAME:+-name "$VM_NAME"}					\
		   -pidfile $VM_DIR/pid						\
		   -monitor unix:$VM_DIR/monitor,server,nowait			\
		   -serial file:$VM_DIR/release_mon 				\
		   -usb								\
		   -net nic,model=virtio,vlan=0,macaddr=$VM_MAC_ADDR				\
		   -net tap,vlan=0,fd=%FD%					\
		   -hda $VM_DIR/$IMAGE						\
                   -chardev socket,path=$VM_DIR/negotiator-host-to-guest.sock,server,nowait,id=host2guest \
                   -device virtio-serial \
                   -device virtserialport,chardev=host2guest,name=negotiator-host-to-guest.0 \
                   -chardev socket,path=$VM_DIR/negotiator-guest-to-host.sock,server,nowait,id=guest2host \
                   -device virtio-serial \
                   -device virtserialport,chardev=guest2host,name=negotiator-guest-to-host.0 \
		   -vga vmware                                                  \
		   -vnc :$(( $VNC_PORT - 5900 ))${VNC_LOGIN:+,password}		\
		   >>$VM_DIR/last_run						\
		   2>&1 &
#
#		   -drive file=$VM_DIR/$IMAGE,if=virtio				\
#                   ${FIRST_RUN:+-drive file=$VM_DIR/seed.iso,if=virtio}		\

#		   -hda $VM_DIR/$IMAGE						\

# Store the PID of the process for later
KVM_PID=$!

# For some reason, a connection to the monitor seems to be needed
# to trigger VM startup
sleep 2
for time in $(seq 1 $TIMEOUT); do
  if echo "" | nc -U $VM_DIR/monitor >/dev/null; then
    break
  else
    if kill -0 $KVM_PID 2>&1 | grep -q "No such process" ; then
      echo "Error starting guest VM:"
      cat $VM_DIR/last_run
      exit 4
    fi
  fi
  sleep 1
done

TIMEOUT_2=$(( $TIMEOUT - $time ))

if [[ $VNC_LOGIN = 1 ]] ; then
  echo "set_password vnc $LOGIN_PWD" | nc -U $VM_DIR/monitor >/dev/null
fi

# Make sure VM actually started

for time in $(seq 1 $TIMEOUT_2); do
  if ping -c1 -w1 $VM_IP_ADDR >/dev/null 2>&1; then
    break
  else
    if kill -0 $KVM_PID 2>&1 | grep -q "No such process" ; then
      echo "Error starting guest VM:"
      cat $VM_DIR/last_run
      exit 4
    fi
  fi
done

ping -c1 -w1 $VM_IP_ADDR >/dev/null 2>&1

if [ $? -ne 0 ]; then
  if /usr/sbin/pidof `basename $QEMU` | grep -q $KVM_PID; then
    echo "Warning: guest failed to obtain IP address within $TIMEOUT seconds; may have failed to boot"
  else
    echo "Error: guest failed to start"
    exit 5
  fi
fi

# All VMs start in Maintenance mode initially
echo "Maintenance" > $VM_DIR/mode

# Add user's ssh key and guacamole client's ssh key
if [ "$SSH_KEY" ]; then
     sleep 90       # wait till ssh deamon starts
     $SCRIPT_DIR/updatekey.sh --wdir $VM_DIR --pubkey "$SSH_KEY"
fi

# Replace .htrc file
# Remove password and provision user if NO_PASSWORD is not set.
if [ $NO_PASSWORD ] && ["$IMAGE" != *"$UBUNTU_12_04_IMAGE"* ]; then
      scp -o StrictHostKeyChecking=no  -i $ROOT_PRIVATE_KEY $HTRC_CONFIG root@$VM_IP_ADDR:/home/dcuser/.htrc
 elif ["$IMAGE" != *"$UBUNTU_12_04_IMAGE"* ]; then
       sshpass -p 'dcuser' scp -o StrictHostKeyChecking=no -r $UPLOADS $SCRIPTS dcuser@$VM_IP_ADDR:
        sshpass -p 'dcuser' ssh -o StrictHostKeyChecking=no dcuser@$VM_IP_ADDR /home/dcuser/scripts/existing_capsule_provisioning.sh
        ssh -o StrictHostKeyChecking=no  -i $ROOT_PRIVATE_KEY root@$VM_IP_ADDR "/home/dcuser/scripts/remove_password.sh dcuser"
        echo "NO_PASSWORD=1" >> $VM_DIR/config
fi




# Switch machine into secure mode or apply firewall policy as needed
if [[ $BOOT_SECURE = 0 ]]; then
  $SCRIPT_DIR/switch.sh $VM_DIR --mode s --policy $POLICY
  SWITCH_RES=$?

  if [ $SWITCH_RES -ne 0 ]; then
    echo "Error: Failed to switch VM to security mode on startup; error code ($SWITCH_RES)"
    exit 7
  fi

fi

# Return successfully (only reaches here if no errors occur)
exit 0