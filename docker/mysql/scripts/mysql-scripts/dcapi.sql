DROP DATABASE IF EXISTS htrcvirtdb;
CREATE DATABASE htrcvirtdb;
GRANT ALL ON htrcvirtdb.* TO htrcvirt@"%" IDENTIFIED BY 'htrcvirtpassword';
FLUSH PRIVILEGES;
