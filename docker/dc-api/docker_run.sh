#!/bin/bash
dir=$(pwd)

docker run --name htrc-dcapi -v $dir/config:/etc/htrc/dcapi --link htrc-mysql:mysql -p 8080:8080 -d htrc/dcapi
