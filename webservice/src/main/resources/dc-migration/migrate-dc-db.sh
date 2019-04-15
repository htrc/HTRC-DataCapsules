#!/bin/bash
#
# ./migrate-dc-db.sh <user> <passwd> <old_db> <copy_of_old_db> <new_db> <new_db_schema>
#

# Creating a new database
mysql -u $1 -p$2 << EOF
# Create temporary db to copy old db
CREATE DATABASE $4;
# Create new db for the new schema
DROP DATABASE IF EXISTS $5;
CREATE DATABASE $5 DEFAULT CHARACTER SET utf8;
GRANT ALL PRIVILEGES ON $5.* TO 'htrc'@'localhost';
EOF

# Copying old database to new temporary database
mysqldump -u $1 -p$2 $3 | mysql -u $1 -p$2 $4

# Add new schema to new database
mysql -u $1 -p$2 $5 < $6

mysql -u $1 -p$2 << EOF
# Migrate images table
USE $4;
ALTER TABLE images ADD COLUMN status varchar(128) DEFAULT "DEPRICATED" after imagename;
UPDATE images SET status="ACTIVE" where imagename="ubuntu-16-04";

INSERT INTO $5.images SELECT * from $4.images;

# Migrate users table
INSERT INTO $5.users SELECT * from $4.users;

# Insert values to vmhosts table in new database
INSERT INTO $5.vmhosts (hostname , cpu_cores , mem_gb) VALUES ("thatchpalm.pti.indiana.edu",24,96), ("silverpalm.pti.indiana.edu",24,96);

# Migrate vms table
ALTER TABLE vms ADD created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE vms ADD username VARCHAR(128);
update vms set username=(select uservm.username from uservm where uservm.vmid=vms.vmid);
alter table vms change column publicip host VARCHAR(128) after username;
INSERT INTO $5.vms SELECT * from $4.vms;

# Migrate results table
alter table results add notifiedtime TIMESTAMP NULL;
alter table results change column notified notified VARCHAR(128);
update results set notified= case when notified=1 then "YES" else "NO" end;
INSERT INTO $5.results SELECT $4.results.* from $4.results, $4.vms where $4.results.vmid=$4.vms.vmid;

# Migrate uservm table to vmactivity table
RENAME TABLE uservm TO vmactivity;
ALTER TABLE vmactivity ADD COLUMN prev_mode varchar(128) DEFAULT "NOT_DEFINED" AFTER vmid;
ALTER TABLE vmactivity ADD COLUMN curr_mode varchar(128) AFTER prev_mode;
UPDATE vmactivity set curr_mode=(select vms.vmmode from vms where vms.vmid=vmactivity.vmid);
ALTER TABLE vmactivity ADD COLUMN prev_state varchar(128) DEFAULT "SHUTDOWN" AFTER curr_mode;
ALTER TABLE vmactivity ADD COLUMN curr_state varchar(128) AFTER prev_state;
UPDATE vmactivity set curr_state=(select vms.vmstate from vms where vms.vmid=vmactivity.vmid);
ALTER TABLE vmactivity DROP COLUMN deleted;
ALTER TABLE vmactivity MODIFY COLUMN username VARCHAR(128) AFTER curr_state;
ALTER TABLE vmactivity DROP FOREIGN KEY fk_users;
ALTER TABLE vmactivity DROP PRIMARY KEY;
ALTER TABLE vmactivity ADD id BIGINT NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY(id);
ALTER TABLE vmactivity ADD ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP AFTER id;
INSERT INTO $5.vmactivity SELECT $4.vmactivity.* from $4.vmactivity, $4.vms where $4.vmactivity.vmid=$4.vms.vmid;

EOF
