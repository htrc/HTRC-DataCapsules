USE vmdb;

INSERT INTO images (imagename, imagepath, imagedescription, loginusername, loginpassword) VALUES ("ubuntu-image", "/u/hathitrust/htrc-sloan/script-simulator/vm-image/ubuntu-image", "This is an Ubuntu image.", "ubuntu", "password");
INSERT INTO images (imagename, imagepath, imagedescription, loginusername, loginpassword) VALUES ("suse-image", "/u/hathitrust/htrc-sloan/script-simulator/vm-image/suse-image", "This is a Suse image.", "ubuntu", "password");
INSERT INTO images (imagename, imagepath, imagedescription, loginusername, loginpassword) VALUES ("redhat-image", "/u/hathitrust/htrc-sloan/script-simulator/vm-image/redhat-image", "This is a Redhat image.", "ubuntu", "password");
INSERT INTO policies (policyname, policypath) VALUES ("policy-1", "/u/hathitrust/htrc-sloan/script-simulator/firewall-policy/policy-1");
INSERT INTO policies (policyname, policypath) VALUES ("policy-2", "/u/hathitrust/htrc-sloan/script-simulator/firewall-policy/policy-2");