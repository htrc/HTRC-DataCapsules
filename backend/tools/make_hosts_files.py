#! /usr/bin/python

# Copyright 2015 University of Michigan
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

# This should only happen on the first use of the scripts;
# Be sure these files are initialized before opening system to real users!

import argparse
import os
import netaddr
import random

def path_type(s):
	if not os.path.exists(s):
		msg = "Error: invalid or nonexistent path given in arguments"
		raise argparse.ArgumentTypeError(msg)
	return s
		
parser = argparse.ArgumentParser(description="Generates network configuration files for a data capsules deployment")
parser.add_argument('dest_dir', type=path_type, help="Path to the directory where the files will be generated")
parser.add_argument('gateway', type=netaddr.IPAddress, help="The gateway host address for the subnet being allocated")
parser.add_argument('netmask', type=netaddr.IPAddress, help="The subnet mask for the subnet being allocated")

args = parser.parse_args()

dhcp_hosts = open(os.path.join(args.dest_dir, "dhcp_hosts"), 'w+')
free_hosts = open(os.path.join(args.dest_dir, "free_hosts"), 'w+')

mac_addr_fmt = "{0:02x}:{1:02x}:{2:02x}:{3:02x}:{4:02x}:{5:02x}"

for ip in netaddr.IPNetwork("{network}/{netmask}".format(network=args.gateway, netmask=args.netmask)).iter_hosts():

	if ip == args.gateway:
		continue

	generated_mac_val = [random.randint(0,255) for k in range(6)]
	generated_mac_val[0] = 0xFE & (generated_mac_val[0] | 0x02) # address is locally administered

	mac_addr = mac_addr_fmt.format(*generated_mac_val)

	dhcp_hosts.write("{mac_addr},{ip_addr}\n".format(mac_addr=mac_addr, ip_addr=ip))
	free_hosts.write("{ip_addr}\n".format(ip_addr=ip))

dhcp_hosts.close()
free_hosts.close()
