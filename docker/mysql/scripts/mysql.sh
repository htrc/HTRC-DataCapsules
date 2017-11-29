#!/bin/sh -eux

# Create the MySQL database
mysql --user=root  --password=$MYSQL_ROOT_PASSWORD < /docker-entrypoint-initdb.d/mysql-scripts/dcapi.sql
echo "dcapi.sql done"

# Set up database for dc api
mysql --user=root  --password=$MYSQL_ROOT_PASSWORD htrcvirtdb < /docker-entrypoint-initdb.d/mysql-scripts/dcapi_schema.sql
echo "dcapi_schema.sql done"
mysql --user=root  --password=$MYSQL_ROOT_PASSWORD htrcvirtdb < /docker-entrypoint-initdb.d/mysql-scripts/dcapi_loaddata.sql
echo "dcapi_loaddata.sql done"
