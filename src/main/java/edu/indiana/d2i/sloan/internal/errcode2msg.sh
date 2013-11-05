#!/bin/sh

usage() {
	printf "Usage: $0 <error code>\n"
	exit 1
}

[ $# -ne 1 ] && usage

# code 1 is reserved for the uncaught exception

case $1 in
	0) echo "Success" ;;
	1) echo "Unknown(Uncaught) error" ;;
	2) echo "Invalid input arguments" ;;
	3) echo "Image file doesn't exist" ;;
	4) echo "Not enough CPU resource" ;;
	5) echo "Not enough memory resource" ;;
	6) echo "IO exception" ;;
	7) echo "VM doesn't exist" ;;
	8) echo "Cannot find specified firewall policy file" ;;
	9) echo "Invalid requested VM mode" ;;
	10) echo "Cannot find VM state file" ;;
	11) echo "VM is already in the requested mode" ;;
	12) echo "VM is not in running state" ;;
	13) echo "VM is not in shutdown state" ;;
	14) echo "VM already exists" ;;
	*) echo "Unknown error" ;;
esac
