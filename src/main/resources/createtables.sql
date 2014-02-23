DROP DATABASE IF EXISTS vmdb;
CREATE DATABASE vmdb;
USE vmdb;

DROP TABLE IF EXISTS vms, users, uservm, images, policies;

CREATE TABLE IF NOT EXISTS images(
	imagename VARCHAR(128) PRIMARY KEY,
	imagedescription VARCHAR(1024),
	imagepath VARCHAR(512),
	loginusername VARCHAR(32),
	loginpassword VARCHAR(128)) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS vms(
	vmid VARCHAR(128) PRIMARY KEY, 
	vmmode VARCHAR(64), 
	vmstate VARCHAR(64), 
	publicip VARCHAR(64), 
	sshport INT, 
	vncport INT, 
	workingdir VARCHAR(512),
	imagename VARCHAR(128),
	vncusername VARCHAR(128),
	vncpassword VARCHAR(128),
	numcpus INT,
	memorysize INT,
	diskspace INT,
	CONSTRAINT fk_images FOREIGN KEY (imagename)
		REFERENCES images(imagename)) ENGINE=InnoDB;

/* More fields will be added */
CREATE TABLE IF NOT EXISTS users(
	username VARCHAR(128) PRIMARY KEY,
	useremail VARCHAR(128) NOT NULL,
	cpuleftquota INT,
	memoryleftquota INT,
	diskleftquota INT,
	usertype VARCHAR(64)) ENGINE=InnoDB;
	
CREATE TABLE IF NOT EXISTS uservm(
    username VARCHAR(128),
	vmid VARCHAR(128),
	deleted TINYINT NOT NULL DEFAULT 0, /*0: false, 1: true*/
	CONSTRAINT fk_users FOREIGN KEY (username)
		REFERENCES users(username),
	/*CONSTRAINT fk_vms FOREIGN KEY (vmid) REFERENCES vms(vmid) ON DELETE CASCADE,*/
	PRIMARY KEY(username, vmid)) ENGINE=InnoDB;
	
/* randomid(128) tells MySQL only indexes first 128 chars. See http://bugs.mysql.com/bug.php?id=6604 */
CREATE TABLE IF NOT EXISTS results(
	vmid VARCHAR(128), 
	resultid VARCHAR(256),
	datafield LONGBLOB,
	starttime DATETIME,
	notified TINYINT NOT NULL DEFAULT 0, /*0: false, 1: true*/
	/* CONSTRAINT fk_results FOREIGN KEY (vmid) REFERENCES uservm(vmid), */
	PRIMARY KEY(vmid, resultid(128))) ENGINE=InnoDB;
