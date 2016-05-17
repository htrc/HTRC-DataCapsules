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

touch /tmp/rfh_args_check.py
chmod 700 /tmp/rfh_args_check.py

cat >/tmp/rfh_args_check.py <<EOF
#! /usr/bin/python

import argparse
import os
import sys

def path_type(s):
	if not os.path.exists(s):
		msg = "Error: invalid or nonexistent path given in arguments"
		raise argparse.ArgumentTypeError(msg)
	return s

parser = argparse.ArgumentParser(prog='repair_free_hosts.sh', description="Repairs a free_hosts file if it has become corrupted. Administrators must first create a new free_hosts file using make_hosts_files.py before using this tool.")
parser.add_argument('free_hosts', type=path_type, help="Path to the newly created free_hosts file")
parser.add_argument('vm_directories', nargs='+', type=path_type, help="List of all directories in which active VMs reside.  These directories will be combed through in order to produce a list of actively used VMs whose IP addresses will be filtered out of the new free_hosts file.")

args = parser.parse_args()

sys.exit(163)

EOF

/tmp/rfh_args_check.py $@
RES=$?

rm /tmp/rfh_args_check.py

if [[ $RES -ne 163 ]]; then
  if [[ $RES -eq 0 ]]; then
    exit 0
  fi

  exit 1
fi

FREE_HOSTS=$1
shift

for DIR in $@; do
  for VM in $(find $DIR -name "config"); do
    for IP_ADDR in $(grep "VM_IP_ADDR" $VM | awk -F= '{print $2}'); do
      sed -ni '/'"$IP_ADDR"'$/!p' $FREE_HOSTS
    done
  done
done
