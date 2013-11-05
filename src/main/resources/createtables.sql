DROP DATABASE IF EXISTS vmdb;
CREATE DATABASE vmdb;
USE vmdb;

DROP TABLE IF EXISTS vms, users, uservm, images, policies;

CREATE TABLE IF NOT EXISTS images(
	imagename VARCHAR(128) PRIMARY KEY,
	imagepath VARCHAR(512)) ENGINE=InnoDB;
	
CREATE TABLE IF NOT EXISTS policies(
	policyname VARCHAR(128) PRIMARY KEY,
	policypath VARCHAR(512)) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS vms(
	vmid VARCHAR(128) PRIMARY KEY, 
	vmmode VARCHAR(64), 
	vmstate VARCHAR(64), 
	publicip VARCHAR(64), 
	sshport INT, 
	vncport INT, 
	workingdir VARCHAR(512),
	imagename VARCHAR(128),
	vmusername VARCHAR(128),
	vmpassword VARCHAR(128),
	numcpus INT,
	memorysize INT,
	diskspace INT,
	CONSTRAINT fk_images FOREIGN KEY (imagename)
		REFERENCES images(imagename)) ENGINE=InnoDB;

/* More fields will be added */
CREATE TABLE IF NOT EXISTS users(
	username VARCHAR(128) PRIMARY KEY, 
	cpuleftquota INT,
	memoryleftquota INT,
	diskleftquota INT,
	usertype VARCHAR(64)) ENGINE=InnoDB;
	
CREATE TABLE IF NOT EXISTS uservm(
    username VARCHAR(128),
	vmid VARCHAR(128),
	CONSTRAINT fk_users FOREIGN KEY (username)
		REFERENCES users(username),
	CONSTRAINT fk_vms FOREIGN KEY (vmid)
		REFERENCES vms(vmid) ON DELETE CASCADE,
	PRIMARY KEY(username, vmid)) ENGINE=InnoDB;