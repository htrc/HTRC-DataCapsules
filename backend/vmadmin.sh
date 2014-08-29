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

  echo "Usage: $0 <qemu Disk Image File> --cdrom <CD-ROM Image> --vnc <VNC Port>"
  echo ""
  echo "Launches a VM, allowing for administration and creation of guest images."
  echo ""
  echo "--img     The guest hard disk image file being booted"
  echo ""
  echo "--cdrom   (optional) The CD-ROM image, for instance of an installation disk for the"
  echo "          operating system being installed on the disk image"
  echo ""
  echo "--vnc     The VNC port to be used for administration"

}

REQUIRED_OPTS="VM_IMG"
ALL_OPTS="$REQUIRED_OPTS CD_ROM VNC_PORT"
UNDEFINED=12345capsulesxXxXxundefined54321

for var in $ALL_OPTS; do
  eval $var=$UNDEFINED
done

if [[ $1 && $1 != -* ]]; then
  VM_IMG=$1
  shift
fi

declare -A longoptspec
longoptspec=( [img]=1 [cdrom]=1 [vnc]=1 )
optspec=":h-:v:"
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
    i|img)
      VM_IMG=$OPTARG
      ;;
    c|cdrom)
      CD_ROM=$OPTARG
      ;;
    v|vnc)
      if ! [[ $OPTARG =~ ^[0-9]+$ && $OPTARG -ge 5900 && $OPTARG -le 65535 ]]; then
        echo "error: provided vnc port ($OPTARG) is invalid;"
        echo "note: the port value must be at least 5900"
        exit 1
      fi
      VNC_PORT=$OPTARG
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

if [ ! -e $VM_IMG ] ; then
  echo "Error: Invalid disk image specified!"
  exit 2
fi

if [[ $CD_ROM = $UNDEFINED ]]; then
  CD_ROM=
fi

sudo $SCRIPT_DIR/launchkvm.sh

# Start guest process
nohup $QEMU						\
		   -enable-kvm				\
		   -m 2G				\
		   -smp 1				\
		   -usb					\
		   -hda $VM_IMG				\
		   ${CD_ROM:+-cdrom $CD_ROM}		\
		   -vnc :$(( $VNC_PORT - 5900 ))	\
		   >>/dev/null				\
		   2>&1 &

exit 0
