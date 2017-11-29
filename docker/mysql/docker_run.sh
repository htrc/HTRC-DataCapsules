#!/bin/bash
root_password=$1
dir=$(pwd)

if [[ -n "$root_password" ]]; then
    docker run --name htrc-mysql -v $dir/scripts:/docker-entrypoint-initdb.d -p 3306:3306 -p 33060:33060 -e MYSQL_ROOT_PASSWORD=$root_password -d mysql/mysql-server:5.7
 else
    echo "Please specify mysql root password"
fi