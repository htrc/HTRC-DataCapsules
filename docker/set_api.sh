#!/bin/bash

root_password=$1
dir=$(pwd)
mysql_container="htrc-mysql"

cd dc-api
./docker-build.sh

cd ../mysql
./docker_run.sh $root_password

cd ../dc-api
./docker_run.sh
