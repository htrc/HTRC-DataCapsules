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
  echo "Usage: $0 --wdir <Directory for VM> --vmtype <Capsule Type> "
  echo ""
  echo "Launches a VM instance for the VM in the given directory."
  echo ""
  echo "--wdir  Directory: The directory where this VM's data will be held"
  echo ""
  echo "--vmtype  Capsule Type - DEMO or RESEARCH"
  echo ""
  echo "-h|--help Show help."

}

# Initialize all the option variables.
# This ensures we are not contaminated by variables from the environment.
VM_DIR=
DC_TYPE=

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
        --vmtype)       # Takes an option argument; ensure it has been specified.
            if [ "$2" ]; then
                DC_TYPE=$2
                shift
            else
                die 'ERROR: "--vmtype" requires a non-empty option argument.'
            fi
            ;;
        --vmtype=?*)
            DC_TYPE=${1#*=} # Delete everything up to "=" and assign the remainder.
            ;;
        --vmtype=)         # Handle the case of an empty --vmtype=
            die 'ERROR: "--vmtype" requires a non-empty option argument.'
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

if [[ -z "$VM_DIR" || -z "$DC_TYPE"  ]]; then
  printf 'WARN: Missing required argument'  >&2
  usage
  exit 1
fi

if [[ ! -d $VM_DIR ]] ; then
  echo "Error: Invalid VM directory specified!"
  exit 2
fi

# Check if VM is already running
if [[ `$SCRIPT_DIR/vmstatus.sh --wdir $VM_DIR` =~ "Status:  Running" ]]; then
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
sudo $SCRIPT_DIR/fw.sh $VM_DIR $MAINTENANCE_POLICY
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




logger "$VM_DIR Waiting for the capsule to come up..."

start=$SECONDS
while ! nc -z $VM_IP_ADDR 22; do
  sleep 0.5 # wait for 1/10 of the second before check again
  end=$SECONDS
  elapsed=$(( end - start ))
  if (( elpased > 180 )); then
    logger "$VM_DIR Capsule startup timed out."
    exit 6
  fi
done

# Replace .htrc file
# Remove password and provision user if NO_PASSWORD is not set.
# Add user's ssh key and guacamole client's ssh key

logger "$VM_DIR Remove VM IP $VM_IP_ADDR  from known_hosts file."
ex -s +"g/$VM_IP_ADDR/d" -cwq $KNOWN_HOSTS

if [[ -z "$NO_PASSWORD" ]]; then
      logger "$VM_DIR Add guest scripts and uploads directories into /tmp in DC"
       sshpass -p 'dcuser' scp -o StrictHostKeyChecking=no -r $GUEST_UPLOADS dcuser@$VM_IP_ADDR:/tmp/
       sshpass -p 'dcuser' scp -o StrictHostKeyChecking=no -r $GUEST_SCRIPTS dcuser@$VM_IP_ADDR:/tmp/

       logger "$VM_DIR Provisioning the capsule."
       sshpass -p 'dcuser' ssh -o StrictHostKeyChecking=no dcuser@$VM_IP_ADDR "/bin/sh /tmp/guest_scripts/existing_capsule_provisioning.sh " > $VM_DIR/provisioning_out 2>&1

       logger "$VM_DIR Update Voyant version to 2.4_M7"
       ssh -o StrictHostKeyChecking=no  -i $ROOT_PRIVATE_KEY root@$VM_IP_ADDR "/bin/sh /tmp/guest_scripts/update_voyant.sh dcuser " > $VM_DIR/update_voyant_out 2>&1

       logger "$VM_DIR enable logging of root user activity."
       ssh -o StrictHostKeyChecking=no  -i $ROOT_PRIVATE_KEY root@$VM_IP_ADDR "cat  /tmp/guest_uploads/enableSyslogging >> /root/.bashrc"

       ssh -o StrictHostKeyChecking=no  -i $ROOT_PRIVATE_KEY root@$VM_IP_ADDR "cp /tmp/guest_uploads/release-upgrades /etc/update-manager/release-upgrades " > $VM_DIR/disable_release_out 2>&1

       ssh -o StrictHostKeyChecking=no  -i $ROOT_PRIVATE_KEY root@$VM_IP_ADDR "/bin/sh /tmp/guest_scripts/remove_password.sh dcuser " > $VM_DIR/remove_password_out 2>&1

       logger "$VM_DIR Waiting for the capsule to come up after reboot"

       sleep 5 # wait till vm is shutdown
       start=$SECONDS
       while ! nc -z $VM_IP_ADDR 22; do
           sleep 0.5 # wait for 1/10 of the second before check again
           end=$SECONDS
           elapsed=$(( end - start ))
           if (( elpased > 180 )); then
              logger "$VM_DIR Capsule startup timed out."
              exit 6
            fi
        done

       echo "NO_PASSWORD=1" >> $VM_DIR/config
       echo "DISABLE_NEW_RELEASE=1" >> $VM_DIR/config
       logger "$VM_DIR Disabled new Ubuntu releases and password of dcuser."
fi

if [[ -z "$DISABLE_NEW_RELEASE" ]]; then
      scp -o StrictHostKeyChecking=no -i $ROOT_PRIVATE_KEY $GUEST_UPLOADS/release-upgrades root@$VM_IP_ADDR:/etc/update-manager/release-upgrades > $VM_DIR/disable_release_out 2>&1
      echo "DISABLE_NEW_RELEASE=1" >> $VM_DIR/config
      logger "$VM_DIR Disabled new Ubuntu releases."
fi

# Check whether authorized_keys file available in VM_DIR. If not get the copy from the VM
if [ ! -e $VM_DIR/authorized_keys ]; then
      scp -o StrictHostKeyChecking=no -i $ROOT_PRIVATE_KEY root@$VM_IP_ADDR:$DC_USER_KEY_FILE $VM_DIR/authorized_keys >> $VM_DIR/copy_authorized_keys_out 2>&1
fi

#add/update Guacamole client's public key
$SCRIPT_DIR/updategmckey.sh --wdir $VM_DIR

# copy authorized_keys file from VM_DIR to VM
scp -o StrictHostKeyChecking=no  -i $ROOT_PRIVATE_KEY $VM_DIR/authorized_keys root@$VM_IP_ADDR:$DC_USER_KEY_FILE >> $VM_DIR/copy_authorized_keys_out 2>&1

# update htrc package
logger "$VM_DIR Update htrc package"
scp -o StrictHostKeyChecking=no  -i $GMC_PRIVATE_KEY -r $GUEST_SCRIPTS dcuser@$VM_IP_ADDR:/tmp/ > $VM_DIR/install_python_packages_out 2>&1
ssh -o StrictHostKeyChecking=no  -i $GMC_PRIVATE_KEY dcuser@$VM_IP_ADDR "/opt/anaconda/bin/pip install --upgrade pip && /opt/anaconda/bin/pip install --upgrade htrc-feature-reader && /opt/anaconda/bin/pip install --upgrade htrc && /opt/anaconda/bin/python /tmp/guest_scripts/download_nltk_data.py" >> $VM_DIR/install_python_packages_out 2>&1

ssh -o StrictHostKeyChecking=no  -i $GMC_PRIVATE_KEY dcuser@$VM_IP_ADDR "/bin/rm -r /tmp/guest_scripts" >> $VM_DIR/install_python_packages_out 2>&1

# Set Google Chrome as the default browser
if [[ -z "$SET_CHROME_DEFAULT" ]]; then
  scp -o StrictHostKeyChecking=no  -i $GMC_PRIVATE_KEY $GUEST_UPLOADS/mimeapps.list dcuser@$VM_IP_ADDR:/home/dcuser/.config/mimeapps.list > $VM_DIR/set_chrome_default_out 2>&1
  echo "SET_CHROME_DEFAULT="$(date +%m-%d-%Y) >> $VM_DIR/config
  logger "$VM_DIR set chrome as the default browser"
fi

# Install HTRC-JupyterNotebooks
if [[ -z "$INSTALL_JUPYTER_NOTE_BOOKS" ]]; then
  scp -o StrictHostKeyChecking=no  -i $ROOT_PRIVATE_KEY -r $GUEST_UPLOADS/jupyter-notebooks root@$VM_IP_ADDR:/tmp > $VM_DIR/install_jupyter_notebooks_out 2>&1
  ssh -o StrictHostKeyChecking=no  -i $ROOT_PRIVATE_KEY root@$VM_IP_ADDR "/tmp/jupyter-notebooks/install.sh" >> $VM_DIR/install_jupyter_notebooks_out 2>&1
  echo "INSTALL_JUPYTER_NOTE_BOOKS="$(date +%m-%d-%Y) >> $VM_DIR/config
  echo "CHANGE_ICON_NAME_JNB="$(date +%m-%d-%Y) >> $VM_DIR/config
  logger "$VM_DIR installed HTRC-JupyterNotebooks"
fi

# Load config file to get CHANGE_ICON_NAME_JNB config value
. $VM_DIR/config

#Change JupyterNotebook icon name
if [[ -z "$CHANGE_ICON_NAME_JNB" ]]; then
  scp -o StrictHostKeyChecking=no  -i $GMC_PRIVATE_KEY $GUEST_UPLOADS/jupyter-notebooks/JupyterNotebooks/notebook.desktop dcuser@$VM_IP_ADDR:/home/dcuser/Desktop > $VM_DIR/change_icon_name_jnb_out 2>&1
  echo "CHANGE_ICON_NAME_JNB="$(date +%m-%d-%Y) >> $VM_DIR/config
  logger "$VM_DIR change icon name of HTRC-JupyterNotebooks"
fi


# Return successfully (only reaches here if no errors occur)
exit 0
