#!/bin/bash
#
# ./db-migration-un_to_guid.sh <user> <passwd> <host> <db_name> <input_csv_file>
#
# Input CSV File format
#USERNAME,GUID
#username-0,000
#username-1,001
#username-2,002
#username-3,003
#

if [ "$1" == "" ] || [ "$2" == "" ] || [ "$3" == "" ] || [ "$4" == "" ] || [ "$5" == "" ]; then
    echo "Please add all inputs. Syntex : ./db-migration-un_to_guid.sh <user> <passwd> <host> <db_name> <input_csv_file>"
    display_help
    exit 1
fi

DATE=`date "+%Y-%m-%d"`

# Create a DB backup
mysqldump -u $1 -p$2 -h $3 --databases $4 > $4-backup-$DATE.sql

# Create temporary table 'user_guid_map' and populate with CSV data
mysql --local-infile -u $1 -h $3 -p$2 << EOF

use $4;

CREATE TABLE user_guid_map(username VARCHAR(50) NOT NULL, guid VARCHAR(50) NOT NULL) ;
LOAD DATA LOCAL INFILE '$5' INTO TABLE  user_guid_map FIELDS TERMINATED BY ',' LINES TERMINATED BY '\n' IGNORE 1 ROWS (username, guid) ;

EOF


# Change DB schema to use 'guid' instead of 'username' in all tables
mysql -u $1 -h $3 -p$2 << EOF

USE $4;

# Add guid column to users table
ALTER TABLE users ADD COLUMN guid VARCHAR(64) AFTER username;
# Populate guid data from user_guid_map
UPDATE users t1 INNER JOIN user_guid_map t2 ON t1.username = t2.username SET t1.guid = t2.guid ;
# Update guid=username for non existing guid's
UPDATE users set guid=username where guid is NULL;
# Make the users.guid column not null
ALTER TABLE users MODIFY COLUMN guid VARCHAR(64) NOT NULL;


# Add guid column to uservmmap table
ALTER TABLE uservmmap ADD COLUMN guid VARCHAR(64) AFTER username;
# Populate guid data from user_guid_map
UPDATE uservmmap t1 INNER JOIN user_guid_map t2 ON t1.username = t2.username SET t1.guid = t2.guid ;
# Update guid=username for non existing guid's
UPDATE uservmmap set guid=username where guid is NULL;
# Make the uservmmap.guid column not null
ALTER TABLE uservmmap MODIFY COLUMN guid VARCHAR(64) NOT NULL;


# Add guid column to vmactivity table
ALTER TABLE vmactivity ADD COLUMN guid VARCHAR(64) AFTER username;
# Populate guid data from user_guid_map
UPDATE vmactivity t1 INNER JOIN user_guid_map t2 ON t1.username = t2.username SET t1.guid = t2.guid ;
# Update guid=username for non existing guid's
UPDATE vmactivity set guid=username where guid is NULL;
# Make the vmactivity.guid column not null
ALTER TABLE vmactivity MODIFY COLUMN guid VARCHAR(64) NOT NULL;


# Add guid column to vms table
ALTER TABLE vms ADD COLUMN guid VARCHAR(64) AFTER username;
# Populate guid data from user_guid_map
UPDATE vms t1 INNER JOIN user_guid_map t2 ON t1.username = t2.username SET t1.guid = t2.guid ;
# Update guid=username for non existing guid's
UPDATE vms set guid=username where guid is NULL;
# Make the vms.guid column not null
ALTER TABLE vms MODIFY COLUMN guid VARCHAR(64) NOT NULL;


# Drop foreign keys of uservmmap, vmactivity and vms that are referring to username in users table
ALTER TABLE  uservmmap DROP FOREIGN KEY fk_m_username;
ALTER TABLE  vmactivity DROP FOREIGN KEY fk_username;
ALTER TABLE  vms DROP FOREIGN KEY fk_users;

# Drop primary keys of uservmmap and add (vmid,guid) as new primary key
ALTER TABLE  uservmmap DROP PRIMARY KEY , ADD PRIMARY KEY (vmid,guid);

# Drop indexes of uservmmap, vmactivity and vms that are referring to username
DROP INDEX fk_users_idx ON vms;
DROP INDEX fk_username_idx ON vmactivity;
DROP INDEX fk_m_username_idx ON uservmmap;

# Drop primary key of users(username) table and add 'guid' as the new primary key
ALTER TABLE  users DROP PRIMARY KEY , ADD PRIMARY KEY (guid);

# Re-add foreign keys to tables uservmmap, vmactivity,vms and refer that to guid column in users table
ALTER TABLE uservmmap ADD CONSTRAINT fk_m_guid FOREIGN KEY (guid) REFERENCES htrcvirtdb.users (guid) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE vmactivity ADD CONSTRAINT fk_guid FOREIGN KEY (guid) REFERENCES htrcvirtdb.users (guid) ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE vms ADD CONSTRAINT fk_users FOREIGN KEY (guid) REFERENCES htrcvirtdb.users (guid) ON DELETE NO ACTION ON UPDATE NO ACTION;

# Recreate indexes of uservmmap, vmactivity,vms tables with guid
CREATE INDEX fk_users_idx ON vms (guid);
CREATE INDEX fk_guid_idx ON vmactivity (guid);
CREATE INDEX fk_m_guid_idx ON uservmmap (guid);

# drop username column from users, vms, vmactivity and uservmmap tables
ALTER TABLE  uservmmap DROP COLUMN username;
ALTER TABLE  vmactivity DROP COLUMN username;
ALTER TABLE  vms DROP COLUMN username;
ALTER TABLE  users DROP COLUMN username;

# drop the temporary created table
DROP TABLE user_guid_map;

EOF
