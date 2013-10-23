DROP DATABASE IF EXISTS vmdb;
CREATE DATABASE vmdb;
USE vmdb;

DROP TABLE IF EXISTS vms, users, uservm;

CREATE TABLE IF NOT EXISTS vms(
	vmid VARCHAR(128) PRIMARY KEY, 
	vmmode VARCHAR(64), 
	vmstate VARCHAR(64), 
	publicip VARCHAR(64), 
	sshport INT, 
	vncport INT, 
	workingdir VARCHAR(128),
	imagename VARCHAR(128),
	vmusername VARCHAR(128),
	vmpassword VARCHAR(128)) ENGINE=InnoDB;

/* More fields will be added */
CREATE TABLE IF NOT EXISTS users(
	username VARCHAR(128) PRIMARY KEY, 
	usertype VARCHAR(64)) ENGINE=InnoDB;
	
CREATE TABLE IF NOT EXISTS uservm(
    username VARCHAR(128),
	vmid VARCHAR(128),
	CONSTRAINT fk_users FOREIGN KEY (username)
		REFERENCES users(username),
	CONSTRAINT fk_vms FOREIGN KEY (vmid)
		REFERENCES vms(vmid) ON DELETE CASCADE,
	PRIMARY KEY(username, vmid)) ENGINE=InnoDB;